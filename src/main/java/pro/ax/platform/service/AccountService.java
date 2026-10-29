package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.Industry;
import pro.ax.platform.domain.Role;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.CompanyRepository;
import pro.ax.platform.repo.UserRepository;
import pro.ax.platform.security.PasswordHasher;
import pro.ax.platform.support.Slugifier;
import pro.ax.platform.web.dto.PersonRegisterForm;
import pro.ax.platform.web.dto.RegisterForm;

import java.util.List;
import java.util.Locale;

/** Регистрация участников: аккаунт представителя + карточка компании. */
@Service
public class AccountService {

    private static final List<String> ACCENTS = List.of(
            "#F37022", "#3F7D6E", "#C5621B", "#5F308C", "#8E5BA6", "#4C6FA8", "#C9A227");

    private final UserRepository users;
    private final CompanyRepository companies;
    private final IndustryService industryService;
    private final PasswordHasher hasher;

    public AccountService(UserRepository users, CompanyRepository companies,
                          IndustryService industryService, PasswordHasher hasher) {
        this.users = users;
        this.companies = companies;
        this.industryService = industryService;
        this.hasher = hasher;
    }

    @Transactional
    public User register(RegisterForm form) {
        String email = form.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Пользователь с таким e-mail уже зарегистрирован");
        }
        Industry industry = industryService.require(form.industryCode());

        User user = new User(email, hasher.hash(form.password()), form.displayName().trim(), Role.COMPANY);
        users.save(user);

        Company company = new Company(form.companyName().trim(), uniqueSlug(form.companyName()), industry);
        company.setCity(trimToNull(form.city()));
        company.setDescription(trimToNull(form.description()));
        company.setWebsite(normalizeWebsite(form.website()));
        company.setPhone(trimToNull(form.phone()));
        company.setEmail(email);
        company.setOwner(user);
        company.setAccentColor(accentFor(form.companyName()));
        companies.save(company);

        return user;
    }

    /** Регистрация частного лица: аккаунт без компании, со статусом PERSON. */
    @Transactional
    public User registerPerson(PersonRegisterForm form) {
        String email = form.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Пользователь с таким e-mail уже зарегистрирован");
        }
        User user = new User(email, hasher.hash(form.password()), form.displayName().trim(), Role.PERSON);
        user.setCity(trimToNull(form.city()));
        user.setPhone(trimToNull(form.phone()));
        users.save(user);
        return user;
    }

    /** Создание карточки компании и повышение статуса до юрлица. */
    @Transactional
    public Company createCompany(User owner, String name, String industryCode, String city) {
        if (companies.findByOwnerId(owner.getId()).isPresent()) {
            throw ApiException.conflict("У аккаунта уже есть карточка компании");
        }
        Industry industry = industryService.require(industryCode);
        String cleanName = name == null || name.isBlank() ? "Компания " + owner.getDisplayName() : name.trim();

        Company company = new Company(cleanName, uniqueSlug(cleanName), industry);
        company.setCity(trimToNull(city));
        company.setOwner(owner);
        company.setEmail(owner.getEmail());
        company.setAccentColor(accentFor(cleanName));
        companies.save(company);

        owner.setRole(Role.COMPANY);
        users.save(owner);
        return company;
    }

    @Transactional
    public Company createCompanyFor(User user) {
        if (hasCompany(user)) {
            throw ApiException.conflict("У аккаунта уже есть компания");
        }
        Industry industry = industryService.list().stream()
                .findFirst()
                .map(dto -> industryService.require(dto.code()))
                .orElseThrow(() -> ApiException.badRequest("Справочник отраслей не заполнен"));

        Company company = new Company("Компания " + user.getDisplayName(), uniqueSlug(user.getDisplayName()), industry);
        company.setOwner(user);
        company.setEmail(user.getEmail());
        company.setAccentColor(accentFor(user.getDisplayName()));
        return companies.save(company);
    }

    @Transactional(readOnly = true)
    public boolean hasCompany(User user) {
        return user != null && companies.findByOwnerId(user.getId()).isPresent();
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

    private static String accentFor(String name) {
        return ACCENTS.get(Math.abs(name.hashCode()) % ACCENTS.size());
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String normalizeWebsite(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }
}
