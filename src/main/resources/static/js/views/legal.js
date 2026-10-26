import { api } from '../api.js';
import {
    esc, money, fmtDate, relative, plural, monogram, ratingInline, verifiedBadge, planBadge,
    emptyState, toast, openModal, skeletonCards
} from '../ui.js';
import { store } from '../store.js';
import { navigate } from '../router.js';

const TARGETS = [
    ['', 'Все статусы'],
    ['SELF_EMPLOYED', 'Самозанятый'],
    ['IP', 'ИП'],
    ['LLC', 'ООО'],
    ['JSC', 'АО']
];

const SORTS = [
    ['default', 'Рекомендуемые'],
    ['near', 'Ближе к вам'],
    ['rating', 'По рейтингу'],
    ['price', 'По цене']
];

// ------------------------------------------------------------ каталог услуг

export async function legalCatalog({ root, query }) {
    const user = store.user;
    const state = {
        target: query.target || '',
        city: query.city !== undefined ? query.city : (user && user.city ? user.city : ''),
        sort: query.sort || 'default',
        q: query.q || ''
    };

    root.innerHTML = `<div class="container"><div class="page-head"><h1>Юридические услуги</h1></div>
        ${skeletonCards(4)}</div>`;

    const [services, industries, cities] = await Promise.all([
        api.legalServices(state),
        api.industries(),
        api.cities().catch(() => [])
    ]);

    const update = (patch) => {
        const next = { ...state, ...patch };
        const params = new URLSearchParams();
        Object.entries(next).forEach(([key, value]) => {
            if (value !== '' && value !== undefined) params.set(key, value);
        });
        navigate(`/legal?${params.toString()}`);
    };

    const statusChips = TARGETS.map(([value, label]) => `
        <button type="button" class="filter-option ${state.target === value ? 'is-active' : ''}"
                data-target="${value}"><span>${label}</span></button>`).join('');

    const cityOptions = ['', ...cities].map((city) => `
        <option value="${esc(city)}" ${state.city === city ? 'selected' : ''}>
            ${city === '' ? 'Любой город' : esc(city)}</option>`).join('');

    const cards = services.length
        ? `<div class="legal-grid">${services.map((service) => legalCard(service)).join('')}</div>`
        : emptyState('Услуги не найдены',
            'Измените фильтры или город — или посмотрите все юридические фирмы в каталоге компаний.',
            '<a class="btn btn--dark" href="#/catalog?industry=legal">Юридические фирмы</a>');

    root.innerHTML = `<div class="container">
        <div class="page-head">
            <div class="eyebrow">Путь к юридическому статусу</div>
            <h1>Оформите статус рядом с вами</h1>
            <p>От самозанятого до акционерного общества: выберите фирму по расстоянию, рейтингу и отзывам.
                Заявка, документы, очная явка и оплата — на площадке, деньги уходят исполнителю
                только после подтверждения кодом.</p>
        </div>

        <button class="btn btn--outline filter-toggle" type="button" id="filter-toggle">
            Фильтры и поиск${[state.q, state.target, state.city].filter(Boolean).length
                ? ` (${[state.q, state.target, state.city].filter(Boolean).length})` : ''}
        </button>

        <div class="legal-layout">
            <aside class="filters">
                <div class="filters__head"><h3>Параметры</h3>
                    <button class="link-btn small" type="button" id="reset-legal">Сбросить</button></div>

                <form class="filter-group" id="legal-search">
                    <label class="filter-label" for="legal-q">Поиск</label>
                    <input class="input" id="legal-q" name="q" type="search" value="${esc(state.q)}"
                           placeholder="Фирма или услуга">
                </form>

                <div class="filter-group">
                    <span class="filter-label">Что оформляем</span>
                    <div class="filter-list" id="target-list">${statusChips}</div>
                </div>

                <div class="filter-group">
                    <label class="filter-label" for="city-select">Мой город</label>
                    <select class="select" id="city-select">${cityOptions}</select>
                </div>
            </aside>

            <div>
                <div class="results-head">
                    <div class="results-count">
                        Найдено <strong>${services.length}</strong>
                        ${plural(services.length, ['услуга', 'услуги', 'услуг'])}
                    </div>
                    <select class="select" id="sort-select" style="width:auto" aria-label="Сортировка">
                        ${SORTS.map(([value, label]) =>
                            `<option value="${value}" ${state.sort === value ? 'selected' : ''}>${label}</option>`).join('')}
                    </select>
                </div>
                ${cards}
            </div>
        </div>
        <div style="height:48px"></div>
    </div>`;

    root.querySelector('#legal-search').addEventListener('submit', (event) => {
        event.preventDefault();
        update({ q: new FormData(event.currentTarget).get('q')?.toString().trim() || '' });
    });
    root.querySelector('#target-list').addEventListener('click', (event) => {
        const button = event.target.closest('[data-target]');
        if (button) update({ target: button.dataset.target });
    });
    root.querySelector('#city-select').addEventListener('change', (event) => update({ city: event.target.value }));
    root.querySelector('#sort-select').addEventListener('change', (event) => update({ sort: event.target.value }));
    root.querySelector('#reset-legal').addEventListener('click', () => navigate('/legal'));

    const filterToggle = root.querySelector('#filter-toggle');
    filterToggle?.addEventListener('click', () => {
        const filters = root.querySelector('.filters');
        const open = filters.classList.toggle('is-open');
        filterToggle.textContent = open ? 'Скрыть фильтры' : 'Фильтры и поиск';
    });

    root.querySelectorAll('[data-apply]').forEach((button) => {
        button.addEventListener('click', () => {
            const service = services.find((item) => item.id === Number(button.dataset.apply));
            if (service) openApplyModal(service, industries);
        });
    });
}

