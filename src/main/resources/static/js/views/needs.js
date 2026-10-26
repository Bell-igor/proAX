import { api } from '../api.js';
import { esc, num, plural, needRow, emptyState, skeletonRows, openModal, toast } from '../ui.js';
import { store } from '../store.js';
import { navigate } from '../router.js';

const STATUSES = [
    ['', 'Все'],
    ['OPEN', 'Открытые'],
    ['IN_PROGRESS', 'В работе'],
    ['CLOSED', 'Закрытые']
];

export default async function needs({ root, query }) {
    const state = {
        q: query.q || '',
        industry: query.industry || '',
        status: query.status || '',
        page: Math.max(Number(query.page || 0), 0)
    };

    root.innerHTML = `<div class="container"><div class="page-head"><h1>Лента заявок</h1></div>${skeletonRows(4)}</div>`;

    const [industries, result] = await Promise.all([
        api.industries(),
        api.needs({ ...state, size: 12 })
    ]);

    const update = (patch) => {
        const next = { ...state, page: 0, ...patch };
        const params = new URLSearchParams();
        Object.entries(next).forEach(([key, value]) => {
            if (value !== '' && value !== undefined && !(key === 'page' && value === 0)) params.set(key, value);
        });
        navigate(`/needs?${params.toString()}`);
    };

    const statusButtons = STATUSES.map(([value, label]) => `
        <button type="button" class="filter-option ${state.status === value ? 'is-active' : ''}"
                data-status="${value}"><span>${label}</span></button>`).join('');

    const industryOptions = industries.map((industry) =>
        `<option value="${esc(industry.code)}" ${state.industry === industry.code ? 'selected' : ''}>
            ${industry.icon || ''} ${esc(industry.title)}
        </option>`).join('');

    const list = result.items.length
        ? result.items.map((need) => needRow(need)).join('')
        : emptyState('Заявок не найдено', 'Измените фильтры или опубликуйте свою потребность — отклики приходят быстро.',
            '<button class="btn btn--dark" id="empty-create" type="button">Разместить заявку</button>');

    root.innerHTML = `<div class="container">
        <div class="page-head">
            <div class="row row--between" style="align-items:flex-end">
                <div>
                    <div class="eyebrow">Потребности компаний</div>
                    <h1>Лента заявок</h1>
                    <p>Здесь компании публикуют, что им нужно: материалы, перевозка, склад, подрядчик, консультация.
                        Откликнитесь — и обсудите условия напрямую.</p>
                </div>
                <button class="btn btn--primary" type="button" id="create-need">+ Разместить заявку</button>
            </div>
        </div>

        <button class="btn btn--outline filter-toggle" type="button" id="filter-toggle">
            Фильтры и поиск${[state.q, state.status, state.industry].filter(Boolean).length
                ? ` (${[state.q, state.status, state.industry].filter(Boolean).length})` : ''}
        </button>

        <div class="catalog-layout">
            <aside class="filters">
                <div class="filters__head"><h3>Фильтры</h3>
                    <button class="link-btn small" type="button" id="reset-filters">Сбросить</button></div>

                <form class="filter-group" id="filter-search">
                    <label class="filter-label" for="need-q">Поиск</label>
                    <input class="input" id="need-q" name="q" type="search" value="${esc(state.q)}"
                           placeholder="Например, металлопрокат">
                </form>

                <div class="filter-group">
                    <span class="filter-label">Статус</span>
                    <div class="filter-list" id="status-list">${statusButtons}</div>
                </div>

                <div class="filter-group">
                    <label class="filter-label" for="industry-select">Отрасль</label>
                    <select class="select" id="industry-select">
                        <option value="">Все отрасли</option>
                        ${industryOptions}
                    </select>
                </div>
            </aside>

            <div>
                <div class="results-head">
                    <div class="results-count">
                        <strong>${num(result.total)}</strong> ${plural(result.total, ['заявка', 'заявки', 'заявок'])}
                    </div>
                </div>
                <div class="need-list">${list}</div>
                ${pagination(state.page, result.totalPages)}
            </div>
        </div>
    </div>`;

    root.querySelector('#create-need').addEventListener('click', () => openNeedForm(industries));
    root.querySelector('#empty-create')?.addEventListener('click', () => openNeedForm(industries));
    root.querySelector('#filter-search').addEventListener('submit', (event) => {
        event.preventDefault();
        update({ q: new FormData(event.currentTarget).get('q')?.toString().trim() || '' });
    });
    root.querySelector('#status-list').addEventListener('click', (event) => {
        const button = event.target.closest('[data-status]');
        if (button) update({ status: button.dataset.status });
    });
    root.querySelector('#industry-select').addEventListener('change', (event) => {
        update({ industry: event.target.value });
    });
    root.querySelector('#reset-filters').addEventListener('click', () => navigate('/needs'));

    const activeCount = [state.q, state.status, state.industry].filter(Boolean).length;
    const filterToggle = root.querySelector('#filter-toggle');
    filterToggle?.addEventListener('click', () => {
        const filters = root.querySelector('.filters');
        const open = filters.classList.toggle('is-open');
        filterToggle.textContent = open
            ? 'Скрыть фильтры'
            : `Фильтры и поиск${activeCount ? ` (${activeCount})` : ''}`;
    });
    root.querySelectorAll('[data-page]').forEach((button) => {
        button.addEventListener('click', () => {
            const params = new URLSearchParams();
            Object.entries({ ...state, page: Number(button.dataset.page) }).forEach(([key, value]) => {
                if (value !== '' && value !== undefined && !(key === 'page' && value === 0)) params.set(key, value);
            });
            navigate(`/needs?${params.toString()}`);
        });
    });
}

