package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pro.ax.platform.domain.ApplicationDocument;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.EscrowPayment;
import pro.ax.platform.domain.EscrowStatus;
import pro.ax.platform.domain.LegalApplication;
import pro.ax.platform.domain.LegalApplicationStatus;
import pro.ax.platform.domain.LegalService;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.ApplicationDocumentRepository;
import pro.ax.platform.repo.EscrowPaymentRepository;
import pro.ax.platform.repo.LegalApplicationRepository;
import pro.ax.platform.repo.LegalServiceRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.DocumentDto;
import pro.ax.platform.web.dto.EscrowDto;
import pro.ax.platform.web.dto.LegalApplicationDto;
import pro.ax.platform.web.dto.LegalApplicationForm;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Жизненный цикл заявки «частное лицо → юридический статус»:
 * заявка → документы → очная явка → эскроу-оплата → подтверждение кодом → статус юрлица.
 */
@Service
public class LegalApplicationService {

    private static final int OTP_TTL_MINUTES = 15;
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final LegalApplicationRepository applications;
    private final LegalServiceRepository services;
    private final EscrowPaymentRepository payments;
    private final ApplicationDocumentRepository documents;
    private final CompanyService companyService;
    private final IndustryService industryService;
    private final AccountService accountService;
    private final FileStorageService storage;
    private final DtoMapper mapper;
    private final SecureRandom random = new SecureRandom();

