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
import pro.ax.platform.domain.Industry;
import pro.ax.platform.repo.IndustryRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозные проверки API: регистрация, заявки, отклики, отзывы и роли.
 * Тестовая база — отдельная H2 в памяти, демо-данные отключены.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:proax-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "proax.demo-data=false"
})
@AutoConfigureMockMvc
class MarketplaceApiTest {

    private static final String RAWTOKEN = "Сырьё и материалы";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private IndustryRepository industries;

    @BeforeEach
    void seedIndustries() {
        if (industries.count() == 0) {
            industries.save(new Industry("raw", RAWTOKEN, "Металл и полимеры", "⛏", 10));
            industries.save(new Industry("logistics", "Логистика и перевозки", "Перевозки", "🚚", 20));
            industries.save(new Industry("warehouse", "Складские помещения", "Склады", "🏬", 30));
        }
    }

    @Test
    void guestCannotMutateApi() throws Exception {
        mvc.perform(post("/api/needs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Нужен металл","industryCode":"raw"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));

        mvc.perform(get("/api/favorites")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationCreatesAccountAndCompany() throws Exception {
        MockHttpSession session = new MockHttpSession();
        register(session, "owner@metall.ru", "МеталлТорг", "raw");

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@metall.ru"))
                .andExpect(jsonPath("$.companyName").value("МеталлТорг"))
                .andExpect(jsonPath("$.admin").value(false));

        mvc.perform(get("/api/companies").param("q", "МеталлТорг"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].verified").value(false));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        register(new MockHttpSession(), "dup@metall.ru", "Первая", "raw");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("dup@metall.ru", "Вторая", "raw")))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        register(new MockHttpSession(), "login@metall.ru", "Логин", "raw");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@metall.ru","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void marketplaceFlowFromNeedToAcceptedProposal() throws Exception {
        MockHttpSession customer = new MockHttpSession();
        register(customer, "customer@zavod.ru", "ЗаводЗаказчик", "warehouse");

        MockHttpSession contractor = new MockHttpSession();
        register(contractor, "contractor@sklad.ru", "СкладИсполнитель", "warehouse");

        String needJson = mvc.perform(post("/api/needs")
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Нужен склад на 500 паллет","industryCode":"warehouse",
                                 "city":"Екатеринбург","budget":400000,"description":"Класс A, температура +5"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        long needId = json.readTree(needJson).get("id").asLong();

        String proposalJson = mvc.perform(post("/api/needs/" + needId + "/proposals")
                        .session(contractor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Есть 600 паллетомест, отгрузка за 4 часа","price":380000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        long proposalId = json.readTree(proposalJson).get("id").asLong();

        // автор заявки видит отклик и принимает его
        mvc.perform(get("/api/needs/" + needId + "/proposals").session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].companyName").value("СкладИсполнитель"));

        mvc.perform(patch("/api/proposals/" + proposalId)
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mvc.perform(get("/api/needs/" + needId).session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.proposalsCount").value(1));
    }

    @Test
    void contractorCannotRespondToOwnNeed() throws Exception {
        MockHttpSession session = new MockHttpSession();
        register(session, "self@zavod.ru", "СамСебе", "raw");

        String needJson = mvc.perform(post("/api/needs")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Нужен лист 3 мм","industryCode":"raw"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long needId = json.readTree(needJson).get("id").asLong();

        mvc.perform(post("/api/needs/" + needId + "/proposals")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Сам себе предложу"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewRulesAreEnforced() throws Exception {
        MockHttpSession owner = new MockHttpSession();
        register(owner, "review-owner@metall.ru", "КомпанияОтзыв", "raw");

        MockHttpSession guest = new MockHttpSession();
        register(guest, "review-guest@log.ru", "Рецензент", "logistics");

        String companyJson = mvc.perform(get("/api/companies").param("q", "КомпанияОтзыв"))
                .andReturn().getResponse().getContentAsString();
        String slug = json.readTree(companyJson).at("/items/0/slug").asText();
        assertThat(slug).isNotBlank();

        mvc.perform(post("/api/companies/" + slug + "/reviews")
                        .session(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"text":"Отзыв о себе"}
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/companies/" + slug + "/reviews")
                        .session(guest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":4,"text":"Быстро отгрузили, рекомендую"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4));

        mvc.perform(post("/api/companies/" + slug + "/reviews")
                        .session(guest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"text":"Повторный отзыв"}
                                """))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/companies/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewsCount").value(1))
                .andExpect(jsonPath("$.rating").value(4.0));
    }

    @Test
    void favoritesAreScopedToUser() throws Exception {
        MockHttpSession owner = new MockHttpSession();
        register(owner, "fav-owner@metall.ru", "ВИзбранном", "raw");

        MockHttpSession other = new MockHttpSession();
        register(other, "fav-other@log.ru", "Избранник", "logistics");

        long companyId = json.readTree(mvc.perform(get("/api/companies").param("q", "ВИзбранном"))
                        .andReturn().getResponse().getContentAsString())
                .at("/items/0/id").asLong();

        mvc.perform(post("/api/favorites/" + companyId).session(other)).andExpect(status().isOk());
        mvc.perform(get("/api/favorites").session(other))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/favorites").session(owner))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void adminSectionIsRestricted() throws Exception {
        MockHttpSession user = new MockHttpSession();
        register(user, "plain@metall.ru", "НеАдмин", "raw");

        mvc.perform(get("/api/admin/pending-companies").session(user))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownCompanyReturnsNotFound() throws Exception {
        mvc.perform(get("/api/companies/no-such-company"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    // ------------------------------------------------------------------ helpers

    private void register(MockHttpSession session, String email, String companyName, String industry) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(email, companyName, industry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    private String registrationJson(String email, String companyName, String industry) throws Exception {
        return json.writeValueAsString(Map.of(
                "email", email,
                "password", "secret123",
                "displayName", "Представитель",
                "companyName", companyName,
                "industryCode", industry,
                "city", "Екатеринбург",
                "description", "Тестовая компания",
                "website", "example.ru",
                "phone", "+7 (343) 000-00-00"
        ));
    }
}
