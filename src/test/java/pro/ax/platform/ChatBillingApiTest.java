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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import pro.ax.platform.domain.Industry;
import pro.ax.platform.repo.IndustryRepository;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Переписка по заявкам, тарифы с продвижением и загрузка файлов.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:proax-chat;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "proax.demo-data=false",
        "proax.uploads-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class ChatBillingApiTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private IndustryRepository industries;

    @BeforeEach
    void seedIndustries() {
        if (industries.count() == 0) {
            industries.save(new Industry("raw", "Сырьё и материалы", "Металл", "⛏", 10));
            industries.save(new Industry("warehouse", "Складские помещения", "Склады", "🏬", 20));
            industries.save(new Industry("logistics", "Логистика и перевозки", "Перевозки", "🚚", 30));
        }
    }

    @Test
    void proposalOpensDialogForBothSides() throws Exception {
        MockHttpSession customer = register("chat-customer@zavod.ru", "ЗаявкаЧат", "raw");
        MockHttpSession contractor = register("chat-contractor@sklad.ru", "ОткликЧат", "warehouse");

        long needId = createNeed(customer, "Нужен склад для переписки");

        mvc.perform(post("/api/needs/" + needId + "/proposals")
                        .session(contractor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Предлагаю 300 паллетомест","price":150000}
                                """))
                .andExpect(status().isOk());

        // Диалог появился у обеих сторон с правильной ролью
        String contractorThreads = mvc.perform(get("/api/threads").session(contractor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("RESPONDER"))
                .andReturn().getResponse().getContentAsString();
        long threadId = json.readTree(contractorThreads).get(0).get("id").asLong();

        mvc.perform(get("/api/threads").session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("AUTHOR"))
                .andExpect(jsonPath("$[0].id").value(threadId));

        mvc.perform(post("/api/threads/" + threadId + "/messages")
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Здравствуйте! Уточните сроки приёмки."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mine").value(true));

        mvc.perform(post("/api/threads/" + threadId + "/messages")
                        .session(contractor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Можем принять груз в понедельник."}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/threads/" + threadId).session(contractor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thread.messagesCount").value(2))
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].mine").value(false))
                .andExpect(jsonPath("$.messages[1].mine").value(true));
    }

    @Test
    void outsiderCannotOpenOrReadDialog() throws Exception {
        MockHttpSession customer = register("outsider-customer@zavod.ru", "ЗаявкаГость", "raw");
        MockHttpSession contractor = register("outsider-contractor@sklad.ru", "ОткликГость", "warehouse");
        long needId = createNeed(customer, "Заявка для проверки доступа");
        mvc.perform(post("/api/needs/" + needId + "/proposals")
                        .session(contractor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Берёмся"}
                                """))
                .andExpect(status().isOk());

        MockHttpSession stranger = register("outsider-third@log.ru", "ТретийЛишний", "logistics");
        mvc.perform(post("/api/threads/ensure")
                        .session(stranger)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("needId", needId, "companyId", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void dialogRequiresProposal() throws Exception {
        MockHttpSession author = register("noprops-author@zavod.ru", "БезОтклика", "raw");
        MockHttpSession other = register("noprops-other@sklad.ru", "КомпанияБезОтклика", "warehouse");
        long needId = createNeed(author, "Заявка без откликов");

        String ownCompany = mvc.perform(get("/api/auth/me").session(other))
                .andReturn().getResponse().getContentAsString();
        long companyId = json.readTree(ownCompany).get("companyId").asLong();

        mvc.perform(post("/api/threads/ensure")
                        .session(author)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("needId", needId, "companyId", companyId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upgradeActivatesPromotionAndShowsInCatalog() throws Exception {
        MockHttpSession owner = register("billing-owner@promo.ru", "ПромоКомпания", "raw");

        mvc.perform(get("/api/billing").session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("FREE"))
                .andExpect(jsonPath("$.promoted").value(false))
                .andExpect(jsonPath("$.plans.length()").value(3));

        mvc.perform(post("/api/billing/upgrade")
                        .session(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plan":"PRO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("PRO"))
                .andExpect(jsonPath("$.promoted").value(true))
                .andExpect(jsonPath("$.history.length()").value(1));

        String catalog = mvc.perform(get("/api/companies").param("q", "ПромоКомпания"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].planCode").value("PRO"))
                .andExpect(jsonPath("$.items[0].promoted").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(catalog).at("/items/0/planRank").isMissingNode()).isTrue();

        mvc.perform(post("/api/billing/upgrade")
                        .session(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plan":"FREE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("FREE"))
                .andExpect(jsonPath("$.promoted").value(false));
    }

    @Test
    void logoAndDocumentsAreUploadedAndServed() throws Exception {
        MockHttpSession owner = register("upload-owner@files.ru", "ФайловаяКомпания", "raw");

        String logoResponse = mvc.perform(multipart("/api/cabinet/logo")
                        .file(new MockMultipartFile("file", "logo.png", "image/png", PNG))
                        .session(owner))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String logoUrl = json.readTree(logoResponse).get("logoUrl").asText();
        assertThat(logoUrl).startsWith("/uploads/logos/").endsWith(".png");

        mvc.perform(get(logoUrl)).andExpect(status().isOk());

        mvc.perform(multipart("/api/cabinet/logo")
                        .file(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes()))
                        .session(owner))
                .andExpect(status().isBadRequest());

        String documentResponse = mvc.perform(multipart("/api/cabinet/documents")
                        .file(new MockMultipartFile("file", "charter.pdf", "application/pdf", "pdf-bytes".getBytes()))
                        .param("title", "Устав компании")
                        .session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Устав компании"))
                .andReturn().getResponse().getContentAsString();
        long documentId = json.readTree(documentResponse).get("id").asLong();

        mvc.perform(get("/api/cabinet").session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents.length()").value(1))
                .andExpect(jsonPath("$.company.logoUrl").value(logoUrl));

        mvc.perform(delete("/api/cabinet/documents/" + documentId).session(owner))
                .andExpect(status().isOk());

        mvc.perform(get("/api/cabinet").session(owner))
                .andExpect(jsonPath("$.documents.length()").value(0));
    }

    // ------------------------------------------------------------------ helpers

    private MockHttpSession register(String email, String companyName, String industry) throws Exception {
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
                                "city", "Екатеринбург"
                        ))))
                .andExpect(status().isOk());
        return session;
    }

    private long createNeed(MockHttpSession session, String title) throws Exception {
        String body = mvc.perform(post("/api/needs")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "title", title,
                                "industryCode", "warehouse",
                                "budget", 200000))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("id").asLong();
    }
}
