package pro.ax.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.Plan;
import pro.ax.platform.domain.Subscription;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.CompanyRepository;
import pro.ax.platform.repo.SubscriptionRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.BillingDto;
import pro.ax.platform.web.dto.PlanDto;
import pro.ax.platform.web.dto.SubscriptionDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Тарифы и продвижение. В демо-версии оплата имитируется: подключение тарифа
 * сразу активирует продвижение на 30 дней и пишет запись в историю платежей.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final int PERIOD_DAYS = 30;

    private final CompanyRepository companies;
    private final SubscriptionRepository subscriptions;
    private final CompanyService companyService;
    private final DtoMapper mapper;

    public BillingService(CompanyRepository companies, SubscriptionRepository subscriptions,
                          CompanyService companyService, DtoMapper mapper) {
        this.companies = companies;
        this.subscriptions = subscriptions;
        this.companyService = companyService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public BillingDto current(User user) {
        Company company = companyService.requireMyCompany(user);
        return dto(company);
    }

    @Transactional
    public BillingDto upgrade(User user, String planCode) {
        Company company = companyService.requireMyCompany(user);
        Plan plan = parse(planCode);
        Instant now = Instant.now();
        Instant expiresAt = plan.isPaid() ? now.plus(PERIOD_DAYS, ChronoUnit.DAYS) : null;

        company.setPlan(plan);
        company.setPromotedUntil(expiresAt);
        companies.save(company);

        BigDecimal amount = BigDecimal.valueOf(plan.getMonthlyPrice());
        subscriptions.save(new Subscription(company, plan, amount, now, expiresAt));
        log.info("Компания {} перешла на тариф {} ({})", company.getName(), plan, amount);

        return dto(company);
    }

    /** Снимает истёкшие тарифы. Вызывается планировщиком. */
    @Transactional
    public int expirePromotions() {
        List<Company> expired = companies.findByPlanNotAndPromotedUntilBefore(Plan.FREE, Instant.now());
        for (Company company : expired) {
            company.setPlan(Plan.FREE);
            company.setPromotedUntil(null);
            companies.save(company);
        }
        if (!expired.isEmpty()) {
            log.info("Продвижение завершено у {} компаний", expired.size());
        }
        return expired.size();
    }

    private BillingDto dto(Company company) {
        List<Subscription> history = subscriptions.findByCompanyIdOrderByCreatedAtDesc(company.getId());
        BigDecimal totalSpent = history.stream()
                .map(Subscription::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SubscriptionDto> historyDto = history.stream().map(mapper::subscription).toList();
        return new BillingDto(
                company.getPlan().name(),
                company.getPlan().getTitle(),
                company.isPromoted(),
                company.getPromotedUntil(),
                company.getPlan().getMonthlyPrice(),
                totalSpent,
                plans(company.getPlan()),
                historyDto
        );
    }

    private static List<PlanDto> plans(Plan current) {
        return List.of(
                new PlanDto("FREE", "Базовый", 0, "Чтобы вас нашли в каталоге",
                        List.of("Карточка компании в каталоге", "До 3 предложений товаров и услуг",
                                "Отклики на заявки", "Отзывы и рейтинг"),
                        current == Plan.FREE),
                new PlanDto("PRO", "PRO", 4900, "Больше показов и заявок",
                        List.of("Всё из «Базового»", "Продвижение в каталоге и поиске",
                                "До 20 предложений", "Документы компании", "Бейдж PRO в карточке"),
                        current == Plan.PRO),
                new PlanDto("PREMIUM", "Premium", 14900, "Верх каталога и максимум внимания",
                        List.of("Всё из «PRO»", "Первые позиции в каталоге", "Бейдж Premium",
                                "Неограниченные предложения", "Приоритетная поддержка"),
                        current == Plan.PREMIUM)
        );
    }

    private static Plan parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("Не выбран тариф");
        }
        try {
            return Plan.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Неизвестный тариф: " + raw);
        }
    }
}