    public LegalApplicationService(LegalApplicationRepository applications, LegalServiceRepository services,
                                   EscrowPaymentRepository payments, ApplicationDocumentRepository documents,
                                   CompanyService companyService, IndustryService industryService,
                                   AccountService accountService, FileStorageService storage, DtoMapper mapper) {
        this.applications = applications;
        this.services = services;
        this.payments = payments;
        this.documents = documents;
        this.companyService = companyService;
        this.industryService = industryService;
        this.accountService = accountService;
        this.storage = storage;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------- чтение

    @Transactional(readOnly = true)
    public List<LegalApplicationDto> myApplications(User user) {
        return applications.findByAuthorIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(application -> toDto(application, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LegalApplicationDto> incoming(User user) {
        Optional<Company> company = companyService.findMyCompany(user);
        if (company.isEmpty()) {
            return List.of();
        }
        return applications.findByProviderIdOrderByCreatedAtDesc(company.get().getId()).stream()
                .map(application -> toDto(application, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public long pendingForProvider(User user) {
        return companyService.findMyCompany(user)
                .map(company -> applications.countByProviderIdAndStatusIn(company.getId(),
                        List.of(LegalApplicationStatus.NEW, LegalApplicationStatus.DOCUMENTS_REQUESTED,
                                LegalApplicationStatus.SCHEDULED, LegalApplicationStatus.PAID)))
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public LegalApplicationDto detail(User user, Long id) {
        LegalApplication application = require(id);
        requireView(user, application);
        return toDto(application, user);
    }

    // ------------------------------------------------------------ создание

    @Transactional
    public LegalApplicationDto create(User user, LegalApplicationForm form) {
        if (companyService.findMyCompany(user).isPresent() || user.isAdmin()) {
            throw ApiException.badRequest("Оформление статуса доступно частному лицу без компании");
        }
        LegalService service = services.findById(form.serviceId())
                .filter(LegalService::isActive)
                .orElseThrow(() -> ApiException.notFound("Услуга не найдена"));
        Company provider = service.getProvider();
        industryService.require(form.industryCode());

        boolean activeExists = applications.existsByAuthorIdAndProviderIdAndStatusNotIn(
                user.getId(), provider.getId(),
                List.of(LegalApplicationStatus.COMPLETED, LegalApplicationStatus.CANCELLED));
        if (activeExists) {
            throw ApiException.conflict("У вас уже есть активная заявка в эту юридическую фирму");
        }

        LegalApplication application = new LegalApplication(user, provider, service,
                service.getTargetStatus(), form.companyName().trim(), form.city().trim(), form.industryCode());
        application.setInn(form.inn() == null ? null : form.inn().trim());
        application.setComment(form.comment() == null ? null : form.comment().trim());
        return toDto(applications.save(application), user);
    }

    // ----------------------------------------------------- шаги исполнителя

    @Transactional
    public LegalApplicationDto requestDocuments(User user, Long id, String comment) {
        LegalApplication application = require(id);
        requireProviderSide(user, application);
        requireNotFinal(application);
        if (application.getStatus() == LegalApplicationStatus.PAID) {
            throw ApiException.conflict("Услуга уже оплачена");
        }
        application.setProviderComment(comment == null ? null : comment.trim());
        application.setStatus(LegalApplicationStatus.DOCUMENTS_REQUESTED);
        return toDto(applications.save(application), user);
    }

    @Transactional
    public LegalApplicationDto schedule(User user, Long id, String appointmentAt) {
        LegalApplication application = require(id);
        requireProviderSide(user, application);
        requireNotFinal(application);
        Instant moment = parseAppointment(appointmentAt);
        if (moment.isBefore(Instant.now())) {
            throw ApiException.badRequest("Дата и время явки должны быть в будущем");
        }
        application.setAppointmentAt(moment);
        application.setStatus(LegalApplicationStatus.SCHEDULED);
        return toDto(applications.save(application), user);
    }

    @Transactional
    public LegalApplicationDto complete(User user, Long id) {
        LegalApplication application = require(id);
        requireProviderSide(user, application);
        if (application.getStatus() != LegalApplicationStatus.PAID) {
            throw ApiException.conflict("Услугу можно подтвердить только после оплаты");
        }
        EscrowPayment payment = payments.findByApplicationId(application.getId())
                .orElseThrow(() -> ApiException.conflict("Платёж не найден"));
        if (payment.getStatus() != EscrowStatus.HELD) {
            throw ApiException.conflict("Платёж уже закрыт");
        }
        if (payment.isOtpActive()) {
            throw ApiException.conflict("Код уже выдан заказчику");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        payment.issueOtp(code, Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
        payments.save(payment);
        return toDto(application, user);
    }

    @Transactional
    public LegalApplicationDto confirm(User user, Long id, String code) {
        LegalApplication application = require(id);
        requireProviderSide(user, application);
        EscrowPayment payment = payments.findByApplicationId(application.getId())
                .orElseThrow(() -> ApiException.conflict("Платёж не найден"));
        if (payment.getStatus() != EscrowStatus.HELD) {
            throw ApiException.conflict("Платёж уже закрыт");
        }
        if (!payment.isOtpActive()) {
            throw ApiException.badRequest("Код не выдан или истёк — подтвердите выполнение заново");
        }
        if (payment.getOtpAttempts() >= OTP_MAX_ATTEMPTS) {
            throw ApiException.conflict("Слишком много попыток. Подтвердите выполнение заново, чтобы получить новый код");
        }
        if (!codesEqual(payment.getOtpCode(), code)) {
            payment.registerAttempt();
            payments.save(payment);
            throw ApiException.badRequest("Неверный код подтверждения");
        }

        payment.release();
        payments.save(payment);
        application.setStatus(LegalApplicationStatus.COMPLETED);
        application.setCompletedAt(Instant.now());
        applications.save(application);
        upgradeAuthorToCompany(application);
        return toDto(application, user);
    }

    // --------------------------------------------------------- шаги заказчика

    @Transactional
    public LegalApplicationDto pay(User user, Long id) {
        LegalApplication application = require(id);
        requireAuthor(user, application);
        if (application.getStatus() != LegalApplicationStatus.SCHEDULED) {
            throw ApiException.conflict("Оплата доступна после согласования даты явки");
        }
        if (payments.existsByApplicationId(application.getId())) {
            throw ApiException.conflict("Заявка уже оплачена");
        }
        BigDecimal amount = application.getService() == null
                ? BigDecimal.ZERO
                : application.getService().getPrice();
        EscrowPayment payment = payments.save(new EscrowPayment(application, amount));
        application.setStatus(LegalApplicationStatus.PAID);
        applications.save(application);
        // Возвращаем DTO заказчика — он видит статус удержания
        return toDto(application, user);
    }

    @Transactional
    public LegalApplicationDto cancel(User user, Long id) {
        LegalApplication application = require(id);
        boolean admin = user.isAdmin();
        if (!admin) {
            requireAuthor(user, application);
        }
        if (application.getStatus().isFinal()) {
            throw ApiException.conflict("Заявка уже закрыта");
        }
        payments.findByApplicationId(application.getId()).ifPresent(payment -> {
            if (payment.getStatus() == EscrowStatus.HELD) {
                payment.refund();
                payments.save(payment);
            }
        });
        application.setStatus(LegalApplicationStatus.CANCELLED);
        return toDto(applications.save(application), user);
    }

    // ------------------------------------------------------------ документы

    @Transactional
    public DocumentDto uploadDocument(User user, Long id, String title, MultipartFile file) {
        LegalApplication application = require(id);
        requireAuthor(user, application);
        requireNotFinal(application);
        String storedPath = storage.storeDocument(file);
        String cleanTitle = title == null || title.isBlank()
                ? (file.getOriginalFilename() == null ? "Документ" : file.getOriginalFilename())
                : title.trim();
        if (cleanTitle.length() > 160) {
            cleanTitle = cleanTitle.substring(0, 160);
        }
        ApplicationDocument document = new ApplicationDocument(application, cleanTitle,
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename(),
                file.getContentType(), file.getSize(), storedPath);
        return mapper.applicationDocument(documents.save(document));
    }

    @Transactional
    public void deleteDocument(User user, Long id, Long documentId) {
        LegalApplication application = require(id);
        requireAuthor(user, application);
        ApplicationDocument document = documents.findByIdAndApplicationId(documentId, application.getId())
                .orElseThrow(() -> ApiException.notFound("Документ не найден"));
        documents.delete(document);
        storage.delete(document.getStoredPath());
    }

    // ------------------------------------------------------------ внутреннее

    private void upgradeAuthorToCompany(LegalApplication application) {
        User author = application.getAuthor();
        if (companyService.findMyCompany(author).isPresent()) {
            return;
        }
        accountService.createCompany(author, application.getCompanyName(),
                application.getIndustryCode(), application.getCity());
    }

    public LegalApplicationDto toDto(LegalApplication application, User viewer) {
        Company provider = application.getProvider();
        Optional<Company> viewerCompany = companyService.findMyCompany(viewer);
        boolean author = application.getAuthor().getId().equals(viewer.getId());
        boolean providerSide = viewerCompany
                .map(company -> company.getId().equals(provider.getId()))
                .orElse(false);
        boolean admin = viewer.isAdmin();
        String role = author ? "AUTHOR" : providerSide ? "PROVIDER" : admin ? "ADMIN" : "GUEST";

        List<DocumentDto> documentDtos = author || providerSide || admin
                ? documents.findByApplicationIdOrderByCreatedAtDesc(application.getId()).stream()
                        .map(mapper::applicationDocument)
                        .toList()
                : List.of();

        EscrowPayment payment = payments.findByApplicationId(application.getId()).orElse(null);
        EscrowDto paymentDto = payment == null ? null : new EscrowDto(
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getStatus().getTitle(),
                payment.getHeldAt(),
                payment.getReleasedAt(),
                payment.getRefundedAt(),
                payment.isOtpActive(),
                author ? payment.getOtpCode() : null,
                author ? payment.getOtpExpiresAt() : null
        );

        boolean finalStatus = application.getStatus().isFinal();
        boolean paidStatus = application.getStatus() == LegalApplicationStatus.PAID;
        boolean heldPayment = payment != null && payment.getStatus() == EscrowStatus.HELD;

        return new LegalApplicationDto(
                application.getId(),
                application.getAuthor().getId(),
                application.getAuthor().getDisplayName(),
                author || admin ? application.getAuthor().getEmail() : null,
                application.getAuthor().getCity(),
                providerSide || admin ? application.getAuthor().getPhone() : null,
                provider.getId(),
                provider.getName(),
                provider.getSlug(),
                provider.getCity(),
                application.getService() == null ? null : application.getService().getId(),
                application.getService() == null ? null : application.getService().getTitle(),
                application.getTargetStatus().name(),
                application.getTargetStatus().getTitle(),
                application.getCompanyName(),
                application.getInn(),
                application.getCity(),
                application.getIndustryCode(),
                industryTitle(application.getIndustryCode()),
                application.getComment(),
                application.getProviderComment(),
                application.getStatus().name(),
                application.getStatus().getTitle(),
                application.getAppointmentAt(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                application.getCompletedAt(),
                paymentDto,
                documentDtos,
                role,
                author && !finalStatus,
                author && payment == null && application.getStatus() == LegalApplicationStatus.SCHEDULED,
                author && !finalStatus,
                providerSide && !finalStatus,
                providerSide && !finalStatus,
                providerSide && paidStatus && heldPayment && !payment.isOtpActive(),
                providerSide && heldPayment && payment.isOtpActive(),
                payment == null
                        ? (application.getService() == null ? BigDecimal.ZERO : application.getService().getPrice())
                        : payment.getAmount()
        );
    }

    private String industryTitle(String code) {
        try {
            return industryService.require(code).getTitle();
        } catch (RuntimeException ex) {
            return code;
        }
    }

    public LegalApplication require(Long id) {
        return applications.findById(id)
                .orElseThrow(() -> ApiException.notFound("Заявка не найдена"));
    }

    private static void requireView(User user, LegalApplication application) {
        if (user.isAdmin()) {
            return;
        }
        if (application.getAuthor().getId().equals(user.getId())) {
            return;
        }
        throw ApiException.forbidden("Заявка доступна заказчику и исполнителю");
    }

    private static void requireAuthor(User user, LegalApplication application) {
        if (!application.getAuthor().getId().equals(user.getId())) {
            throw ApiException.forbidden("Действие доступно только автору заявки");
        }
    }

    private void requireProviderSide(User user, LegalApplication application) {
        Company company = companyService.findMyCompany(user)
                .orElseThrow(() -> ApiException.forbidden("Действие доступно только юридической фирме"));
        if (!company.getId().equals(application.getProvider().getId())) {
            throw ApiException.forbidden("Заявка адресована другой компании");
        }
    }

    private static void requireNotFinal(LegalApplication application) {
        if (application.getStatus().isFinal()) {
            throw ApiException.conflict("Заявка уже закрыта");
        }
    }

    private static boolean codesEqual(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(), actual.trim().getBytes());
    }

    private static Instant parseAppointment(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("Укажите дату и время явки");
        }
        String value = raw.trim();
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (RuntimeException ignored) {
            // пробуем следующий формат
        }
        try {
            return LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant();
        } catch (RuntimeException ignored) {
            // пробуем ISO instant
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("Некорректная дата и время. Формат: 2026-11-05T10:00");
        }
    }
}
