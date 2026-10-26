package pro.ax.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.Industry;
import pro.ax.platform.domain.LegalService;
import pro.ax.platform.domain.LegalTargetStatus;
import pro.ax.platform.repo.CompanyRepository;
import pro.ax.platform.repo.IndustryRepository;
import pro.ax.platform.repo.LegalServiceRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сценарий «частное лицо → юридический статус»: регистрация без компании,
 * каталог юруслуг с расстоянием, заявка, эскроу и подтверждение одноразовым кодом.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:proax-legal;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "proax.demo-data=false",
        "proax.uploads-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class LegalFlowApiTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private IndustryRepository industries;

    @Autowired
    private CompanyRepository companies;

    @Autowired
    private LegalServiceRepository legalServices;

    @BeforeEach
    void seedIndustries() {
        if (industries.count() == 0) {
            industries.save(new Industry("legal", "Юридические услуги", "Юристы", "⚖️", 10));
            industries.save(new Industry("warehouse", "Складские помещения", "Склады", "🏬", 20));
            industries.save(new Industry("logistics", "Логистика и перевозки", "Перевозки", "🚚", 30));
        }
    }

    @Test
    void personRegistersWithoutCompany() throws Exception {
        MockHttpSession person = registerPerson("Екатеринбург");
        String me = mvc.perform(get("/api/auth/me").session(person))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PERSON"))
                .andExpect(jsonPath("$.city").value("Екатеринбург"))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(me).get("companyId").isNull()).isTrue();
        assertThat(json.readTree(me).get("companyName").isNull()).isTrue();
    }

    @Test
    void companyCannotApplyForStatus() throws Exception {
        Company provider = provider("law-mini", "ЮрМини", "Москва");
        long serviceId = service(provider, LegalTargetStatus.IP, 30000, 4);

        MockHttpSession company = registerCompany("client@company.ru", "КомпанияКлиент", "warehouse");
        mvc.perform(post("/api/legal/applications")
                        .session(company)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson(serviceId, "ООО «Новая»", "Москва")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullFlowFromApplicationToLegalEntity() throws Exception {
        Company provider = provider("law-full", "ПравоПолное", "Екатеринбург");
        long serviceId = service(provider, LegalTargetStatus.LLC, 69000, 9);

        MockHttpSession person = registerPerson("Екатеринбург");
        MockHttpSession providerSession = login("law-full@test.ru");

        long applicationId = createApplication(person, serviceId, "ООО «Складской двор»", "Екатеринбург");

        mvc.perform(get("/api/legal/applications").session(person))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].role").value("AUTHOR"))
                .andExpect(jsonPath("$[0].canPay").value(false));

        String appointment = Instant.now().plus(3, ChronoUnit.DAYS).toString();
        mvc.perform(post("/api/legal/applications/" + applicationId + "/schedule")
                        .session(providerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("appointmentAt", appointment))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mvc.perform(post("/api/legal/applications/" + applicationId + "/pay").session(person))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payment.status").value("HELD"))
                .andExpect(jsonPath("$.payment.amount").value(69000));

        // Исполнитель подтверждает выполнение — код уходит заказчику, а не исполнителю
        mvc.perform(post("/api/legal/applications/" + applicationId + "/complete").session(providerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.otpIssued").value(true))
                .andExpect(jsonPath("$.payment.otp").doesNotExist());

        String personView = mvc.perform(get("/api/legal/applications/" + applicationId).session(person))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.otpIssued").value(true))
                .andReturn().getResponse().getContentAsString();
        String otp = json.readTree(personView).at("/payment/otp").asText();
        assertThat(otp).hasSize(6).matches("\\d{6}");

        mvc.perform(post("/api/legal/applications/" + applicationId + "/confirm")
                        .session(providerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", "000000".equals(otp) ? "111111" : "000000"))))
                .andExpect(status().isBadRequest());

        String completed = mvc.perform(post("/api/legal/applications/" + applicationId + "/confirm")
                        .session(providerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", otp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.payment.status").value("RELEASED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(completed).get("status").asText()).isEqualTo("COMPLETED");

        // Статус сменился: у человека появилась карточка компании
        mvc.perform(get("/api/auth/me").session(person))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COMPANY"))
                .andExpect(jsonPath("$.companyName").value("ООО «Складской двор»"));

        // Входящие у исполнителя содержат заявку
        mvc.perform(get("/api/legal/applications/incoming").session(providerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("PROVIDER"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void onlyProviderCanRunProviderSteps() throws Exception {
        Company provider = provider("law-one", "ПравоОдин", "Москва");
        provider("law-two", "ПравоДва", "Казань");
        long serviceId = service(provider, LegalTargetStatus.IP, 38000, 5);

        MockHttpSession person = registerPerson("Москва");
        MockHttpSession otherSession = login("law-two@test.ru");
        long applicationId = createApplication(person, serviceId, "ИП Тестов", "Москва");

        mvc.perform(post("/api/legal/applications/" + applicationId + "/schedule")
                        .session(otherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("appointmentAt", Instant.now().plus(2, ChronoUnit.DAYS).toString()))))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/legal/applications/" + applicationId + "/confirm")
                        .session(person)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", "123456"))))
                .andExpect(status().isForbidden());

        // Чужой человек не видит заявку
        MockHttpSession stranger = registerPerson("Казань");
        mvc.perform(get("/api/legal/applications/" + applicationId).session(stranger))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelRefundsEscrow() throws Exception {
        Company provider = provider("law-refund", "ПравоВозврат", "Москва");
        long serviceId = service(provider, LegalTargetStatus.SELF_EMPLOYED, 12000, 2);

        MockHttpSession person = registerPerson("Москва");
        MockHttpSession providerSession = login("law-refund@test.ru");
        long applicationId = createApplication(person, serviceId, "ИП Возврат", "Москва");

        mvc.perform(post("/api/legal/applications/" + applicationId + "/schedule")
                        .session(providerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("appointmentAt", Instant.now().plus(1, ChronoUnit.DAYS).toString()))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/legal/applications/" + applicationId + "/pay").session(person))
                .andExpect(status().isOk());

        mvc.perform(post("/api/legal/applications/" + applicationId + "/cancel").session(person))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.payment.status").value("REFUNDED"));
    }

    @Test
    void nearestServicesComeFirst() throws Exception {
        Company near = provider("law-near", "ЮрРядом", "Екатеринбург");
        Company far = provider("law-far", "ЮрДалеко", "Москва");
        service(near, LegalTargetStatus.IP, 38000, 4);
        service(far, LegalTargetStatus.IP, 35000, 4);

        MockHttpSession person = registerPerson("Екатеринбург");
        String body = mvc.perform(get("/api/legal/services").param("sort", "near").session(person))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode items = json.readTree(body);

        int nearIndex = -1;
        int farIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            String providerName = items.get(i).get("providerName").asText();
            if ("ЮрРядом".equals(providerName)) nearIndex = i;
            if ("ЮрДалеко".equals(providerName)) farIndex = i;
        }
        assertThat(nearIndex).describedAs(body).isGreaterThanOrEqualTo(0);
        assertThat(farIndex).isGreaterThan(nearIndex);
        assertThat(items.get(nearIndex).get("distanceKm").asInt()).isZero();
        assertThat(items.get(farIndex).get("distanceKm").asInt()).isGreaterThan(1000);
    }

    // ------------------------------------------------------------------ helpers

    private MockHttpSession registerPerson(String city) throws Exception {
        int index = COUNTER.incrementAndGet();
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/register-person")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", "person" + index + "@test.ru",
                                "password", "secret123",
                                "displayName", "Иван Петров",
                                "city", city,
                                "phone", "+7 (343) 000-00-00"))))
                .andExpect(status().isOk());
        return session;
    }

    private MockHttpSession registerCompany(String email, String companyName, String industry) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email,
                                "password", "secret123",
                                "displayName", "Представитель",
                                "companyName", companyName,
                                "industryCode", industry,
                                "city", "Москва"))))
                .andExpect(status().isOk());
        return session;
    }

    private MockHttpSession login(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "secret123"))))
                .andExpect(status().isOk());
        return session;
    }

    private Company provider(String slug, String name, String city) throws Exception {
        MockHttpSession session = registerCompany(slug + "@test.ru", name, "legal");
        String me = mvc.perform(get("/api/auth/me").session(session))
                .andReturn().getResponse().getContentAsString();
        long companyId = json.readTree(me).get("companyId").asLong();
        Company company = companies.findById(companyId).orElseThrow();
        company.setCity(city);
        return companies.save(company);
    }

    private long service(Company provider, LegalTargetStatus target, int price, int days) {
        LegalService service = new LegalService(provider, "Услуга " + target.name(), target);
        service.setPrice(BigDecimal.valueOf(price));
        service.setDurationDays(days);
        service.setDescription("Тестовая юридическая услуга");
        return legalServices.save(service).getId();
    }

    private long createApplication(MockHttpSession session, long serviceId, String companyName, String city)
            throws Exception {
        String body = mvc.perform(post("/api/legal/applications")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson(serviceId, companyName, city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private String applicationJson(long serviceId, String companyName, String city) throws Exception {
        return json.writeValueAsString(Map.of(
                "serviceId", serviceId,
                "companyName", companyName,
                "city", city,
                "industryCode", "warehouse",
                "comment", "Хочу оформить статус для сдачи склада в аренду"));
    }
}
