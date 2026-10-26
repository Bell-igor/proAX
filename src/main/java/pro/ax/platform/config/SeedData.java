package pro.ax.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.ChatMessage;
import pro.ax.platform.domain.ChatThread;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.CompanySize;
import pro.ax.platform.domain.Industry;
import pro.ax.platform.domain.NeedRequest;
import pro.ax.platform.domain.NeedStatus;
import pro.ax.platform.domain.Offer;
import pro.ax.platform.domain.OfferType;
import pro.ax.platform.domain.Plan;
import pro.ax.platform.domain.Proposal;
import pro.ax.platform.domain.ProposalStatus;
import pro.ax.platform.domain.Review;
import pro.ax.platform.domain.Role;
import pro.ax.platform.domain.Subscription;
import pro.ax.platform.domain.User;
import pro.ax.platform.repo.ChatMessageRepository;
import pro.ax.platform.repo.ChatThreadRepository;
import pro.ax.platform.repo.CompanyRepository;
import pro.ax.platform.repo.IndustryRepository;
import pro.ax.platform.repo.NeedRequestRepository;
import pro.ax.platform.repo.OfferRepository;
import pro.ax.platform.repo.ProposalRepository;
import pro.ax.platform.repo.ReviewRepository;
import pro.ax.platform.repo.SubscriptionRepository;
import pro.ax.platform.repo.UserRepository;
import pro.ax.platform.security.PasswordHasher;
import pro.ax.platform.support.Slugifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Демонстрационное наполнение площадки: отрасли, компании, предложения, заявки, отклики и отзывы.
 * Запускается только на пустой базе и только при proax.demo-data=true.
 */