export function openNeedForm(industries, onDone) {
    if (!store.user) {
        toast('Войдите, чтобы разместить заявку', 'error');
        navigate('/login?next=/needs');
        return;
    }
    openModal({
        title: 'Новая заявка',
        submitLabel: 'Опубликовать',
        wide: true,
        body: `<div class="form-note">Опишите потребность конкретно: что нужно, в каком объёме, сроке и бюджете.
                   Так отклики будут точнее.</div>
            <div class="form-grid">
                <div class="field field--full">
                    <label class="field__label" for="need-title">Что нужно <span class="req">*</span></label>
                    <input class="input" id="need-title" name="title" maxlength="200" required
                           placeholder="Например: лист 09Г2С, 12 тонн">
                </div>
                <div class="field">
                    <label class="field__label" for="need-industry">Отрасль <span class="req">*</span></label>
                    <select class="select" id="need-industry" name="industryCode" required>
                        ${industries.map((industry) =>
                            `<option value="${esc(industry.code)}">${esc(industry.title)}</option>`).join('')}
                    </select>
                </div>
                <div class="field">
                    <label class="field__label" for="need-city">Город</label>
                    <input class="input" id="need-city" name="city" maxlength="80" placeholder="Екатеринбург">
                </div>
                <div class="field">
                    <label class="field__label" for="need-budget">Бюджет, ₽</label>
                    <input class="input" id="need-budget" name="budget" type="number" min="0" step="1000"
                           placeholder="1200000">
                </div>
                <div class="field">
                    <label class="field__label" for="need-deadline">Срок</label>
                    <input class="input" id="need-deadline" name="deadline" type="date">
                </div>
                <div class="field field--full">
                    <label class="field__label" for="need-description">Подробности</label>
                    <textarea class="textarea" id="need-description" name="description" maxlength="4000"
                              placeholder="Объём, требования к качеству, условия поставки, документы"></textarea>
                </div>
            </div>`,
        onSubmit: async (formData) => {
            const payload = Object.fromEntries(formData.entries());
            await api.createNeed({
                title: payload.title,
                industryCode: payload.industryCode,
                city: payload.city || null,
                budget: payload.budget ? Number(payload.budget) : null,
                deadline: payload.deadline || null,
                description: payload.description || null
            });
            toast('Заявка опубликована');
            if (onDone) onDone();
            else window.location.reload();
        }
    });
}

function pagination(page, totalPages) {
    if (totalPages <= 1) return '';
    return `<div class="row" style="justify-content:center;margin-top:28px;gap:6px">
        <button class="btn btn--outline btn--sm" data-page="${page - 1}" ${page === 0 ? 'disabled' : ''}>←</button>
        <span class="muted small">стр. ${page + 1} из ${totalPages}</span>
        <button class="btn btn--outline btn--sm" data-page="${page + 1}" ${page >= totalPages - 1 ? 'disabled' : ''}>→</button>
    </div>`;
}
