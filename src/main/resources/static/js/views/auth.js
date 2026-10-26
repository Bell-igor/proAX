import { api } from '../api.js';
import { esc, toast, markFields } from '../ui.js';
import { setUser } from '../store.js';
import { navigate } from '../router.js';

async function finish(user, query) {
    setUser(user);
    const next = query.next ? `/${String(query.next).replace(/^\//, '')}` : (user.admin ? '/admin' : '/cabinet');
    navigate(next);
}

export async function login({ root, query }) {
    root.innerHTML = `<div class="auth-wrap"><div class="container">
        <div class="auth-card">
            <h1>Вход в ProAX</h1>
            <p>Войдите, чтобы управлять профилем компании, публиковать заявки и откликаться на потребности.</p>
            <form id="login-form" novalidate>
                <div class="form-error hidden"></div>
                <div class="field">
                    <label class="field__label" for="login-email">E-mail</label>
                    <input class="input" id="login-email" name="email" type="email" autocomplete="email"
                           placeholder="you@company.ru" required>
                </div>
                <div class="field">
                    <label class="field__label" for="login-password">Пароль</label>
                    <input class="input" id="login-password" name="password" type="password"
                           autocomplete="current-password" placeholder="••••••" required>
                </div>
                <button class="btn btn--primary btn--block" type="submit">Войти</button>
            </form>

            <div class="divider"></div>
            <p class="small muted mb-0">Демо-доступ:</p>
            <div class="row mt-16">
                <button class="btn btn--outline btn--sm" type="button" data-demo="demo@proax.ru|demo123">
                    Компания «УралМеталл»
                </button>
                <button class="btn btn--outline btn--sm" type="button" data-demo="admin@proax.ru|admin123">
                    Администратор
                </button>
            </div>

            <div class="auth-switch">Нет аккаунта? <a href="#/register">Зарегистрировать компанию</a></div>
        </div>
    </div></div>`;

    const form = root.querySelector('#login-form');
    const errorBox = form.querySelector('.form-error');

    const submit = async (email, password) => {
        errorBox.classList.add('hidden');
        try {
            const user = await api.login({ email, password });
            toast(`Добро пожаловать, ${user.displayName}`);
            await finish(user, query);
        } catch (error) {
            errorBox.textContent = error.message;
            errorBox.classList.remove('hidden');
            markFields(form, error.fields);
        }
    };

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const payload = Object.fromEntries(new FormData(form).entries());
        await submit(payload.email.trim(), payload.password);
    });

    root.querySelectorAll('[data-demo]').forEach((button) => {
        button.addEventListener('click', async () => {
            const [email, password] = button.dataset.demo.split('|');
            form.querySelector('[name="email"]').value = email;
            form.querySelector('[name="password"]').value = password;
            await submit(email, password);
        });
    });
}

export async function register({ root, query }) {
    root.innerHTML = `<div class="auth-wrap"><div class="container"><div class="auth-card auth-card--wide">
        <div class="skeleton" style="height:300px"></div></div></div></div>`;

    const industries = await api.industries();

    root.innerHTML = `<div class="auth-wrap"><div class="container">
        <div class="auth-card auth-card--wide">
            <h1>Разместить компанию</h1>
            <p>Аккаунт представителя и карточка компании создаются одновременно. После регистрации профиль
                появится в каталоге, а вы сможете публиковать заявки и откликаться.</p>
            <form id="register-form" novalidate>
                <div class="form-error hidden"></div>

                <div class="form-note">Шаг 1 · Аккаунт представителя</div>
                <div class="form-grid">
                    <div class="field">
                        <label class="field__label" for="reg-name">Имя и фамилия <span class="req">*</span></label>
                        <input class="input" id="reg-name" name="displayName" maxlength="120" required
                               placeholder="Игорь Соколов">
                    </div>
                    <div class="field">
                        <label class="field__label" for="reg-email">Рабочий e-mail <span class="req">*</span></label>
                        <input class="input" id="reg-email" name="email" type="email" maxlength="180" required
                               autocomplete="email" placeholder="you@company.ru">
                    </div>
                    <div class="field">
                        <label class="field__label" for="reg-password">Пароль <span class="req">*</span></label>
                        <input class="input" id="reg-password" name="password" type="password" minlength="6"
                               maxlength="72" required autocomplete="new-password" placeholder="Минимум 6 символов">
                    </div>
                    <div class="field">
                        <label class="field__label" for="reg-phone">Телефон</label>
                        <input class="input" id="reg-phone" name="phone" maxlength="40" placeholder="+7 (343) 000-00-00">
                    </div>
                </div>

                <div class="form-note mt-24">Шаг 2 · Карточка компании</div>
                <div class="form-grid">
                    <div class="field">
                        <label class="field__label" for="reg-company">Название компании <span class="req">*</span></label>
                        <input class="input" id="reg-company" name="companyName" maxlength="160" required
                               placeholder="ООО «УралМеталл»">
                    </div>
                    <div class="field">
                        <label class="field__label" for="reg-industry">Отрасль <span class="req">*</span></label>
                        <select class="select" id="reg-industry" name="industryCode" required>
                            ${industries.map((industry) =>
                                `<option value="${esc(industry.code)}">${esc(industry.title)}</option>`).join('')}
                        </select>
                    </div>
                    <div class="field">
                        <label class="field__label" for="reg-city">Город</label>
                        <input class="input" id="reg-city" name="city" maxlength="80" placeholder="Екатеринбург">
                    </div>
                    <div class="field">
                        <label class="field__label" for="reg-website">Сайт</label>
                        <input class="input" id="reg-website" name="website" maxlength="255" placeholder="company.ru">
                    </div>
                    <div class="field field--full">
                        <label class="field__label" for="reg-description">Чем занимается компания</label>
                        <textarea class="textarea" id="reg-description" name="description" maxlength="4000"
                                  placeholder="Опишите продукты, услуги, регионы работы и опыт"></textarea>
                    </div>
                </div>

                <button class="btn btn--primary btn--block" type="submit">Создать аккаунт и компанию</button>
                <p class="muted small center mt-16 mb-0">Регистрируясь, вы подтверждаете, что представляете указанную компанию.</p>
            </form>
            <div class="auth-switch">Уже есть аккаунт? <a href="#/login">Войти</a></div>
        </div>
    </div></div>`;

    const form = root.querySelector('#register-form');
    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const errorBox = form.querySelector('.form-error');
        errorBox.classList.add('hidden');
        const payload = Object.fromEntries(new FormData(form).entries());
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        button.textContent = 'Создаём…';
        try {
            const user = await api.register({
                email: payload.email,
                password: payload.password,
                displayName: payload.displayName,
                companyName: payload.companyName,
                industryCode: payload.industryCode,
                city: payload.city || null,
                description: payload.description || null,
                website: payload.website || null,
                phone: payload.phone || null
            });
            toast('Компания создана. Добро пожаловать в ProAX!');
            await finish(user, query);
        } catch (error) {
            errorBox.textContent = error.message;
            errorBox.classList.remove('hidden');
            markFields(form, error.fields);
        } finally {
            button.disabled = false;
            button.textContent = 'Создать аккаунт и компанию';
        }
    });
}