function legalCard(service) {
    const distance = service.distanceKm === null || service.distanceKm === undefined
        ? ''
        : service.distanceKm === 0
            ? '<span class="badge badge--neutral">в вашем городе</span>'
            : `<span class="badge badge--neutral">≈ ${service.distanceKm} ${plural(service.distanceKm, ['км', 'км', 'км'])}</span>`;

    return `<article class="legal-card">
        <div class="legal-card__head">
            ${monogram({ name: service.providerName, accentColor: service.providerAccentColor, logoUrl: service.providerLogoUrl })}
            <div style="min-width:0;flex:1">
                <a class="legal-card__provider" href="#/company/${esc(service.providerSlug)}">
                    ${esc(service.providerName)}</a>
                <div class="company-card__meta">
                    ${service.providerCity ? esc(service.providerCity) : 'город не указан'}
                    · ${ratingInline({ rating: service.providerRating, reviewsCount: service.providerReviewsCount })}
                </div>
            </div>
            <div class="row" style="gap:6px">
                ${planBadge({ promoted: service.promoted, planCode: service.providerPlanCode })}
                ${verifiedBadge(service.providerVerified)}
            </div>
        </div>

        <h3 class="legal-card__title">${esc(service.title)}</h3>
        <p class="legal-card__desc">${esc(service.description || '')}</p>

        <div class="legal-card__foot">
            <div>
                <div class="legal-card__price">${money(service.price)}</div>
                <div class="muted small">срок ~${service.durationDays}
                    ${plural(service.durationDays, ['рабочий день', 'рабочих дня', 'рабочих дней'])}</div>
            </div>
            <div class="row" style="gap:8px">
                ${distance}
                <button class="btn btn--primary btn--sm" type="button" data-apply="${service.id}">
                    Подать заявку
                </button>
            </div>
        </div>
    </article>`;
}

