import { api } from '../api.js';
import { esc, num, relative, needStatusBadge, emptyState, toast } from '../ui.js';
import { store } from '../store.js';
import { navigate } from '../router.js';

export default async function admin({ root }) {
    if (!store.user) {
        navigate('/login?next=/admin');
        return;
    }
    if (!store.user.admin) {
        root.innerHTML = `<div class="container"><div class="page-head">
            <h1>Раздел недоступен</h1>
            <p>Админ-панель доступна только администратору площадки.</p>
            <a class="btn btn--dark" href="#/">На главную</a></div></div>`;
        return;
    }

    root.innerHTML = `<div class="container"><div class="page-head"><h1>Админ-панель</h1></div>
        <div class="skeleton" style="height:320px"></div></div>`;

    let stats = await api.adminStats();
    let pending = await api.adminPending();
    let needs = await api.adminNeeds();

    const reload = async () => {
        [stats, pending, needs] = await Promise.all([api.adminStats(), api.adminPending(), api.adminNeeds()]);
        render();
    };

    const render = () => {
        const cards = [
            ['Всего компаний', stats.companies],
            ['Проверено', stats.verifiedCompanies],
            ['Ждут проверки', stats.pendingCompanies],
            ['Открытых заявок', stats.openNeeds],
            ['Откликов', stats.proposals],
            ['Диалогов', stats.threads],
            ['Платных компаний', stats.paidCompanies],
            ['Пользователей', stats.users]
        ].map(([label, value]) => `
            <div class="stat-card">
                <div class="stat-card__value">${num(value)}</div>
                <div class="stat-card__label">${label}</div>
            </div>`).join('');

        const rows = pending.length
            ? pending.map((company) => `
                <tr>
                    <td><a class="link-btn" href="#/company/${esc(company.slug)}">${esc(company.name)}</a></td>
                    <td>${esc(company.industryTitle || '')}</td>
                    <td>${esc(company.city || '—')}</td>
                    <td>${company.sizeTitle ? esc(company.sizeTitle) : '—'}</td>
                    <td><button class="btn btn--primary btn--xs" data-verify="${company.id}">Верифицировать</button></td>
                </tr>`).join('')
            : `<tr><td colspan="5"><span class="muted">Новых компаний на проверке нет.</span></td></tr>`;

        const needRows = needs.length
            ? needs.map((need) => `
                <div class="list-row">
                    <div class="list-row__main">
                        <div class="list-row__title"><a href="#/need/${need.id}">${esc(need.title)}</a></div>
                        <div class="list-row__meta">
                            ${needStatusBadge(need.status, need.statusTitle)}
                            <span>${esc(need.industryTitle || '')}</span>
                            <span>${esc(need.authorCompanyName || need.authorName)}</span>
                            <span>${relative(need.createdAt)}</span>
                        </div>
                    </div>
                    <div class="list-row__side">
                        <span class="muted small">${need.proposalsCount} откл.</span>
                    </div>
                </div>`).join('')
            : emptyState('Заявок нет', 'Площадка пока пустая.');

        root.innerHTML = `<div class="container">
            <div class="page-head">
                <div class="row row--between" style="align-items:flex-end">
                    <div>
                        <div class="eyebrow">Модерация площадки</div>
                        <h1>Админ-панель</h1>
                        <p>Проверяйте новые компании, следите за активностью и статистикой площадки.</p>
                    </div>
                    <a class="btn btn--outline btn--sm" href="#/catalog">Открыть каталог</a>
                </div>
            </div>

            <div class="stat-grid">${cards}</div>

            <div class="cabinet-card">
                <div class="cabinet-head">
                    <div><h2>Компании на проверке</h2>
                        <p>Подтвердите реквизиты, чтобы выдать отметку «Проверена»</p></div>
                </div>
                <div class="table-wrap">
                    <table class="data-table">
                        <thead><tr>
                            <th>Компания</th><th>Отрасль</th><th>Город</th><th>Размер</th><th></th>
                        </tr></thead>
                        <tbody>${rows}</tbody>
                    </table>
                </div>
            </div>

            <div class="cabinet-card" style="margin-top:16px">
                <div class="cabinet-head">
                    <div><h2>Последние открытые заявки</h2><p>Что происходит на площадке прямо сейчас</p></div>
                    <a class="btn btn--outline btn--sm" href="#/needs">Все заявки</a>
                </div>
                ${needRows}
            </div>
            <div style="height:48px"></div>
        </div>`;

        root.querySelectorAll('[data-verify]').forEach((button) => {
            button.addEventListener('click', async () => {
                button.disabled = true;
                try {
                    await api.adminVerify(Number(button.dataset.verify), true);
                    toast('Компания верифицирована');
                    await reload();
                } catch (error) {
                    toast(error.message, 'error');
                    button.disabled = false;
                }
            });
        });
    };

    render();
}