@Component
@ConditionalOnProperty(name = "proax.demo-data", havingValue = "true", matchIfMissing = true)
public class SeedData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedData.class);
    private static final String DEMO_PASSWORD = "demo123";

    private final IndustryRepository industries;
    private final CompanyRepository companies;
    private final UserRepository users;
    private final OfferRepository offers;
    private final NeedRequestRepository needs;
    private final ProposalRepository proposals;
    private final ReviewRepository reviews;
    private final ChatThreadRepository threads;
    private final ChatMessageRepository messages;
    private final SubscriptionRepository subscriptions;
    private final PasswordHasher hasher;

    public SeedData(IndustryRepository industries, CompanyRepository companies, UserRepository users,
                    OfferRepository offers, NeedRequestRepository needs, ProposalRepository proposals,
                    ReviewRepository reviews, ChatThreadRepository threads, ChatMessageRepository messages,
                    SubscriptionRepository subscriptions, PasswordHasher hasher) {
        this.industries = industries;
        this.companies = companies;
        this.users = users;
        this.offers = offers;
        this.needs = needs;
        this.proposals = proposals;
        this.reviews = reviews;
        this.threads = threads;
        this.messages = messages;
        this.subscriptions = subscriptions;
        this.hasher = hasher;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (industries.count() > 0) {
            log.info("Демо-данные уже загружены, пропускаем сидинг");
            return;
        }
        log.info("Заполняем площадку демонстрационными данными…");

        Map<String, Industry> byCode = seedIndustries();
        seedAdmin();
        List<CompanySeed> seedList = companySeeds();
        List<Company> companyList = seedCompanies(byCode, seedList);
        seedOffers(companyList, seedList);
        seedReviews(companyList);
        List<NeedRequest> needList = seedNeeds(companyList);
        seedProposals(needList, companyList);
        seedPlans(companyList);
        seedChats(needList, companyList);

        log.info("Демо-данные готовы: {} компаний, {} заявок", companyList.size(), needList.size());
    }

    // ---------------------------------------------------------------- отрасли

    private Map<String, Industry> seedIndustries() {
        List<Industry> list = List.of(
                new Industry("raw", "Сырьё и материалы",
                        "Металл, химия, полимеры, стройматериалы, топливо", "⛏", 10),
                new Industry("manufacturing", "Производство и переработка",
                        "Заводы, цеха, контрактное производство, механообработка", "🏭", 20),
                new Industry("logistics", "Логистика и перевозки",
                        "Авто, железная дорога, контейнеры, последняя миля", "🚚", 30),
                new Industry("warehouse", "Складские помещения",
                        "Ответственное хранение, фулфилмент, кросс-докинг", "🏬", 40),
                new Industry("equipment", "Оборудование и станки",
                        "Поставка, монтаж, пусконаладка, сервис", "⚙️", 50),
                new Industry("packaging", "Упаковка и тара",
                        "Гофрокартон, плёнка, паллеты, маркировка", "📦", 60),
                new Industry("legal", "Юридические услуги",
                        "Договоры, арбитраж, ВЭД, интеллектуальная собственность", "⚖️", 70),
                new Industry("finance", "Финансы и бухгалтерия",
                        "Банки, лизинг, аудит, налоговый учёт", "💳", 80),
                new Industry("it", "IT и автоматизация",
                        "ERP, MES, интеграции, промышленная аналитика", "💻", 90),
                new Industry("marketing", "Маркетинг и продвижение",
                        "B2B-маркетинг, выставки, аналитика спроса", "📣", 100),
                new Industry("realty", "Помещения и площади",
                        "Производственные, складские и офисные площади", "🏢", 110),
                new Industry("energy", "Энергетика и ресурсы",
                        "Электроэнергия, газ, вода, тепло, энергоаудит", "⚡", 120),
                new Industry("hr", "Персонал и HR",
                        "Подбор, аутстаффинг, обучение, охрана труда", "👥", 130),
                new Industry("consulting", "Консалтинг",
                        "Стратегия, lean, сертификация, финансовая модель", "📊", 140),
                new Industry("trade", "Торговля и дистрибуция",
                        "Оптовые поставки, дистрибуция, расходные материалы", "🛒", 150)
        );
        industries.saveAll(list);
        return list.stream().collect(java.util.stream.Collectors.toMap(Industry::getCode, i -> i));
    }

    private void seedAdmin() {
        User admin = new User("admin@proax.ru", hasher.hash("admin123"), "Администратор ProAX", Role.ADMIN);
        users.save(admin);
    }

    // --------------------------------------------------------------- компании

    private List<Company> seedCompanies(Map<String, Industry> byCode, List<CompanySeed> seeds) {
        List<Company> result = new ArrayList<>(seeds.size());
        for (CompanySeed seed : seeds) {
            User owner = new User(seed.ownerEmail(), hasher.hash(DEMO_PASSWORD), seed.ownerName(), Role.COMPANY);
            users.save(owner);

            Company company = new Company(seed.name(), uniqueSlug(seed.name()), byCode.get(seed.industry()));
            company.setCity(seed.city());
            company.setDescription(seed.description());
            company.setWebsite(seed.website());
            company.setPhone(seed.phone());
            company.setEmail(seed.ownerEmail());
            company.setSize(seed.size());
            company.setFoundedYear(seed.founded());
            company.setVerified(seed.verified());
            company.setAccentColor(seed.accent());
            company.setOwner(owner);
            companies.save(company);
            result.add(company);
        }
        return result;
    }

    private List<CompanySeed> companySeeds() {
        return List.of(
                new CompanySeed("УралМеталл", "raw", "Екатеринбург", CompanySize.LARGE, 1998, true,
                        "+7 (343) 210-45-16", "https://uralmetall.ru",
                        "Поставщик металлопроката и трубного сортамента для машиностроения, строительства и энергетики. "
                                + "Собственный склад 18 000 м², резка и гибка в размер, доставка по УрФО и Сибири. "
                                + "Работаем по договору с отсрочкой платежа до 45 дней, все партии сопровождаются сертификатами.",
                        "Игорь Соколов", "demo@proax.ru", "#C8F169",
                        List.of(
                                new OfferSeed("Лист горячекатаный 3–12 мм", OfferType.PRODUCT, "68500", "₽/т",
                                        "Ст3сп5 и 09Г2С, резка в размер, сертификаты ГОСТ 19903-2015. Отгрузка от одной тонны."),
                                new OfferSeed("Труба бесшовная 57–219 мм", OfferType.PRODUCT, "84000", "₽/т",
                                        "ГОСТ 8732-78, сталь 20 и 09Г2С. Наличие на складе, доставка по УрФО за 1–3 дня."),
                                new OfferSeed("Заготовка круглая ст. 45", OfferType.PRODUCT, "59000", "₽/т",
                                        "Диаметр 20–160 мм, термическая обработка и ультразвуковой контроль по запросу.")
                        )),
                new CompanySeed("ПолимерПром", "raw", "Нижний Новгород", CompanySize.MEDIUM, 2006, true,
                        "+7 (831) 245-11-90", "https://polymerprom.ru",
                        "Производим и поставляем полимерное сырьё: ПВД, ПВХ, полипропилен и вторичные грануляты. "
                                + "Собственная лаборатория контроля качества, линия компаундирования и гибкие условия по объёмам.",
                        "Марина Волкова", "m.volkova@example.ru", "#7EE0C2",
                        List.of(
                                new OfferSeed("ПВД 15803-020, гранулят", OfferType.PRODUCT, "112000", "₽/т",
                                        "Первичный полиэтилен высокого давления для выдувной экструзии. Мешки 25 кг."),
                                new OfferSeed("ПВХ-С-7059-М", OfferType.PRODUCT, "98000", "₽/т",
                                        "Смола для профилей и труб, стабильное качество, отгрузка от 5 тонн.")
                        )),
                new CompanySeed("Завод «ТехноЛит»", "manufacturing", "Челябинск", CompanySize.LARGE, 1975, true,
                        "+7 (351) 778-30-24", "https://tekhnolit.ru",
                        "Литейное производство полного цикла: чугунное и стальное литьё, механообработка и сборка. "
                                + "Работаем по чертежам заказчика, оснастка своя, серии от 50 изделий. Аттестованы для поставок в нефтегаз.",
                        "Алексей Гринёв", "a.grinev@example.ru", "#FFB86B",
                        List.of(
                                new OfferSeed("Литьё чугунное по чертежам", OfferType.SERVICE, "260", "₽/кг",
                                        "ВЧ50, ВЧ60, СЧ20. Вес отливки 0,5–400 кг, срок изготовления 10–25 дней."),
                                new OfferSeed("Механообработка ЧПУ", OfferType.SERVICE, "1800", "₽/час",
                                        "Токарные и фрезерные центры, 3–5 координат, приёмка по КИМ с протоколом.")
                        )),
                new CompanySeed("Контрактная механика «Станочный двор»", "manufacturing", "Москва",
                        CompanySize.SMALL, 2014, false,
                        "+7 (495) 120-77-03", "https://stanochny-dvor.ru",
                        "Контрактное производство металлоизделий: лазерная резка, гибка, сварка, покраска. "
                                + "Берём мелкие серии и прототипы, срок изготовления от пяти дней, работаем с НДС.",
                        "Павел Ершов", "p.ershov@example.ru", "#A5B4FC",
                        List.of(
                                new OfferSeed("Лазерная резка металла", OfferType.SERVICE, "85", "₽/м",
                                        "Лист до 12 мм, точность ±0,1 мм, раскрой по DXF."),
                                new OfferSeed("Сварка и покраска конструкций", OfferType.SERVICE, "1200", "₽/м²",
                                        "Полуавтомат и аргон, порошковая покраска по RAL.")
                        )),
                new CompanySeed("ТрансЛогистик", "logistics", "Казань", CompanySize.LARGE, 2003, true,
                        "+7 (843) 570-14-38", "https://translogistic.ru",
                        "Автомобильные перевозки FTL и LTL, сборные грузы и железнодорожные направления. "
                                + "120 единиц техники, GPS-мониторинг, страхование груза на полную стоимость. "
                                + "Диспетчерская 24/7, документы в электронном виде.",
                        "Дмитрий Ковалёв", "d.kovalev@example.ru", "#93C5FD",
                        List.of(
                                new OfferSeed("Перевозки FTL по России", OfferType.SERVICE, "42", "₽/км",
                                        "Тенты и рефрижераторы 20 т, подача от 4 часов в Поволжье."),
                                new OfferSeed("Сборные грузы (LTL)", OfferType.SERVICE, "9800", "₽/паллета",
                                        "Два рейса в неделю Казань — Новосибирск, консолидация на терминале.")
                        )),
                new CompanySeed("Северный путь", "logistics", "Санкт-Петербург", CompanySize.MEDIUM, 2011, true,
                        "+7 (812) 440-19-72", "https://severny-put.ru",
                        "Контейнерные перевозки и ВЭД-сопровождение: Китай, Турция, ОАЭ. Мультимодальные схемы, "
                                + "таможенное оформление, страхование и склад временного хранения в порту.",
                        "Ольга Ким", "o.kim@example.ru", "#F9A8D4",
                        List.of(
                                new OfferSeed("Контейнерные перевозки из Китая", OfferType.SERVICE, "4700", "₽/фрахт",
                                        "Мультимодальные схемы через Владивосток и Забайкальск, срок 25–35 дней."),
                                new OfferSeed("Таможенное оформление", OfferType.SERVICE, "45000", "₽/декларация",
                                        "Декларирование импорта и экспорта, сертификация, СВХ.")
                        )),
                new CompanySeed("Логопарк «Урал»", "warehouse", "Екатеринбург", CompanySize.LARGE, 2016, true,
                        "+7 (343) 380-22-11", "https://logopark-ural.ru",
                        "Складской комплекс класса A: 24 000 м², высота 12 м, 32 доковые ворот, полы с нагрузкой 6 т/м². "
                                + "Ответственное хранение, кросс-докинг и фулфилмент для маркетплейсов. Температурные зоны +2…+8 °C.",
                        "Сергей Богданов", "s.bogdanov@example.ru", "#C8F169",
                        List.of(
                                new OfferSeed("Ответственное хранение, класс A", OfferType.SERVICE, "620", "₽/паллета/мес",
                                        "Видеонаблюдение, страхование, доступ к WMS-отчётам в личном кабинете."),
                                new OfferSeed("Фулфилмент для маркетплейсов", OfferType.SERVICE, "95", "₽/заказ",
                                        "Приёмка, маркировка, сборка, отгрузка в Ozon и Wildberries.")
                        )),
                new CompanySeed("Склад-Сервис", "warehouse", "Новосибирск", CompanySize.SMALL, 2019, false,
                        "+7 (383) 312-08-45", "https://sklad-service.ru",
                        "Тёплые и холодные склады от 100 м², краткосрочное хранение и комплектация заказов. "
                                + "Работаем с малым бизнесом и сезонными поставками, есть погрузочная техника.",
                        "Анна Тимофеева", "a.timofeeva@example.ru", "#FCD34D",
                        List.of(
                                new OfferSeed("Краткосрочное хранение", OfferType.SERVICE, "380", "₽/паллета/мес",
                                        "От одной недели, без залога, приёмка 24/7 по предварительной заявке.")
                        )),
                new CompanySeed("Юридическое бюро «Право и Контур»", "legal", "Москва", CompanySize.MEDIUM, 2009, true,
                        "+7 (495) 640-71-20", "https://pravo-kontur.ru",
                        "Юридическое бюро для производственных и торговых компаний: договорная работа, ВЭД, налоговые споры, "
                                + "защита в арбитраже. 14 юристов, более 600 выигранных дел, фиксированные тарифы на абонентское обслуживание.",
                        "Елена Радова", "e.radova@example.ru", "#A5B4FC",
                        List.of(
                                new OfferSeed("Абонентское юридическое обслуживание", OfferType.SERVICE, "95000", "₽/мес",
                                        "Договоры, претензии, консультации и представительство в арбитраже."),
                                new OfferSeed("Сопровождение сделок ВЭД", OfferType.SERVICE, "180000", "₽/сделка",
                                        "Структурирование контракта, валютные оговорки, проверка контрагента.")
                        )),
                new CompanySeed("Капитал-Лизинг", "finance", "Москва", CompanySize.MEDIUM, 2007, true,
                        "+7 (495) 909-33-58", "https://kapital-leasing.ru",
                        "Лизинг оборудования, транспорта и спецтехники для малого и среднего бизнеса. "
                                + "Аванс от 10%, срок до 5 лет, решение за два рабочих дня. Работаем с поставщиками из реестра ProAX.",
                        "Роман Дьяков", "r.dyakov@example.ru", "#FFB86B",
                        List.of(
                                new OfferSeed("Лизинг станков и оборудования", OfferType.SERVICE, "10", "% аванс",
                                        "Финансирование до 60 млн ₽, без залога недвижимости."),
                                new OfferSeed("Лизинг спецтехники", OfferType.SERVICE, "12", "% аванс",
                                        "Погрузчики, экскаваторы, самосвалы. Оценка за 1 день.")
                        )),
                new CompanySeed("Интегратор «Цифровой цех»", "it", "Санкт-Петербург", CompanySize.MEDIUM, 2015, true,
                        "+7 (812) 780-06-14", "https://digital-ceh.ru",
                        "Внедряем MES, ERP и промышленную аналитику на заводах: от аудита процессов до сопровождения. "
                                + "Интеграции с 1С, SCADA и станками, диспетчеризация и цифровые паспорта изделий.",
                        "Никита Фомин", "n.fomin@example.ru", "#7EE0C2",
                        List.of(
                                new OfferSeed("Внедрение MES", OfferType.SERVICE, "1800000", "₽/проект",
                                        "Аудит, план, внедрение за 8–14 недель, обучение сменных мастеров."),
                                new OfferSeed("Интеграция 1С и оборудования", OfferType.SERVICE, "320000", "₽/интеграция",
                                        "Обмен номенклатурой и производственными заданиями, ОРС-сервер.")
                        )),
                new CompanySeed("Агентство «Промо-Пульс»", "marketing", "Москва", CompanySize.MICRO, 2018, false,
                        "+7 (499) 113-02-66", "https://promo-pulse.ru",
                        "B2B-маркетинг для промышленных компаний: выставки, контент, лидогенерация и аналитика спроса. "
                                + "Средний CPL по промышленной тематике — 1 900 ₽.",
                        "Виктория Лаврова", "v.lavrova@example.ru", "#F9A8D4",
                        List.of(
                                new OfferSeed("Продвижение на промышленных выставках", OfferType.SERVICE, "240000", "₽/выставка",
                                        "Стенд, материалы, работа с лидами и follow-up в CRM."),
                                new OfferSeed("Лидогенерация для B2B", OfferType.SERVICE, "1900", "₽/лид",
                                        "Многоканальные кампании, квалификация заявок, отчёт по воронке.")
                        )),
                new CompanySeed("СтанкоСервис", "equipment", "Ростов-на-Дону", CompanySize.MEDIUM, 2005, true,
                        "+7 (863) 290-45-71", "https://stankoservice.ru",
                        "Поставка, пусконаладка и сервис металлообрабатывающего оборудования. Склад запчастей, "
                                + "выездные инженеры по ЮФО и ЦФО, сервисные контракты с гарантией 4 часа на реакцию.",
                        "Артём Литвинов", "a.litvinov@example.ru", "#93C5FD",
                        List.of(
                                new OfferSeed("Пусконаладка станков с ЧПУ", OfferType.SERVICE, "145000", "₽/станок",
                                        "Геометрическая точность, обучение операторов, протокол приёмки."),
                                new OfferSeed("Сервисный контракт", OfferType.SERVICE, "78000", "₽/мес",
                                        "Плановое ТО, склад расходников, выезд инженера в течение 24 часов.")
                        )),
                new CompanySeed("ТараПак", "packaging", "Казань", CompanySize.MEDIUM, 2012, true,
                        "+7 (843) 212-60-32", "https://tarapak.ru",
                        "Гофрокартон, паллеты, стрейч-плёнка, маркировка и печать. Собственное производство, "
                                + "отгрузка от одной паллеты, склад в Казани и партнёрский склад в Екатеринбурге.",
                        "Илья Сорокин", "i.sorokin@example.ru", "#FCD34D",
                        List.of(
                                new OfferSeed("Гофрокороб четырёхклапанный", OfferType.PRODUCT, "38", "₽/шт",
                                        "Марка Т-23, размеры по заданию, печать в две краски."),
                                new OfferSeed("Стрейч-плёнка 500 мм", OfferType.PRODUCT, "890", "₽/рулон",
                                        "17 мкм, 2,4 кг, прозрачная и чёрная, отгрузка со склада.")
                        )),
                new CompanySeed("Индустриальный парк «Заводской»", "realty", "Екатеринбург",
                        CompanySize.LARGE, 2018, true,
                        "+7 (343) 220-88-30", "https://ip-zavodskoy.ru",
                        "Производственные и складские площади от 300 до 8 000 м² в аренду: электрическая мощность 1,2 МВт, "
                                + "кран-балки до 10 т, железнодорожная ветка. Резидентам — налоговые льготы регионального уровня.",
                        "Григорий Панов", "g.panov@example.ru", "#C8F169",
                        List.of(
                                new OfferSeed("Аренда производственного цеха", OfferType.SERVICE, "620", "₽/м²/мес",
                                        "От 300 м², высота 10 м, кран-балка, электричество по счётчику."),
                                new OfferSeed("Аренда склада класса B+", OfferType.SERVICE, "480", "₽/м²/мес",
                                        "От 500 м², тёплый контур, доковое оборудование.")
                        )),
                new CompanySeed("Кадровый центр «Профи-Ресурс»", "hr", "Москва", CompanySize.SMALL, 2017, false,
                        "+7 (495) 771-29-04", "https://profi-resurs.ru",
                        "Подбор рабочего и инженерного персонала для производств, аутстаффинг и обучение охране труда. "
                                + "Собственная база 18 000 кандидатов, закрытие вакансий в среднем за 12 дней.",
                        "Ксения Белова", "k.belova@example.ru", "#F9A8D4",
                        List.of(
                                new OfferSeed("Подбор рабочего персонала", OfferType.SERVICE, "28000", "₽/вакансия",
                                        "Операторы ЧПУ, слесари, наладчики. Гарантия замены 90 дней."),
                                new OfferSeed("Аутстаффинг персонала", OfferType.SERVICE, "3200", "₽/чел./смена",
                                        "Оформление, спецодежда, охрана труда и расчёт заработной платы.")
                        )),
                new CompanySeed("Лин-Консалт", "consulting", "Самара", CompanySize.SMALL, 2013, true,
                        "+7 (846) 205-17-83", "https://lean-consult.ru",
                        "Повышаем производительность заводов: lean, хронометраж, стандартизация и обучение руководителей. "
                                + "Средний результат проектов — минус 22% издержек за шесть месяцев.",
                        "Максим Беляев", "m.belyaev@example.ru", "#7EE0C2",
                        List.of(
                                new OfferSeed("Диагностика производственных потерь", OfferType.SERVICE, "210000", "₽/аудит",
                                        "Две недели на площадке, карта потерь, план быстрых улучшений."),
                                new OfferSeed("Обучение руководителей lean", OfferType.SERVICE, "64000", "₽/группа",
                                        "Три дня практики на реальном участке, стандарты и чек-листы.")
                        )),
                new CompanySeed("ЭнергоСбыт-Регион", "energy", "Екатеринбург", CompanySize.MEDIUM, 2004, true,
                        "+7 (343) 355-40-19", "https://es-region.ru",
                        "Поставка электроэнергии и мощности промышленным потребителям, энергоаудит и оптимизация тарифов. "
                                + "Сопровождение смены поставщика, работа с сетевыми организациями.",
                        "Тимур Асланов", "t.aslanov@example.ru", "#FFB86B",
                        List.of(
                                new OfferSeed("Поставка электроэнергии", OfferType.SERVICE, "4.85", "₽/кВт·ч",
                                        "Договор на 12 месяцев, фиксация цены на 70% объёма."),
                                new OfferSeed("Энергоаудит предприятия", OfferType.SERVICE, "160000", "₽/объект",
                                        "Тепловизионное обследование, отчёт с планом мероприятий.")
                        )),
                new CompanySeed("ОптТорг-Дистрибуция", "trade", "Новосибирск", CompanySize.MEDIUM, 2010, false,
                        "+7 (383) 220-65-77", "https://opttorg.ru",
                        "Дистрибуция промышленных товаров и расходных материалов: крепёж, инструмент, СИЗ. "
                                + "4 500 SKU на складе, отгрузка в день заказа, работа с юридическими лицами по счёту.",
                        "Дарья Орлова", "d.orlova@example.ru", "#A5B4FC",
                        List.of(
                                new OfferSeed("Крепёж оптом", OfferType.PRODUCT, "12", "₽/шт",
                                        "Болты, гайки, шайбы по ГОСТ, фасовка от 1 000 штук."),
                                new OfferSeed("СИЗ и спецодежда", OfferType.PRODUCT, "2400", "₽/комплект",
                                        "Костюмы, обувь, каски, перчатки. Нанесение логотипа.")
                        ))
        );
    }

    // ------------------------------------------------------------ предложения

    private void seedOffers(List<Company> companyList, List<CompanySeed> seeds) {
        for (int i = 0; i < seeds.size(); i++) {
            Company company = companyList.get(i);
            for (OfferSeed seed : seeds.get(i).offers()) {
                Offer offer = new Offer(company, seed.title());
                offer.setDescription(seed.description());
                offer.setType(seed.type());
                offer.setPriceFrom(new BigDecimal(seed.price()));
                offer.setUnit(seed.unit());
                offers.save(offer);
            }
        }
    }

    // ---------------------------------------------------------------- отзывы

    private void seedReviews(List<Company> companyList) {
        List<ReviewSeed> seeds = List.of(
                new ReviewSeed(0, 2, 5, "Отгрузили 18 тонн листа за два дня, сертификаты пришли вместе с машиной. Работаем третий год."),
                new ReviewSeed(0, 14, 4, "Качество стабильное, но по срокам в сезон бывают сдвиги на неделю. Предупреждают заранее."),
                new ReviewSeed(0, 4, 5, "Помогли подобрать аналог стали под наш проект и сэкономили около 8% бюджета."),
                new ReviewSeed(1, 3, 4, "Гранулят соответствует заявленным параметрам, лабораторный протокол предоставили без напоминаний."),
                new ReviewSeed(1, 13, 5, "Гибкие по объёмам: взяли пробную партию 500 кг, потом вышли на 20 тонн в месяц."),
                new ReviewSeed(2, 0, 5, "Отливки по нашим чертежам с первого раза, КИМ-протокол в комплекте. Рекомендую."),
                new ReviewSeed(2, 12, 4, "Сложные тонкостенные отливки делают хорошо, но оснастку ждали пять недель."),
                new ReviewSeed(3, 2, 4, "Быстро режут и гнут, удобно работать с мелкими сериями по 20–50 деталей."),
                new ReviewSeed(4, 6, 5, "120 рейсов за квартал, ни одной просрочки. Диспетчеры на связи до позднего вечера."),
                new ReviewSeed(4, 17, 4, "Цена выше рынка на 5–7%, но страхование и документы в порядке."),
                new ReviewSeed(5, 1, 5, "Провели контейнер из Шанхая за 29 дней, таможню закрыли без корректировок."),
                new ReviewSeed(6, 13, 5, "Хранение 800 паллет, доступ к остаткам онлайн. Инвентаризация сошлась до единицы."),
                new ReviewSeed(6, 7, 4, "Класс A соответствует заявленному, но в декабре не было свободных паллетомест."),
                new ReviewSeed(8, 1, 5, "Помогли переписать контракт с китайским поставщиком, убрали валютный риск."),
                new ReviewSeed(8, 4, 5, "Выиграли арбитраж на 4,2 млн ₽. Отчёты каждую неделю, без сюрпризов по счёту."),
                new ReviewSeed(9, 12, 4, "Одобрили лизинг станка за три дня, аванс 15%. Платёж удобный."),
                new ReviewSeed(10, 2, 5, "MES запустили за 11 недель, мастера реально пользуются. Отдельный плюс — обучение в смену."),
                new ReviewSeed(10, 16, 4, "Интеграция с 1С прошла с двумя доработками, всё закрыли в рамках бюджета."),
                new ReviewSeed(12, 2, 5, "Пусконаладка за один день, операторов обучили на месте. Реакция на заявку — 3 часа."),
                new ReviewSeed(13, 6, 5, "Короба нужного размера, печать чёткая. Отгружают от одной паллеты, что редкость."),
                new ReviewSeed(13, 0, 4, "Плёнка нормальная, но в прошлом месяце была задержка на четыре дня."),
                new ReviewSeed(14, 3, 5, "Арендовали 800 м² с кран-балкой, электричество оформили за две недели."),
                new ReviewSeed(15, 2, 4, "Закрыли 12 вакансий операторов за месяц, двое заменены по гарантии."),
                new ReviewSeed(16, 2, 5, "Аудит выявил узкое место на упаковке, после перестановки участка выпуск вырос на 18%."),
                new ReviewSeed(17, 2, 4, "Оптимизировали тариф, экономия около 6% за квартал. Отчёт подробный."),
                new ReviewSeed(11, 6, 4, "Сделали кампанию к выставке, лиды были качественные, но работа с ними легла на нас.")
        );
        for (ReviewSeed seed : seeds) {
            Company company = companyList.get(seed.companyIndex());
            User author = companyList.get(seed.authorIndex()).getOwner();
            if (company.getId().equals(companyList.get(seed.authorIndex()).getId())) {
                continue;
            }
            if (reviews.existsByCompanyIdAndAuthorId(company.getId(), author.getId())) {
                continue;
            }
            reviews.save(new Review(company, author, seed.rating(), seed.text()));
        }
        for (Company company : companyList) {
            long count = reviews.countByCompanyId(company.getId());
            Double average = reviews.averageRating(company.getId());
            company.setReviewsCount((int) count);
            company.setRating(average == null ? 0d : average);
            companies.save(company);
        }
    }

    // ----------------------------------------------------------------- заявки

    private List<NeedRequest> seedNeeds(List<Company> companyList) {
        List<NeedSeed> seeds = List.of(
                new NeedSeed("Нужен металлопрокат: лист 09Г2С, 12 тонн", "raw", "Екатеринбург",
                        "1200000", 14, 2, NeedStatus.OPEN,
                        "Для серии корпусных деталей требуется лист 09Г2С толщиной 8 и 10 мм, 12 тонн суммарно. "
                                + "Нужны сертификаты и резка в размер 1 500 × 3 000. Отгрузка на наш склад в Челябинске."),
                new NeedSeed("Ответственное хранение 800 паллет, класс A", "warehouse", "Екатеринбург",
                        "480000", 20, 13, NeedStatus.IN_PROGRESS,
                        "Ищем склад класса A для хранения упаковочных материалов: 800 паллетомест, температура не ниже +5 °C. "
                                + "Нужны приёмка по счёту, отгрузка по заявке в течение 4 часов и доступ к остаткам через API."),
                new NeedSeed("Перевозчик: 12 рейсов Казань — Новосибирск, тент 20 т", "logistics", "Казань",
                        "660000", 10, 12, NeedStatus.OPEN,
                        "Регулярные рейсы с марта по май, тент 20 тонн, 82 м³. Погрузка на нашем складе, "
                                + "разгрузка в Новосибирске по графику. Нужны GPS-отчёты и страхование груза."),
                new NeedSeed("Сопровождение контракта ВЭД с поставщиком из Китая", "legal", "Москва",
                        "250000", 25, 1, NeedStatus.OPEN,
                        "Поставщик из Гуанчжоу, контракт на 380 000 USD, отсрочка 30 дней. Нужна проверка контрагента, "
                                + "структурирование валютных оговорок и поддержка при открытии аккредитива."),
                new NeedSeed("Внедрение MES на сборочном участке", "it", "Челябинск",
                        "2800000", 90, 2, NeedStatus.OPEN,
                        "40 рабочих мест, две смены, интеграция с 1С:ERP и сканерами маркировки. "
                                + "Нужен подрядчик с опытом на машиностроительных производствах и поддержкой после запуска."),
                new NeedSeed("Закупаем гофрокороба 400×300×300, 20 000 шт/мес", "packaging", "Казань",
                        "760000", 30, 6, NeedStatus.OPEN,
                        "Ежемесячная потребность 20 000 коробов, марка Т-23, печать в одну краску с нашим логотипом. "
                                + "Нужна стабильная отгрузка двумя партиями в месяц."),
                new NeedSeed("Производственные площади 1 200 м² с кран-балкой", "realty", "Екатеринбург",
                        "1300000", 45, 3, NeedStatus.OPEN,
                        "Ищем цех 1 000–1 400 м², высота не менее 8 м, кран-балка 5 т, электрическая мощность от 400 кВт. "
                                + "Долгосрочная аренда от трёх лет, желательно с железнодорожной веткой."),
                new NeedSeed("Подбор 15 операторов ЧПУ, сменный график", "hr", "Челябинск",
                        "420000", 30, 2, NeedStatus.OPEN,
                        "Нужны операторы токарных и фрезерных центров с опытом от двух лет, график 2/2. "
                                + "Готовы рассмотреть аутстаффинг с оформлением и обучением охране труда."),
                new NeedSeed("Лизинг токарного станка с ЧПУ, бюджет 8 млн ₽", "finance", "Ростов-на-Дону",
                        "8000000", 40, 12, NeedStatus.OPEN,
                        "Рассматриваем лизинг нового токарного центра, аванс до 20%, срок 36–48 месяцев. "
                                + "Нужно решение до конца квартала, поставщик уже выбран."),
                new NeedSeed("Оптимизация тарифа на электроэнергию для завода", "energy", "Челябинск",
                        "180000", 21, 2, NeedStatus.OPEN,
                        "Потребление 1,8 млн кВт·ч в год, тариф высокого напряжения. Интересует аудит договора "
                                + "и переход к более выгодной ценовой категории."),
                new NeedSeed("Продвижение на выставке «Металлообработка-2026»", "marketing", "Москва",
                        "380000", 60, 12, NeedStatus.OPEN,
                        "Нужен подрядчик по организации участия: стенд 18 м², раздаточные материалы, работа с лидами "
                                + "и отчёт по контактам после выставки."),
                new NeedSeed("Поставка стрейч-плёнки 500 мм, 200 рулонов", "trade", "Екатеринбург",
                        "210000", 12, 6, NeedStatus.CLOSED,
                        "Разовая закупка 200 рулонов, 17 мкм, 2,4 кг. Нужна отгрузка до конца месяца на наш склад.")
        );

        List<NeedRequest> result = new ArrayList<>(seeds.size());
        for (NeedSeed seed : seeds) {
            Company authorCompany = companyList.get(seed.authorIndex());
            Industry industry = industries.findByCode(seed.industry()).orElseThrow();
            NeedRequest need = new NeedRequest(seed.title(), industry, authorCompany.getOwner());
            need.setDescription(seed.description());
            need.setCity(seed.city());
            need.setBudget(new BigDecimal(seed.budget()));
            need.setDeadline(LocalDate.now().plusDays(seed.days()));
            need.setStatus(seed.status());
            need.setAuthorCompany(authorCompany);
            needs.save(need);
            result.add(need);
        }
        return result;
    }

    // ---------------------------------------------------------------- отклики

    private void seedProposals(List<NeedRequest> needList, List<Company> companyList) {
        List<ProposalSeed> seeds = List.of(
                new ProposalSeed(0, 0, "Готовы отгрузить 12 тонн листа 09Г2С из наличия: 8 мм — 7 тонн, 10 мм — 5 тонн. "
                        + "Резка в размер на ленточной пиле, сертификаты и протоколы предоставим. "
                        + "Цена 1 190 000 ₽ с доставкой на ваш склад в Челябинске, срок — 4 дня.", "1190000", ProposalStatus.NEW),
                new ProposalSeed(0, 18, "Предложим аналог по той же марке с двух заводов, срок 6 дней. "
                        + "Стоимость 1 240 000 ₽, возможна частичная отгрузка уже на третий день.", "1240000", ProposalStatus.NEW),
                new ProposalSeed(1, 6, "Есть свободный блок 900 паллетомест в тёплой зоне, температура +8…+14 °C. "
                        + "Приёмка по счёту, отгрузка по заявке за 2 часа, API к WMS. Стоимость 610 ₽/паллета в месяц.", "585600", ProposalStatus.ACCEPTED),
                new ProposalSeed(1, 7, "Можем предоставить 800 паллетомест в тёплом складе. "
                        + "Отгрузка по заявке в течение 4 часов, доступ к остаткам через личный кабинет.", "544000", ProposalStatus.REJECTED),
                new ProposalSeed(2, 4, "Планируем 12 рейсов тентами 20 тонн с погрузкой на вашем складе. "
                        + "Стоимость рейса 54 000 ₽, GPS-отчёты и страхование включены. Первый рейс — через 5 дней.", "648000", ProposalStatus.NEW),
                new ProposalSeed(2, 5, "Можем собрать сборные отправки и добавить железнодорожное плечо для части объёма. "
                        + "Срок 6–8 дней на партию, цена 52 500 ₽ за рейс при подписании на 3 месяца.", "630000", ProposalStatus.NEW),
                new ProposalSeed(3, 8, "Проверим контрагента по открытым и закрытым источникам, подготовим редакцию контракта "
                        + "с валютными оговорками и поможем с аккредитивом. Фиксированная стоимость 240 000 ₽, срок 3 недели.", "240000", ProposalStatus.NEW),
                new ProposalSeed(4, 10, "Внедрим MES на 40 рабочих мест за 12 недель: аудит, пилот на одном участке, "
                        + "тиражирование, интеграция с 1С и сканерами. Стоимость 2 650 000 ₽, поддержка первые 3 месяца включена.", "2650000", ProposalStatus.NEW),
                new ProposalSeed(5, 13, "Изготовим короба 400×300×300 из Т-23 с печатью в одну краску. "
                        + "20 000 штук в месяц, отгрузка двумя партиями, цена 36,5 ₽ за штуку при годовом контракте.", "730000", ProposalStatus.NEW),
                new ProposalSeed(6, 14, "Есть цех 1 250 м², высота 9 м, кран-балка 5 т, мощность 500 кВт, ж/д ветка на территории. "
                        + "Аренда 590 ₽/м² в месяц, договор от трёх лет, каникулы на первый месяц.", "885000", ProposalStatus.NEW),
                new ProposalSeed(7, 15, "Закроем 15 операторов ЧПУ за 3–4 недели, включая оформление и обучение охране труда. "
                        + "Аутстаффинг 3 100 ₽ за смену, замена в течение 5 дней.", "405000", ProposalStatus.NEW),
                new ProposalSeed(8, 9, "Одобрим лизинг токарного центра: аванс 15%, срок 42 месяца, ставка от 14,9%. "
                        + "Решение за 2 дня после пакета документов, поставщик из вашего города подходит.", "7900000", ProposalStatus.NEW),
                new ProposalSeed(9, 17, "Проведём аудит договора энергоснабжения и предложим переход на первую ценовую категорию. "
                        + "Экономия по предварительной оценке 5–8% в год, стоимость работ 165 000 ₽.", "165000", ProposalStatus.NEW),
                new ProposalSeed(10, 11, "Организуем участие в «Металлообработке-2026»: стенд 18 м², материалы, "
                        + "работа с лидами и отчёт по 120+ контактам. Бюджет 360 000 ₽, включая застройку.", "360000", ProposalStatus.NEW)
        );

        for (ProposalSeed seed : seeds) {
            NeedRequest need = needList.get(seed.needIndex());
            if (seed.companyIndex() < 0 || seed.companyIndex() >= companyList.size()) {
                continue;
            }
            Company responder = companyList.get(seed.companyIndex());
            if (responder.getId().equals(need.getAuthorCompany().getId())) {
                continue;
            }
            Proposal proposal = new Proposal(need, responder, responder.getOwner());
            proposal.setMessage(seed.message());
            proposal.setPrice(new BigDecimal(seed.price()));
            proposal.setStatus(seed.status());
            proposals.save(proposal);
        }

        for (NeedRequest need : needList) {
            int count = proposals.findByNeedIdOrderByCreatedAtDesc(need.getId()).size();
            need.setProposalsCount(count);
            needs.save(need);
        }
    }

    // ------------------------------------------------------- тарифы и переписка

    private void seedPlans(List<Company> companyList) {
        Instant now = Instant.now();
        applyPlan(companyList.get(14), Plan.PREMIUM, now);
        applyPlan(companyList.get(0), Plan.PRO, now);
    }

    private void applyPlan(Company company, Plan plan, Instant now) {
        Instant expires = now.plus(30, ChronoUnit.DAYS);
        company.setPlan(plan);
        company.setPromotedUntil(expires);
        companies.save(company);
        subscriptions.save(new Subscription(company, plan,
                BigDecimal.valueOf(plan.getMonthlyPrice()), now, expires));
    }

    private void seedChats(List<NeedRequest> needList, List<Company> companyList) {
        // Лист 09Г2С: завод ↔ поставщик металлопроката
        ChatThread steel = threads.save(new ChatThread(needList.get(0), companyList.get(0),
                companyList.get(0).getOwner()));
        message(steel, companyList.get(0).getOwner(),
                "Добрый день! Готовы отгрузить 12 тонн листа 09Г2С: 8 мм — 7 тонн, 10 мм — 5 тонн. "
                        + "Резка в размер на ленточной пиле, сертификаты приложим к УПД.");
        message(steel, needList.get(0).getAuthor(),
                "Здравствуйте! Нужны сертификаты и протоколы на партию. Успеете отгрузить 8 мм к пятнице?");
        message(steel, companyList.get(0).getOwner(),
                "Да, 8 мм отгрузим в четверг, 10 мм — в начале следующей недели. "
                        + "Доставку на ваш склад в Челябинске включим в счёт.");

        // Склад: упаковщик ↔ логопарк
        ChatThread warehouse = threads.save(new ChatThread(needList.get(1), companyList.get(6),
                companyList.get(6).getOwner()));
        message(warehouse, companyList.get(6).getOwner(),
                "Есть свободный блок 900 паллетомест в тёплой зоне, +8…+14 °C. "
                        + "Приёмка по счёту, отгрузка по заявке за 2 часа, доступ к остаткам через API.");
        message(warehouse, needList.get(1).getAuthor(),
                "Подходит. Пришлите, пожалуйста, договор и тарифы на комплектацию и маркировку.");

        // ВЭД: поставщик полимеров ↔ юристы
        ChatThread legal = threads.save(new ChatThread(needList.get(3), companyList.get(8),
                companyList.get(8).getOwner()));
        message(legal, companyList.get(8).getOwner(),
                "Проверили контрагента из Гуанчжоу: замечаний нет. Готовим редакцию контракта "
                        + "с валютной оговоркой и условиями аккредитива.");
        message(legal, needList.get(3).getAuthor(),
                "Спасибо! Ждём также комментарий по аккредитиву — банк просит подтверждение условий поставки.");
    }

    private void message(ChatThread thread, User sender, String body) {
        messages.save(new ChatMessage(thread, sender, body));
        thread.registerMessage(body);
        threads.save(thread);
    }

    private String uniqueSlug(String name) {
        String base = Slugifier.slugify(name, "company");
        String candidate = base;
        int suffix = 2;
        while (companies.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    // ------------------------------------------------------------ вспомогательное

    private record CompanySeed(String name, String industry, String city, CompanySize size, int founded,
                               boolean verified, String phone, String website, String description,
                               String ownerName, String ownerEmail, String accent, List<OfferSeed> offers) {
    }

    private record OfferSeed(String title, OfferType type, String price, String unit, String description) {
    }

    private record ReviewSeed(int companyIndex, int authorIndex, int rating, String text) {
    }

    private record NeedSeed(String title, String industry, String city, String budget, int days,
                            int authorIndex, NeedStatus status, String description) {
    }

    private record ProposalSeed(int needIndex, int companyIndex, String message, String price,
                                ProposalStatus status) {
    }
}