function openApplyModal(service, industries) {
    if (!store.user) {
        toast('Войдите, чтобы подать заявку', 'error');
        navigate('/login?next=/legal');
        return;
    }
    if (store.user.companyId) {
        toast('У аккаунта уже есть карточка компании — оформление статуса не требуется', 'error');
        return;
    }

    openModal({
        title: 'Заявка на оформление',
        submitLabel: 'Отправить заявку',
        wide: true,
        body: `<div class="form-note">
                ${esc(service.providerName)} · ${esc(service.title)} · ${money(service.price)} ·
                срок ~${service.durationDays} дней. Юрфирма свяжется с вами и уточнит пакет документов.
            </div>
            <div class="form-grid">
                <div class="field">
                    <label class="field__label" for="app-company">Название будущей компании <span class="req">*</span></label>
                    <input class="input" id="app-company" name="companyName" maxlength="160" required
                           placeholder="ООО «УралСклад»">
                </div>
                <div class="field">
                    <label class="field__label" for="app-city">Город <span class="req">*</span></label>
                    <input class="input" id="app-city" name="city" maxlength="80" required
                           value="${esc(store.user.city || '')}" placeholder="Екатеринбург">
                </div>
                <div class="field">
                    <label class="field__label" for="app-inn">ИНН, если уже есть</label>
                    <input class="input" id="app-inn" name="inn" maxlength="12" placeholder="необязательно">
                </div>
                <div class="field">
                    <label class="field__label" for="app-industry">Отрасль будущего бизнеса <span class="req">*</span></label>
                    <select class="select" id="app-industry" name="industryCode" required>
                        ${industries.map((industry) =>
                            `<option value="${esc(industry.code)}" ${industry.code === 'warehouse' ? 'selected' : ''}>
                                ${industry.icon || ''} ${esc(industry.title)}</option>`).join('')}
                    </select>
                </div>
                <div class="field field--full">
                    <label class="field__label" for="app-comment">Комментарий для юрфирмы</label>
                    <textarea class="textarea" id="app-comment" name="comment" maxlength="2000"
                              placeholder="Что есть сейчас: земля, помещение, планы по аренде"></textarea>
                </div>
            </div>`,
        onSubmit: async (formData) => {
            const payload = Object.fromEntries(formData.entries());
            await api.createLegalApplication({
                serviceId: service.id,
                companyName: payload.companyName,
                inn: payload.inn || null,
                city: payload.city,
                industryCode: payload.industryCode,
                comment: payload.comment || null
            });
            toast('Заявка отправлена юридической фирме');
            navigate('/legal/applications');
        }
    });
}

// ---------------------------------------------------------- мои заявки

