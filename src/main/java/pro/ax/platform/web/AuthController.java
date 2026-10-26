package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.AccountService;
import pro.ax.platform.service.CompanyService;
import pro.ax.platform.web.dto.LoginForm;
import pro.ax.platform.web.dto.PersonRegisterForm;
import pro.ax.platform.web.dto.RegisterForm;
import pro.ax.platform.web.dto.UserDto;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;
    private final CompanyService companyService;
    private final DtoMapper mapper;

    public AuthController(AuthService authService, AccountService accountService,
                          CompanyService companyService, DtoMapper mapper) {
        this.authService = authService;
        this.accountService = accountService;
        this.companyService = companyService;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    public UserDto register(@Valid @RequestBody RegisterForm form, HttpSession session) {
        User user = accountService.register(form);
        authService.login(session, user);
        return mapper.user(user, companyService.findMyCompany(user).orElse(null));
    }

    /** Регистрация частного лица: без компании, со статусом PERSON. */
    @PostMapping("/register-person")
    public UserDto registerPerson(@Valid @RequestBody PersonRegisterForm form, HttpSession session) {
        User user = accountService.registerPerson(form);
        authService.login(session, user);
        return mapper.user(user, null);
    }

    @PostMapping("/login")
    public UserDto login(@Valid @RequestBody LoginForm form, HttpSession session) {
        User user = authService.authenticate(form.email(), form.password());
        authService.login(session, user);
        return mapper.user(user, companyService.findMyCompany(user).orElse(null));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        authService.logout(session);
        return Map.of("ok", true);
    }

    /** Возвращает текущего пользователя или пустое тело для гостя. */
    @GetMapping("/me")
    public UserDto me(HttpSession session) {
        return authService.currentUser(session)
                .map(user -> mapper.user(user, companyService.findMyCompany(user).orElse(null)))
                .orElse(null);
    }
}
