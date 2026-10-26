import { api } from '../api.js';
import {
    esc, money, relative, plural, monogram, verifiedBadge, needStatusBadge, planBadge,
    proposalStatusBadge, companyCard, emptyState, toast, openModal, fmtDate
} from '../ui.js';
import { store, setUser } from '../store.js';
import { navigate } from '../router.js';
import { openNeedForm } from './needs.js';

const TABS = [
    ['profile', 'Профиль компании'],
    ['offers', 'Предложения'],
    ['needs', 'Мои заявки'],
    ['incoming', 'Входящие отклики'],
    ['proposals', 'Мои отклики'],
    ['billing', 'Тариф и продвижение'],
    ['favorites', 'Избранное']
];

let favoriteListenerBound = false;

export default async function cabinet({ root, query }) {
    if (!store.user) {
        navigate('/login?next=/cabinet');
        return;
    }

    root.innerHTML = `<div class="container"><div class="page-head"><h1>Личный кабинет</h1></div>
        <div class="skeleton" style="height:320px"></div></div>`;

    let data = await api.cabinet();
    let industries = await api.industries();
    let tab = TABS.some(([key]) => key === query.tab) ? query.tab : 'profile';
    let billing = null;

    const loadBilling = async () => {
        try {
            billing = await api.billing();
        } catch {
            billing = null;
        }
        return billing;
    };

    if (tab === 'billing') {
        await loadBilling();
    }

    const reload = async () => {
        data = await api.cabinet();
        render();
    };

    if (!favoriteListenerBound) {
        favoriteListenerBound = true;
        document.addEventListener('favorite-changed', () => {
            if (window.location.hash.startsWith('#/cabinet')) reload();
        });
    }

    const render = () => {
        const company = data.company;
        const hasCompany = Boolean(company);
        const counts = {
            offers: company ? company.offers.length : 0,
            needs: data.needs.length,
            incoming: data.incoming.filter((item) => item.status === 'NEW').length,
            proposals: data.proposals.length,
            favorites: data.favorites.length
        };

        const visibleTabs = hasCompany ? TABS : [['profile', 'Профиль']];
        if (!hasCompany && tab !== 'profile') {
            tab = 'profile';
        }

        const nav = visibleTabs.map(([key, label]) => `
            <button class="cabinet-nav__item ${tab === key ? 'is-active' : ''}" data-tab="${key}">
                <span>${label}</span>
                ${counts[key] ? `<span class="cabinet-nav__count">${counts[key]}</span>` : ''}
            </button>`).join('');

        root.innerHTML = `<div class="container">
            <div class="page-head">
                <div class="row row--between" style="align-items:flex-end">
                    <div>
                        <div class="eyebrow">Личный кабинет</div>
                        <h1>${esc(data.user.displayName)}</h1>
                        <p>${esc(data.user.email)}
                            ${data.user.city ? ` · ${esc(data.user.city)}` : ''}
                            ${data.user.admin ? ' · администратор площадки' : ''}
                            ${!hasCompany && !data.user.admin ? ' · частное лицо' : ''}</p>
                    </div>
                    <div class="row">
                        ${company ? `<a class="btn btn--outline btn--sm" href="#/company/${esc(company.slug)}">Публичная страница</a>` : ''}
                        <button class="btn btn--outline btn--sm" type="button" id="logout">Выйти</button>
                    </div>
                </div>
            </div>

            <div class="cabinet-layout">
                <nav class="cabinet-nav">${nav}
                    <a class="cabinet-nav__item" href="#/legal">Юруслуги</a>
                    <a class="cabinet-nav__item" href="#/legal/applications">Заявки на статус</a>
                    ${hasCompany ? `<a class="cabinet-nav__item" href="#/legal/applications?tab=incoming">
                        Входящие заявки
                        ${data.legalIncoming ? `<span class="cabinet-nav__count">${data.legalIncoming}</span>` : ''}
                    </a>` : ''}
                    <a class="cabinet-nav__item" href="#/chat">Сообщения
                        ${data.threadsCount ? `<span class="cabinet-nav__count">${data.threadsCount}</span>` : ''}
                    </a>
                    ${data.user.admin ? '<a class="cabinet-nav__item" href="#/admin">Админ-панель →</a>' : ''}
                </nav>
                <div id="cabinet-body">
                    ${hasCompany ? renderTab(tab) : (data.user.admin ? noCompanyBlock() : personBlock())}
                </div>
            </div>
            <div style="height:48px"></div>
        </div>`;

        root.querySelectorAll('[data-tab]').forEach((button) => {
            button.addEventListener('click', async () => {
                tab = button.dataset.tab;
                if (tab === 'billing' && !billing) {
                    await loadBilling();
                }
                window.history.replaceState(null, '', `#/cabinet?tab=${tab}`);
                render();
            });
        });

        root.querySelector('#logout')?.addEventListener('click', async () => {
            try {
                await api.logout();
            } finally {
                setUser(null);
                toast('Вы вышли из аккаунта');
                navigate('/');
            }
        });

        bindTab(tab);
    };

    const bindTab = (current) => {
        if (current === 'profile') bindProfile();
        if (current === 'offers') bindOffers();
        if (current === 'needs') bindNeeds();
        if (current === 'incoming') bindIncoming();
        if (current === 'billing') bindBilling();
        if (current === 'favorites') bindFavorites();
    };

    const noCompanyBlock = () => `<div class="cabinet-card">
        ${emptyState('Нет карточки компании',
            'Аккаунт создан без компании. Заполните профиль, чтобы участвовать в каталоге и получать отклики.',
            '<a class="btn btn--dark" href="#/admin">Перейти в админ-панель</a>')}
    </div>`;

    /** Кабинет частного лица: профиль, юридический статус и путь к карточке компании. */
    const personBlock = () => `<div class="cabinet-card">
        <div class="cabinet-head">
            <div><h2>Профиль частного лица</h2>
                <p>Имя, контакты и город — по городу площадка подбирает исполнителей рядом</p></div>
            <span class="badge badge--pending">без юридического статуса</span>
        </div>

        <dl class="kv">
            <dt>Имя</dt><dd>${esc(data.user.displayName)}</dd>
            <dt>E-mail</dt><dd>${esc(data.user.email)}</dd>
            <dt>Город</dt><dd>${data.user.city ? esc(data.user.city) : 'не указан'}</dd>
            <dt>Телефон</dt><dd>${data.user.phone ? esc(data.user.phone) : 'не указан'}</dd>
        </dl>

        <div class="divider"></div>

        <h3>Следующий шаг — юридический статус</h3>
        <p class="muted">Чтобы сдавать актив в аренду или работать с компаниями, нужен статус ИП, ООО
            или самозанятого. Выберите юрфирму рядом: она подготовит документы, согласует очную явку,
            а оплата уйдёт ей только после подтверждения услуги кодом.</p>
        <div class="form-actions">
            <a class="btn btn--primary" href="#/legal">Выбрать юрфирму</a>
            <a class="btn btn--outline" href="#/legal/applications">Мои заявки</a>
        </div>

        <div class="form-note mt-24">
            После подтверждения услуги площадка сама создаст карточку компании, и здесь появятся
            предложения, заявки, отклики и документы.
        </div>
    </div>`;

    const renderTab = (current) => {
        if (current === 'profile') return profileTab();
        if (current === 'offers') return offersTab();
        if (current === 'needs') return needsTab();
        if (current === 'incoming') return incomingTab();
        if (current === 'proposals') return proposalsTab();
        if (current === 'billing') return billingTab();
        if (current === 'favorites') return favoritesTab();
        return '';
    };

    // ------------------------------------------------------------ профиль

    const profileTab = () => {
        const company = data.company;
        return `<div class="cabinet-card">
            <div class="cabinet-head">
                <div class="row" style="gap:14px">
                    ${monogram(company)}
                    <div>
                        <h2>${esc(company.name)}</h2>
                        <p>${verifiedBadge(company.verified)} ${planBadge(company)} · ${company.reviewsCount}
                            ${plural(company.reviewsCount, ['отзыв', 'отзыва', 'отзывов'])} · рейтинг
                            ${company.rating ? company.rating.toFixed(1) : '—'}</p>
                    </div>
                </div>
            </div>
            <form id="profile-form" novalidate>
                <div class="form-error hidden"></div>
                <div class="form-grid">
                    <div class="field">
                        <label class="field__label" for="pf-name">Название <span class="req">*</span></label>
                        <input class="input" id="pf-name" name="name" maxlength="160" required value="${esc(company.name)}">
                    </div>
                    <div class="field">
                        <label class="field__label" for="pf-industry">Отрасль <span class="req">*</span></label>
                        <select class="select" id="pf-industry" name="industryCode" required>
                            ${industries.map((industry) => `<option value="${esc(industry.code)}"
                                ${industry.code === company.industryCode ? 'selected' : ''}>${esc(industry.title)}</option>`).join('')}
                        </select>
                    </div>
                    <div class="field">
                        <label class="field__label" for="pf-city">Город</label>
                        <input class="input" id="pf-city" name="city" maxlength="80" value="${esc(company.city || '')}">
                    </div>
                    <div class="field">
                        <label class="field__label" for="pf-size">Размер компании</label>
                        <select class="select" id="pf-size" name="size">
                            ${[['MICRO', 'до 15 сотрудников'], ['SMALL', '15–100 сотрудников'],
                               ['MEDIUM', '100–500 сотрудников'], ['LARGE', 'более 500 сотрудников']]
                                .map(([value, label]) => `<option value="${value}"
                                    ${company.sizeTitle === label ? 'selected' : ''}>${label}</option>`).join('')}
                        </select>
                    </div>
                    <div class="field">
                        <label class="field__label" for="pf-founded">Год основания</label>
                        <input class="input" id="pf-founded" name="foundedYear" type="number" min="1800" max="2100"
                               value="${company.foundedYear || ''}">
                    </div>
                    <div class="field">
                        <label class="field__label" for="pf-phone">Телефон</label>
                        <input class="input" id="pf-phone" name="phone" maxlength="40" value="${esc(company.phone || '')}">
                    </div>
                    <div class="field">
                        <label class="field__label" for="pf-email">E-mail для связи</label>
                        <input class="input" id="pf-email" name="email" type="email" maxlength="180"
                               value="${esc(company.email || '')}">
                    </div>
                    <div class="field">
                        <label class="field__label" for="pf-website">Сайт</label>
                        <input class="input" id="pf-website" name="website" maxlength="255"
                               value="${esc(company.website || '')}">
                    </div>
                    <div class="field field--full">
                        <label class="field__label" for="pf-description">Описание</label>
                        <textarea class="textarea" id="pf-description" name="description" maxlength="4000"
                                  style="min-height:150px">${esc(company.description || '')}</textarea>
                    </div>
                </div>
                <div class="form-actions">
                    <button class="btn btn--primary" type="submit">Сохранить профиль</button>
                    <span class="muted small">Изменения сразу видны в каталоге</span>
                </div>
            </form>

            <div class="divider"></div>
            <div class="logo-preview">
                ${monogram(company)}
                <div>
                    <div class="field__label">Логотип компании</div>
                    <p class="muted small mb-0">PNG, JPG или WebP до 2 МБ. Показывается в каталоге и в карточке
                        вместо монограммы.</p>
                </div>
                <div class="upload-row" style="margin-left:auto">
                    <input type="file" id="logo-file" class="hidden" accept="image/png,image/jpeg,image/webp">
                    <button class="btn btn--outline btn--sm" type="button" id="logo-upload">
                        ${company.logoUrl ? 'Заменить логотип' : 'Загрузить логотип'}
                    </button>
                    ${company.logoUrl ? '<button class="btn btn--danger btn--sm" type="button" id="logo-remove">Удалить</button>' : ''}
                </div>
            </div>

            <div class="divider"></div>
            <div class="row row--between" style="margin-bottom:8px">
                <h3 class="mb-0">Документы компании</h3>
                <span class="muted small">${data.documents.length}
                    ${plural(data.documents.length, ['документ', 'документа', 'документов'])}</span>
            </div>
            <p class="muted small">Устав, лицензии, сертификаты. Видны только вам и администратору площадки.</p>
            ${documentsHtml(data.documents)}
            <form id="doc-form" class="upload-row" style="margin-top:14px">
                <input class="input" id="doc-title" name="title" maxlength="160"
                       placeholder="Название документа" style="max-width:260px">
                <input class="input" id="doc-file" name="file" type="file" accept=".pdf,.png,.jpg,.jpeg"
                       style="max-width:280px">
                <button class="btn btn--primary btn--sm" type="submit">Загрузить документ</button>
            </form>
        </div>`;
    };

    const bindProfile = () => {
        const form = root.querySelector('#profile-form');
        if (!form) return;
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const errorBox = form.querySelector('.form-error');
            errorBox.classList.add('hidden');
            const payload = Object.fromEntries(new FormData(form).entries());
            const button = form.querySelector('button[type="submit"]');
            button.disabled = true;
            try {
                await api.updateCompany(data.company.id, {
                    name: payload.name,
                    industryCode: payload.industryCode,
                    city: payload.city || null,
                    description: payload.description || null,
                    website: payload.website || null,
                    phone: payload.phone || null,
                    email: payload.email || null,
                    size: payload.size,
                    foundedYear: payload.foundedYear ? Number(payload.foundedYear) : null
                });
                toast('Профиль сохранён');
                await reload();
            } catch (error) {
                errorBox.textContent = error.message;
                errorBox.classList.remove('hidden');
            } finally {
                button.disabled = false;
            }
        });

        const logoInput = root.querySelector('#logo-file');
        root.querySelector('#logo-upload')?.addEventListener('click', () => logoInput.click());
        logoInput?.addEventListener('change', async () => {
            const file = logoInput.files && logoInput.files[0];
            if (!file) return;
            const formData = new FormData();
            formData.append('file', file);
            try {
                await api.uploadLogo(formData);
                toast('Логотип обновлён');
                await reload();
            } catch (error) {
                toast(error.message, 'error');
            }
        });

        root.querySelector('#logo-remove')?.addEventListener('click', async () => {
            if (!window.confirm('Удалить логотип?')) return;
            try {
                await api.removeLogo();
                toast('Логотип удалён');
                await reload();
            } catch (error) {
                toast(error.message, 'error');
            }
        });

        const docForm = root.querySelector('#doc-form');
        docForm?.addEventListener('submit', async (event) => {
            event.preventDefault();
            const fileInput = docForm.querySelector('#doc-file');
            const file = fileInput.files && fileInput.files[0];
            if (!file) {
                toast('Выберите файл документа', 'error');
                return;
            }
            const formData = new FormData();
            formData.append('file', file);
            const title = docForm.querySelector('#doc-title').value.trim();
            if (title) formData.append('title', title);
            const button = docForm.querySelector('button[type="submit"]');
            button.disabled = true;
            try {
                await api.uploadDocument(formData);
                toast('Документ загружен');
                await reload();
            } catch (error) {
                toast(error.message, 'error');
            } finally {
                button.disabled = false;
            }
        });

        root.querySelectorAll('[data-doc-delete]').forEach((button) => {
            button.addEventListener('click', async () => {
                if (!window.confirm('Удалить документ?')) return;
                try {
                    await api.deleteDocument(Number(button.dataset.docDelete));
                    toast('Документ удалён');
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                }
            });
        });
    };

    const documentsHtml = (documents) => {
        if (!documents.length) {
            return '<p class="muted small">Документов пока нет.</p>';
        }
        return documents.map((doc) => {
            const extension = (doc.fileName.split('.').pop() || 'file').toUpperCase();
            const size = doc.size >= 1024 * 1024
                ? `${(doc.size / (1024 * 1024)).toFixed(1)} МБ`
                : `${Math.max(1, Math.round(doc.size / 1024))} КБ`;
            return `<div class="doc-row">
                <div class="doc-row__icon">${esc(extension)}</div>
                <div class="doc-row__main">
                    <div class="doc-row__title">${esc(doc.title)}</div>
                    <div class="doc-row__meta">${esc(doc.fileName)} · ${size} · ${fmtDate(doc.createdAt)}</div>
                </div>
                <a class="btn btn--outline btn--xs" href="${esc(doc.url)}" target="_blank" rel="noopener">Открыть</a>
                <button class="btn btn--danger btn--xs" type="button" data-doc-delete="${doc.id}">Удалить</button>
            </div>`;
        }).join('');
    };

    // ---------------------------------------------------------- тарифы

    const billingTab = () => {
        const b = billing;
        const company = data.company;
        if (!b) {
            return `<div class="cabinet-card">
                ${emptyState('Тарифы недоступны', 'Не удалось загрузить данные тарифа. Обновите страницу.')}
            </div>`;
        }

        const cards = b.plans.map((plan) => `
            <div class="plan-card ${plan.current ? 'is-current' : ''}">
                <div class="row row--between">
                    <span class="plan-card__name">${esc(plan.title)}</span>
                    ${plan.current ? '<span class="badge badge--neutral">текущий</span>' : ''}
                </div>
                <div class="plan-card__price">${plan.price ? money(plan.price) : 'бесплатно'}
                    ${plan.price ? '<small>/ мес</small>' : ''}</div>
                <p class="plan-card__tagline">${esc(plan.tagline)}</p>
                <ul>${plan.features.map((feature) => `<li>${esc(feature)}</li>`).join('')}</ul>
                <button class="btn ${plan.current || plan.price === 0 ? 'btn--outline' : 'btn--primary'} btn--sm mt-16"
                        type="button" data-plan="${esc(plan.code)}" ${plan.current ? 'disabled' : ''}>
                    ${plan.current ? 'Текущий тариф'
                        : (plan.price ? 'Подключить' : 'Перейти на базовый')}
                </button>
            </div>`).join('');

        const history = b.history.length
            ? `<div class="table-wrap mt-24">
                   <table class="data-table">
                       <thead><tr><th>Тариф</th><th>Сумма</th><th>Начало</th><th>Действует до</th></tr></thead>
                       <tbody>${b.history.map((item) => `
                           <tr>
                               <td>${esc(item.planTitle)}</td>
                               <td>${money(item.amount)}</td>
                               <td>${fmtDate(item.startedAt)}</td>
                               <td>${item.expiresAt ? fmtDate(item.expiresAt) : '—'}</td>
                           </tr>`).join('')}
                       </tbody>
                   </table>
               </div>`
            : '<p class="muted small mt-24">Платежей пока не было.</p>';

        return `<div class="cabinet-card">
            <div class="cabinet-head">
                <div><h2>Тариф и продвижение</h2>
                    <p>Платный тариф поднимает компанию в каталоге и добавляет бейдж в карточку</p></div>
                ${planBadge(company)}
            </div>

            <div class="plan-current">
                <div>
                    <div class="small muted">Текущий тариф</div>
                    <div class="plan-current__value">${esc(b.planTitle)}</div>
                </div>
                <div>
                    <div class="small muted">Продвижение</div>
                    <div class="plan-current__value">${b.promoted
                        ? `до ${fmtDate(b.promotedUntil)}`
                        : 'не активно'}</div>
                </div>
                <div>
                    <div class="small muted">Оплачено всего</div>
                    <div class="plan-current__value">${money(b.totalSpent)}</div>
                </div>
            </div>

            <div class="plan-grid">${cards}</div>

            <div class="form-note mt-24">
                Демо-режим: платёжный шлюз не подключён — тариф активируется сразу на 30 дней
                и пишется в историю. По истечении срока продвижение снимается автоматически.
            </div>

            <h3 class="mt-24">История платежей</h3>
            ${history}
        </div>`;
    };

    const bindBilling = () => {
        root.querySelectorAll('[data-plan]').forEach((button) => {
            button.addEventListener('click', async () => {
                const plan = button.dataset.plan;
                if (plan === 'FREE' && !window.confirm('Перейти на базовый тариф? Продвижение будет отключено.')) {
                    return;
                }
                button.disabled = true;
                try {
                    billing = await api.upgrade(plan);
                    toast('Тариф обновлён');
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                    button.disabled = false;
                }
            });
        });
    };

    // ------------------------------------------------------ предложения

    const offersTab = () => {
        const company = data.company;
        const rows = company.offers.length
            ? company.offers.map((offer) => `
                <div class="list-row">
                    <div class="list-row__main">
                        <div class="list-row__title">${esc(offer.title)}</div>
                        <div class="list-row__meta">
                            <span class="tag">${esc(offer.typeTitle)}</span>
                            <span>${offer.priceFrom != null ? money(offer.priceFrom) : 'по запросу'}
                                ${esc(offer.unit || '')}</span>
                            <span>${offer.active ? 'активно' : 'скрыто'}</span>
                        </div>
                    </div>
                    <div class="list-row__side">
                        <button class="btn btn--outline btn--xs" data-edit-offer="${offer.id}">Изменить</button>
                        <button class="btn btn--outline btn--xs" data-toggle-offer="${offer.id}"
                                data-active="${offer.active}">${offer.active ? 'Скрыть' : 'Показать'}</button>
                        <button class="btn btn--danger btn--xs" data-delete-offer="${offer.id}">Удалить</button>
                    </div>
                </div>`).join('')
            : emptyState('Предложений пока нет',
                'Добавьте товары и услуги — они появятся в карточке компании и помогут найти вас в каталоге.');

        return `<div class="cabinet-card">
            <div class="cabinet-head">
                <div><h2>Предложения</h2><p>Товары и услуги, которые видны в вашей карточке</p></div>
                <button class="btn btn--primary" type="button" id="add-offer">+ Добавить предложение</button>
            </div>
            ${rows}
        </div>`;
    };

    const bindOffers = () => {
        const company = data.company;
        root.querySelector('#add-offer')?.addEventListener('click', () => openOfferForm(null));
        root.querySelectorAll('[data-edit-offer]').forEach((button) => {
            button.addEventListener('click', () => {
                const offer = company.offers.find((item) => item.id === Number(button.dataset.editOffer));
                openOfferForm(offer);
            });
        });
        root.querySelectorAll('[data-toggle-offer]').forEach((button) => {
            button.addEventListener('click', async () => {
                const active = button.dataset.active === 'true';
                try {
                    await api.toggleOffer(Number(button.dataset.toggleOffer), !active);
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                }
            });
        });
        root.querySelectorAll('[data-delete-offer]').forEach((button) => {
            button.addEventListener('click', async () => {
                if (!window.confirm('Удалить предложение?')) return;
                try {
                    await api.deleteOffer(Number(button.dataset.deleteOffer));
                    toast('Предложение удалено');
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                }
            });
        });
    };

    const openOfferForm = (offer) => {
        openModal({
            title: offer ? 'Изменить предложение' : 'Новое предложение',
            submitLabel: offer ? 'Сохранить' : 'Добавить',
            body: `<div class="form-grid">
                <div class="field field--full">
                    <label class="field__label" for="offer-title">Название <span class="req">*</span></label>
                    <input class="input" id="offer-title" name="title" maxlength="160" required
                           value="${esc(offer?.title || '')}" placeholder="Лист горячекатаный 3–12 мм">
                </div>
                <div class="field">
                    <label class="field__label" for="offer-type">Тип</label>
                    <select class="select" id="offer-type" name="type">
                        <option value="PRODUCT" ${offer?.type === 'PRODUCT' ? 'selected' : ''}>Товар</option>
                        <option value="SERVICE" ${offer?.type === 'SERVICE' ? 'selected' : ''}>Услуга</option>
                    </select>
                </div>
                <div class="field">
                    <label class="field__label" for="offer-price">Цена от, ₽</label>
                    <input class="input" id="offer-price" name="priceFrom" type="number" min="0" step="0.01"
                           value="${offer?.priceFrom ?? ''}">
                </div>
                <div class="field">
                    <label class="field__label" for="offer-unit">Единица</label>
                    <input class="input" id="offer-unit" name="unit" maxlength="30"
                           value="${esc(offer?.unit || '')}" placeholder="₽/т, ₽/час, ₽/м²">
                </div>
                <div class="field field--full">
                    <label class="field__label" for="offer-desc">Описание</label>
                    <textarea class="textarea" id="offer-desc" name="description" maxlength="2000">${esc(offer?.description || '')}</textarea>
                </div>
            </div>`,
            onSubmit: async (formData) => {
                const payload = Object.fromEntries(formData.entries());
                const body = {
                    title: payload.title,
                    type: payload.type,
                    priceFrom: payload.priceFrom ? Number(payload.priceFrom) : null,
                    unit: payload.unit || null,
                    description: payload.description || null,
                    active: offer ? offer.active : true
                };
                if (offer) await api.updateOffer(offer.id, body);
                else await api.createOffer(body);
                toast(offer ? 'Предложение обновлено' : 'Предложение добавлено');
                await reload();
            }
        });
    };

    // ------------------------------------------------------------- заявки

    const needsTab = () => {
        const rows = data.needs.length
            ? data.needs.map((need) => `
                <div class="list-row">
                    <div class="list-row__main">
                        <div class="list-row__title"><a href="#/need/${need.id}">${esc(need.title)}</a></div>
                        <div class="list-row__meta">
                            ${needStatusBadge(need.status, need.statusTitle)}
                            <span>${need.proposalsCount} ${plural(need.proposalsCount, ['отклик', 'отклика', 'откликов'])}</span>
                            <span>${relative(need.createdAt)}</span>
                            ${need.budget ? `<span>${money(need.budget)}</span>` : ''}
                        </div>
                    </div>
                    <div class="list-row__side">
                        <a class="btn btn--outline btn--xs" href="#/need/${need.id}">Открыть</a>
                    </div>
                </div>`).join('')
            : emptyState('Заявок пока нет',
                'Опубликуйте потребность — компании увидят её в ленте и пришлют предложения.');

        return `<div class="cabinet-card">
            <div class="cabinet-head">
                <div><h2>Мои заявки</h2><p>Все потребности, которые вы опубликовали</p></div>
                <button class="btn btn--primary" type="button" id="add-need">+ Разместить заявку</button>
            </div>
            ${rows}
        </div>`;
    };

    const bindNeeds = () => {
        root.querySelector('#add-need')?.addEventListener('click', () => openNeedForm(industries, reload));
    };

    // --------------------------------------------------------- входящие

    const incomingTab = () => {
        const rows = data.incoming.length
            ? data.incoming.map((proposal) => `
                <div class="list-row">
                    <div class="list-row__main">
                        <div class="list-row__title">
                            <a href="#/company/${esc(proposal.companySlug)}">${esc(proposal.companyName)}</a>
                            <span class="muted small"> → ${esc(proposal.needTitle)}</span>
                        </div>
                        <div class="list-row__meta">
                            ${proposalStatusBadge(proposal.status, proposal.statusTitle)}
                            <span>${proposal.price != null ? money(proposal.price) : 'цена по запросу'}</span>
                            <span>${relative(proposal.createdAt)}</span>
                        </div>
                    </div>
                    <div class="list-row__side">
                        <a class="btn btn--outline btn--xs" href="#/need/${proposal.needId}">К заявке</a>
                    </div>
                </div>`).join('')
            : emptyState('Входящих откликов нет',
                'Отклики появятся здесь, когда компании ответят на ваши заявки.');

        return `<div class="cabinet-card">
            <div class="cabinet-head"><div><h2>Входящие отклики</h2>
                <p>Предложения компаний на ваши заявки</p></div></div>
            ${rows}
        </div>`;
    };

    const bindIncoming = () => {};

    // ------------------------------------------------------- мои отклики

    const proposalsTab = () => {
        const rows = data.proposals.length
            ? data.proposals.map((proposal) => `
                <div class="list-row">
                    <div class="list-row__main">
                        <div class="list-row__title">
                            <a href="#/need/${proposal.needId}">${esc(proposal.needTitle)}</a>
                        </div>
                        <div class="list-row__meta">
                            ${proposalStatusBadge(proposal.status, proposal.statusTitle)}
                            <span>${proposal.price != null ? money(proposal.price) : 'цена не указана'}</span>
                            <span>${relative(proposal.createdAt)}</span>
                        </div>
                    </div>
                    <div class="list-row__side">
                        <a class="btn btn--outline btn--xs" href="#/need/${proposal.needId}">Открыть заявку</a>
                    </div>
                </div>`).join('')
            : emptyState('Вы ещё не откликались',
                'Найдите подходящую заявку в ленте и отправьте коммерческое предложение.',
                '<a class="btn btn--dark" href="#/needs">Открыть ленту заявок</a>');

        return `<div class="cabinet-card">
            <div class="cabinet-head"><div><h2>Мои отклики</h2>
                <p>Предложения, которые вы отправили другим компаниям</p></div>
                <a class="btn btn--outline btn--sm" href="#/needs">Найти заявки</a></div>
            ${rows}
        </div>`;
    };

    // ---------------------------------------------------------- избранное

    const favoritesTab = () => {
        const cards = data.favorites.length
            ? `<div class="company-grid">${data.favorites.map((company) => companyCard(company, { favorite: true })).join('')}</div>`
            : emptyState('В избранном пусто',
                'Сохраняйте компании, чтобы вернуться к ним позже.',
                '<a class="btn btn--dark" href="#/catalog">Открыть каталог</a>');

        return `<div class="cabinet-card">
            <div class="cabinet-head"><div><h2>Избранное</h2>
                <p>Сохранённые компании: ${data.favorites.length}</p></div></div>
            ${cards}
        </div>`;
    };

    const bindFavorites = () => {};

    render();
}