export async function legalApplications({ root, query }) {
    if (!store.user) {
        navigate('/login?next=/legal/applications');
        return;
    }

    let tab = query.tab === 'incoming' ? 'incoming' : 'mine';
    let mine = [];
    let incoming = [];

    const load = async () => {
        const [m, i] = await Promise.all([
            api.myLegalApplications().catch(() => []),
            api.incomingLegalApplications().catch(() => [])
        ]);
        mine = m;
        incoming = i;
    };

    root.innerHTML = `<div class="container"><div class="page-head"><h1>Заявки</h1></div>
        ${skeletonCards(3)}</div>`;
    await load();

    const render = () => {
        const items = tab === 'incoming' ? incoming : mine;
        const list = items.length
            ? `<div class="stack">${items.map((application) => applicationCard(application)).join('')}</div>`
            : (tab === 'incoming'
                ? emptyState('Входящих заявок нет',
                    'Здесь появятся заявки частных лиц, которые выбрали вашу компанию для оформления статуса.')
                : emptyState('Заявок пока нет',
                    'Выберите юридическую фирму и подайте заявку на оформление статуса.',
                    '<a class="btn btn--dark" href="#/legal">Выбрать юрфирму</a>'));

        root.innerHTML = `<div class="container">
            <div class="page-head">
                <div class="eyebrow">Оформление статуса</div>
                <h1>Заявки</h1>
                <p>Заявка, документы, очная явка и защищённый платёж. Деньги уходят исполнителю
                    только после того, как вы передадите одноразовый код.</p>
            </div>

            <div class="tabs" role="tablist">
                <button class="tab ${tab === 'mine' ? 'is-active' : ''}" data-tab="mine">
                    Мои заявки (${mine.length})
                </button>
                <button class="tab ${tab === 'incoming' ? 'is-active' : ''}" data-tab="incoming">
                    Входящие (${incoming.length})
                </button>
                <a class="btn btn--outline btn--sm" href="#/legal" style="margin-left:auto;align-self:center">
                    + Новая заявка
                </a>
            </div>

            ${list}
            <div style="height:48px"></div>
        </div>`;

        root.querySelectorAll('[data-tab]').forEach((button) => {
            button.addEventListener('click', () => {
                tab = button.dataset.tab;
                window.history.replaceState(null, '', `#/legal/applications?tab=${tab}`);
                render();
            });
        });
        bindActions();
    };

    const reload = async () => {
        await load();
        render();
    };

    const bindActions = () => {
        root.querySelectorAll('[data-pay]').forEach((button) => {
            button.addEventListener('click', async () => {
                button.disabled = true;
                try {
                    await api.payLegal(Number(button.dataset.pay));
                    toast('Платёж внесён и удерживается до подтверждения услуги');
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                    button.disabled = false;
                }
            });
        });

        root.querySelectorAll('[data-cancel]').forEach((button) => {
            button.addEventListener('click', async () => {
                if (!window.confirm('Отменить заявку? Если оплата внесена, деньги вернутся.')) return;
                try {
                    await api.cancelLegal(Number(button.dataset.cancel));
                    toast('Заявка отменена');
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                }
            });
        });

        root.querySelectorAll('[data-request-docs]').forEach((button) => {
            button.addEventListener('click', () => {
                openModal({
                    title: 'Запросить документы',
                    submitLabel: 'Запросить',
                    body: `<div class="field">
                        <label class="field__label" for="doc-comment">Что нужно от заказчика</label>
                        <textarea class="textarea" id="doc-comment" name="comment" maxlength="1000"
                            placeholder="Паспорт, выписка, согласие собственника…"></textarea>
                    </div>`,
                    onSubmit: async (formData) => {
                        const comment = Object.fromEntries(formData.entries()).comment;
                        await api.requestLegalDocuments(Number(button.dataset.requestDocs), comment);
                        toast('Запрос отправлен заказчику');
                        await reload();
                    }
                });
            });
        });

        root.querySelectorAll('[data-schedule]').forEach((button) => {
            button.addEventListener('click', () => {
                openModal({
                    title: 'Назначить очную явку',
                    submitLabel: 'Назначить',
                    body: `<div class="field">
                        <label class="field__label" for="appointment">Дата и время</label>
                        <input class="input" id="appointment" name="appointmentAt" type="datetime-local" required>
                    </div>
                    <p class="muted small">Заказчик увидит дату в заявке и сможет оплатить услугу.</p>`,
                    onSubmit: async (formData) => {
                        const value = Object.fromEntries(formData.entries()).appointmentAt;
                        if (!value) throw new Error('Укажите дату и время');
                        await api.scheduleLegal(Number(button.dataset.schedule), value);
                        toast('Явка согласована');
                        await reload();
                    }
                });
            });
        });

        root.querySelectorAll('[data-complete]').forEach((button) => {
            button.addEventListener('click', async () => {
                button.disabled = true;
                try {
                    await api.completeLegal(Number(button.dataset.complete));
                    toast('Услуга отмечена как оказанная, код отправлен заказчику');
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                    button.disabled = false;
                }
            });
        });

        root.querySelectorAll('[data-confirm]').forEach((button) => {
            button.addEventListener('click', () => {
                openModal({
                    title: 'Подтвердить кодом заказчика',
                    submitLabel: 'Подтвердить',
                    body: `<div class="field">
                        <label class="field__label" for="otp-code">Код из 6 цифр</label>
                        <input class="input" id="otp-code" name="code" inputmode="numeric" maxlength="6"
                               pattern="\\d{6}" required placeholder="000000" style="letter-spacing:.4em;font-family:var(--font-mono)">
                    </div>
                    <p class="muted small">Код передаёт заказчик после оказания услуги. После подтверждения
                        деньги из эскроу уходят вашей компании.</p>`,
                    onSubmit: async (formData) => {
                        const code = Object.fromEntries(formData.entries()).code;
                        await api.confirmLegal(Number(button.dataset.confirm), code);
                        toast('Услуга подтверждена, платёж выплачен');
                        await reload();
                    }
                });
            });
        });

        root.querySelectorAll('[data-doc-upload]').forEach((input) => {
            input.addEventListener('change', async () => {
                const file = input.files && input.files[0];
                if (!file) return;
                const formData = new FormData();
                formData.append('file', file);
                try {
                    await api.uploadLegalDocument(Number(input.dataset.docUpload), formData);
                    toast('Документ приложен');
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                }
            });
        });

        root.querySelectorAll('[data-doc-delete]').forEach((button) => {
            button.addEventListener('click', async () => {
                if (!window.confirm('Удалить документ?')) return;
                try {
                    await api.deleteLegalDocument(Number(button.dataset.app), Number(button.dataset.docDelete));
                    toast('Документ удалён');
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                }
            });
        });
    };

    render();
}

