import { api } from '../api.js';
import { esc, num, plural, companyCard, emptyState, skeletonCards } from '../ui.js';
import { href, navigate } from '../router.js';

const SORTS = [
    ['default', 'По релевантности'],
    ['rating', 'По рейтингу'],
    ['name', 'По названию'],
    ['newest', 'Сначала новые']
];

export default async function catalog({ root, query }) {
    const state = {
        q: query.q || '',
        industry: query.industry || '',
        city: query.city || '',
        sort: query.sort || 'default',
        page: Math.max(Number(query.page || 0), 0)
    };

    root.innerHTML = `<div class="container"><div class="page-head"><h1>Каталог компаний</h1></div>${skeletonCards(6)}</div>`;

    const [industries, cities, result] = await Promise.all([
        api.industries(),
        api.cities(),
        api.companies({
            q: state.q,
            industry: state.industry,
            city: state.city,
            sort: state.sort,
            page: state.page,
            size: 12
        })
    ]);

    const update = (patch) => {
        const next = { ...state, page: 0, ...patch };
        navigate(`/catalog?${new URLSearchParams(trim(next)).toString()}`);
    };

    const industryOptions = [
        `<button type="button" class="filter-option ${state.industry ? '' : 'is-active'}" data-industry="">
            <span>Все отрасли</span><span class="filter-option__count">${num(industries.reduce((sum, i) => sum + i.companiesCount, 0))}</span>
        </button>`,
        ...industries.map((industry) => `
            <button type="button" class="filter-option ${state.industry === industry.code ? 'is-active' : ''}"
                    data-industry="${esc(industry.code)}">
                <span>${industry.icon || '•'} ${esc(industry.title)}</span>
                <span class="filter-option__count">${industry.companiesCount}</span>
            </button>`)
    ].join('');

    const cityOptions = [
        `<button type="button" class="filter-option ${state.city ? '' : 'is-active'}" data-city="">
            <span>Любой город</span></button>`,
        ...cities.slice(0, 14).map((city) => `
            <button type="button" class="filter-option ${state.city === city ? 'is-active' : ''}" data-city="${esc(city)}">
                <span>${esc(city)}</span>
            </button>`)
    ].join('');

    const cards = result.items.length
        ? result.items.map((company) => companyCard(company)).join('')
        : emptyState('Ничего не нашлось',
            'Попробуйте изменить запрос, выбрать другую отрасль или сбросить фильтры.',
            '<button class="btn btn--dark" type="button" id="reset-empty">Сбросить фильтры</button>');

    const activeFilters = [state.q, state.industry, state.city].filter(Boolean).length;

    root.innerHTML = `<div class="container">
        <div class="page-head">
            <div class="eyebrow">Каталог участников</div>
            <h1>Компании площадки</h1>
            <p>Найдите поставщика, подрядчика или сервис по отрасли и городу. Отмеченные знаком «Проверена»
                компании прошли модерацию профиля.</p>
        </div>

        <button class="btn btn--outline filter-toggle" type="button" id="filter-toggle">
            Фильтры и поиск${activeFilters ? ` (${activeFilters})` : ''}
        </button>

        <div class="catalog-layout">
            <aside class="filters">
                <div class="filters__head">
                    <h3>Фильтры</h3>
                    <button class="link-btn small" type="button" id="reset-filters">Сбросить</button>
                </div>

                <form class="filter-group" id="filter-search">
                    <label class="filter-label" for="catalog-q">Поиск</label>
                    <input class="input" id="catalog-q" name="q" type="search" placeholder="Название, описание, город"
                           value="${esc(state.q)}">
                </form>

                <div class="filter-group">
                    <span class="filter-label">Отрасль</span>
                    <div class="filter-list" id="industry-list">${industryOptions}</div>
                </div>

                <div class="filter-group">
                    <span class="filter-label">Город</span>
                    <div class="filter-list" id="city-list">${cityOptions}</div>
                </div>
            </aside>

            <div>
                <div class="results-head">
                    <div class="results-count">
                        Найдено <strong>${num(result.total)}</strong> ${plural(result.total, ['компания', 'компании', 'компаний'])}
                        ${activeFilters ? '<span class="tag" style="margin-left:8px">с фильтрами</span>' : ''}
                    </div>
                    <div class="toolbar">
                        <select class="select" id="sort-select" aria-label="Сортировка" style="width:auto">
                            ${SORTS.map(([value, label]) =>
                                `<option value="${value}" ${state.sort === value ? 'selected' : ''}>${label}</option>`).join('')}
                        </select>
                    </div>
                </div>

                <div class="company-grid">${cards}</div>
                ${pagination(state.page, result.totalPages)}
            </div>
        </div>
    </div>`;

    root.querySelector('#filter-search').addEventListener('submit', (event) => {
        event.preventDefault();
        update({ q: new FormData(event.currentTarget).get('q')?.toString().trim() || '' });
    });

    root.querySelector('#industry-list').addEventListener('click', (event) => {
        const button = event.target.closest('[data-industry]');
        if (button) update({ industry: button.dataset.industry });
    });

    root.querySelector('#city-list').addEventListener('click', (event) => {
        const button = event.target.closest('[data-city]');
        if (button) update({ city: button.dataset.city });
    });

    root.querySelector('#sort-select').addEventListener('change', (event) => {
        update({ sort: event.target.value });
    });

    root.querySelector('#reset-filters')?.addEventListener('click', () => navigate('/catalog'));
    root.querySelector('#reset-empty')?.addEventListener('click', () => navigate('/catalog'));

    const filterToggle = root.querySelector('#filter-toggle');
    filterToggle?.addEventListener('click', () => {
        const filters = root.querySelector('.filters');
        const open = filters.classList.toggle('is-open');
        filterToggle.textContent = open
            ? 'Скрыть фильтры'
            : `Фильтры и поиск${activeFilters ? ` (${activeFilters})` : ''}`;
    });

    root.querySelectorAll('[data-page]').forEach((button) => {
        button.addEventListener('click', () => {
            navigate(`/catalog?${new URLSearchParams(trim({ ...state, page: Number(button.dataset.page) })).toString()}`);
        });
    });
}

function trim(state) {
    const result = {};
    Object.entries(state).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '' && !(key === 'page' && value === 0)) {
            result[key] = value;
        }
    });
    return result;
}

function pagination(page, totalPages) {
    if (totalPages <= 1) return '';
    const numbers = [];
    const from = Math.max(0, page - 2);
    const to = Math.min(totalPages - 1, page + 2);
    for (let index = from; index <= to; index += 1) numbers.push(index);

    return `<div class="row" style="justify-content:center;margin-top:28px;gap:6px">
        <button class="btn btn--outline btn--sm" data-page="${page - 1}" ${page === 0 ? 'disabled' : ''}>←</button>
        ${numbers.map((number) => `
            <button class="btn ${number === page ? 'btn--dark' : 'btn--outline'} btn--sm" data-page="${number}">
                ${number + 1}
            </button>`).join('')}
        <button class="btn btn--outline btn--sm" data-page="${page + 1}" ${page >= totalPages - 1 ? 'disabled' : ''}>→</button>
    </div>`;
}
