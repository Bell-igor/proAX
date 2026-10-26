import { route, resolveRoute, currentRoute, navigate } from './router.js';
import { loadUser, store, dropUser } from './store.js';
import { toast, esc } from './ui.js';
import { api } from './api.js';

import home from './views/home.js';
import catalog from './views/catalog.js';
import company from './views/company.js';
import needs from './views/needs.js';
import need from './views/need.js';
import cabinet from './views/cabinet.js';
import admin from './views/admin.js';
import chat from './views/chat.js';
import { login, register } from './views/auth.js';
import { industriesPage, howPage } from './views/pages.js';

const viewEl = document.getElementById('view');

function mount(handler) {
    return async (context) => {
        window.scrollTo({ top: 0, behavior: 'instant' });
        try {
            await handler(context);
        } catch (error) {
            renderError(error);
        }
    };
}

function renderError(error) {
    const message = error && error.message ? error.message : 'Не удалось загрузить страницу';
    viewEl.innerHTML = `<div class="container"><div class="page-head">
        <div class="eyebrow">Ошибка</div>
        <h1>Что-то пошло не так</h1>
        <p>${esc(message)}</p>
        <div class="row mt-16">
            <a class="btn btn--dark" href="#/">На главную</a>
            <button class="btn btn--outline" type="button" id="retry">Обновить страницу</button>
        </div>
    </div></div>`;
    viewEl.querySelector('#retry')?.addEventListener('click', () => window.location.reload());
}

route('/', mount(home));
route('/catalog', mount(catalog));
route('/industries', mount(industriesPage));
route('/needs', mount(needs));
route('/need/:id', mount(need));
route('/company/:slug', mount(company));
route('/login', mount(login));
route('/register', mount(register));
route('/cabinet', mount(cabinet));
route('/chat', mount(chat));
route('/chat/:id', mount(chat));
route('/admin', mount(admin));
route('/how', mount(howPage));

// ------------------------------------------------------------ шапка и меню

const nav = document.getElementById('main-nav');
const menuToggle = document.getElementById('menu-toggle');

menuToggle?.addEventListener('click', () => {
    const open = nav.classList.toggle('is-open');
    menuToggle.setAttribute('aria-expanded', String(open));
});

nav?.addEventListener('click', (event) => {
    if (event.target.closest('.nav__link')) {
        nav.classList.remove('is-open');
        menuToggle?.setAttribute('aria-expanded', 'false');
    }
});

function setActiveNav() {
    const { path } = currentRoute();
    nav?.querySelectorAll('.nav__link').forEach((link) => {
        const target = link.getAttribute('href').replace(/^#/, '');
        link.classList.toggle('is-active', path === target || path.startsWith(`${target}/`));
    });
}

function renderAuthZone() {
    const zone = document.getElementById('auth-zone');
    if (!zone) return;
    const user = store.user;
    if (!user) {
        zone.innerHTML = `<a class="btn btn--ghost btn--sm" href="#/login">Войти</a>
            <a class="btn btn--primary btn--sm" href="#/register">Разместить компанию</a>`;
        return;
    }
    zone.innerHTML = `
        ${user.admin ? '<a class="btn btn--outline btn--sm" href="#/admin" style="color:#F6F7F2;border-color:rgba(255,255,255,.3)">Админ</a>' : ''}
        <a class="btn btn--primary btn--sm" href="#/cabinet">
            ${esc(user.companyName || user.displayName)}
        </a>`;
}

document.addEventListener('auth-changed', () => {
    renderAuthZone();
});

// --------------------------------------------------------- общие обработчики

document.addEventListener('click', async (event) => {
    const button = event.target.closest('.js-fav');
    if (!button) return;
    event.preventDefault();

    if (!store.user) {
        toast('Войдите, чтобы сохранять компании в избранное', 'error');
        navigate(`/login?next=${encodeURIComponent(window.location.hash.replace(/^#/, ''))}`);
        return;
    }

    const companyId = Number(button.dataset.id);
    const active = button.classList.contains('is-active');
    button.disabled = true;
    try {
        if (active) {
            await api.removeFavorite(companyId);
            button.classList.remove('is-active');
            button.textContent = '♡';
            toast('Удалено из избранного');
        } else {
            await api.addFavorite(companyId);
            button.classList.add('is-active');
            button.textContent = '♥';
            toast('Добавлено в избранное');
        }
        document.dispatchEvent(new CustomEvent('favorite-changed', { detail: { companyId, active: !active } }));
    } catch (error) {
        handleApiError(error);
    } finally {
        button.disabled = false;
    }
});

window.addEventListener('toast', (event) => toast(event.detail));
window.addEventListener('api-error', (event) => handleApiError(event.detail));

function handleApiError(error) {
    if (!error) return;
    if (error.status === 401) {
        dropUser();
        toast('Сессия истекла — войдите заново', 'error');
        return;
    }
    toast(error.message || 'Ошибка запроса', 'error');
}

window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled', event.reason);
});

// ------------------------------------------------------------------- старт

const yearHolder = document.getElementById('footer-year');
if (yearHolder) yearHolder.textContent = String(new Date().getFullYear());

async function boot() {
    // Прямой заход на /catalog или /cabinet: переносим путь сервера в hash-маршрут
    if (!window.location.hash && window.location.pathname !== '/' && window.location.pathname !== '/index.html') {
        window.location.hash = `#${window.location.pathname}`;
        return;
    }
    await loadUser();
    renderAuthZone();
    setActiveNav();

    const resolved = await resolveRoute({ root: viewEl });
    if (!resolved) {
        navigate('/', { replace: true });
    }
}

window.addEventListener('hashchange', async () => {
    setActiveNav();
    const resolved = await resolveRoute({ root: viewEl });
    if (!resolved) navigate('/', { replace: true });
});

boot();