function applicationCard(app) {
    const statusClass = {
        NEW: 'badge--new',
        DOCUMENTS_REQUESTED: 'badge--progress',
        SCHEDULED: 'badge--open',
        PAID: 'badge--accepted',
        COMPLETED: 'badge--verified',
        CANCELLED: 'badge--closed'
    }[app.status] || 'badge--neutral';

    const escrow = app.payment;
    const escrowBox = escrow
        ? `<div class="escrow">
               <div class="small muted">Защищённый платёж</div>
               <div class="escrow__amount">${money(escrow.amount)}</div>
               <span class="badge ${escrow.status === 'RELEASED' ? 'badge--verified'
                    : escrow.status === 'REFUNDED' ? 'badge--closed' : 'badge--progress'}">
                   ${esc(escrow.statusTitle)}</span>
               ${escrow.heldAt ? `<div class="small muted" style="margin-top:8px">
                   Удержан ${fmtDate(escrow.heldAt)}</div>` : ''}
           </div>`
        : `<div class="escrow">
               <div class="small muted">Защищённый платёж</div>
               <div class="escrow__amount">${money(app.amount)}</div>
               <div class="small muted">Оплата ещё не внесена</div>
           </div>`;

    const otpBlock = app.role === 'AUTHOR' && app.payment && app.payment.otp
        ? `<div class="otp">
               <div class="small muted">Код для исполнителя</div>
               <div class="otp__code">${esc(app.payment.otp)}</div>
               <div class="small muted">Передайте код юрфирме после оказания услуги.
                   ${app.payment.otpExpiresAt ? `Действует до ${fmtDate(app.payment.otpExpiresAt)}` : ''}</div>
           </div>`
        : '';

    const documents = app.documents.length
        ? app.documents.map((doc) => `<div class="doc-row">
               <div class="doc-row__icon">${esc((doc.fileName.split('.').pop() || 'file').toUpperCase())}</div>
               <div class="doc-row__main">
                   <div class="doc-row__title">${esc(doc.title)}</div>
                   <div class="doc-row__meta">${esc(doc.fileName)} · ${Math.max(1, Math.round(doc.size / 1024))} КБ</div>
               </div>
               <a class="btn btn--outline btn--xs" href="${esc(doc.url)}" target="_blank" rel="noopener">Открыть</a>
               ${app.role === 'AUTHOR' && app.canUploadDocuments
                   ? `<button class="btn btn--danger btn--xs" type="button"
                              data-app="${app.id}" data-doc-delete="${doc.id}">Удалить</button>` : ''}
           </div>`).join('')
        : '<p class="muted small">Документов пока нет.</p>';

    const authorActions = [
        app.canPay ? `<button class="btn btn--primary btn--sm" type="button" data-pay="${app.id}">
                          Оплатить ${money(app.amount)}</button>` : '',
        app.canUploadDocuments ? `<label class="btn btn--outline btn--sm" style="cursor:pointer">
                Приложить документ
                <input type="file" hidden accept=".pdf,.png,.jpg,.jpeg"
                       data-doc-upload="${app.id}"></label>` : '',
        app.canCancel ? `<button class="btn btn--danger btn--sm" type="button" data-cancel="${app.id}">
                             Отменить заявку</button>` : ''
    ].filter(Boolean).join('');

    const providerActions = [
        app.canRequestDocuments ? `<button class="btn btn--outline btn--sm" type="button"
            data-request-docs="${app.id}">Запросить документы</button>` : '',
        app.canSchedule ? `<button class="btn btn--dark btn--sm" type="button"
            data-schedule="${app.id}">Назначить явку</button>` : '',
        app.canComplete ? `<button class="btn btn--primary btn--sm" type="button"
            data-complete="${app.id}">Услуга оказана</button>` : '',
        app.canConfirm ? `<button class="btn btn--primary btn--sm" type="button"
            data-confirm="${app.id}">Подтвердить кодом</button>` : ''
    ].filter(Boolean).join('');

    const peer = app.role === 'AUTHOR'
        ? `Исполнитель: <a class="link-btn" href="#/company/${esc(app.providerSlug)}">${esc(app.providerName)}</a>`
        : `Заказчик: <b>${esc(app.authorName)}</b>${app.authorCity ? ` · ${esc(app.authorCity)}` : ''}`;

    return `<article class="app-card">
        <div class="app-card__head">
            <div>
                <div class="row" style="gap:8px;margin-bottom:6px">
                    <span class="badge ${statusClass}">${esc(app.statusTitle)}</span>
                    <span class="badge badge--neutral">${esc(app.targetStatusTitle)}</span>
                    ${app.role === 'PROVIDER' ? '<span class="tag">входящая</span>' : ''}
                </div>
                <h3 class="app-card__title">${esc(app.companyName)}</h3>
                <div class="muted small">${peer} · заявка №${app.id} от ${fmtDate(app.createdAt)}</div>
            </div>
            <div class="app-card__amount">
                <div class="small muted">Стоимость</div>
                <div class="plan-current__value">${money(app.amount)}</div>
            </div>
        </div>

        <div class="app-card__body">
            <div>
                <dl class="kv">
                    <dt>Услуга</dt><dd>${esc(app.serviceTitle || app.targetStatusTitle)}</dd>
                    <dt>Город</dt><dd>${esc(app.city)}</dd>
                    <dt>Отрасль</dt><dd>${esc(app.industryTitle || app.industryCode)}</dd>
                    ${app.inn ? `<dt>ИНН</dt><dd>${esc(app.inn)}</dd>` : ''}
                    ${app.appointmentAt ? `<dt>Явка</dt><dd>${fmtDate(app.appointmentAt)}</dd>` : ''}
                </dl>
                ${app.comment ? `<p class="small muted mt-16">«${esc(app.comment)}»</p>` : ''}
                ${app.providerComment ? `<div class="form-note" style="margin:12px 0 0">
                    <b>Юрфирма:</b> ${esc(app.providerComment)}</div>` : ''}
                ${otpBlock}
                ${authorActions || providerActions
                    ? `<div class="form-actions">${authorActions || providerActions}</div>` : ''}
            </div>
            <div>
                ${escrowBox}
                <div style="margin-top:16px">
                    <div class="filter-label">Документы</div>
                    ${documents}
                </div>
            </div>
        </div>
    </article>`;
}
