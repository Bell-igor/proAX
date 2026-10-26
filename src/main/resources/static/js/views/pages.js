import { api } from '../api.js';
import { esc, num, plural } from '../ui.js';
import { href } from '../router.js';

export async function industriesPage({ root }) {
    root.innerHTML = `<div class="container"><div class="page-head"><h1>Отрасли</h1></div>
        <div class="skeleton" style="height:300px"></div></div>`;

    const industries = await api.industries();
    const cards = industries.map((industry) => `
        <a class="industry-card" href="${href('/catalog', { industry: industry.code })}">
            <span class="industry-card__icon">${industry.icon || '•'}</span>
            <span>
                <span class="industry-card__title">${esc(industry.title)}</span>
                <span class="industry-card__desc">${esc(industry.description || '')}</span>
                <span class="industry-card__count">${industry.companiesCount}
                    ${plural(industry.companiesCount, ['компания', 'компании', 'компаний'])}</span>
            </span>
        </a>`).join('');

    root.innerHTML = `<div class="container">
        <div class="page-head">
            <div class="eyebrow">Карта цепочки</div>
            <h1>${industries.length} отраслей площадки</h1>
            <p>ProAX собирает участников всей производственной и коммерческой цепочки: от добычи и поставки
                ресурсов до юридического сопровождения сделки и вывода продукта на рынок.</p>
        </div>
        <div class="industry-grid">${cards}</div>
        <div class="section">
            <div class="cta">
                <div>
                    <h2>Вашей отрасли ещё нет?</h2>
                    <p>Зарегистрируйте компанию — мы добавим подходящую категорию. Справочник отраслей расширяется
                        вместе с участниками площадки.</p>
                </div>
                <div class="cta__actions">
                    <a class="btn btn--primary" href="#/register">Разместить компанию</a>
                    <a class="btn btn--ghost" href="#/catalog">Смотреть каталог</a>
                </div>
            </div>
        </div>
    </div>`;
}

export async function howPage({ root }) {
    const stats = await api.stats();
    root.innerHTML = `<div class="container">
        <div class="page-head">
            <div class="eyebrow">Как это работает</div>
            <h1>Площадка для всей производственной цепочки</h1>
            <p>Одна учётная запись, один профиль компании и общая лента потребностей. Ниже — как устроены
                основные сценарии: поиск подрядчика, публикация заявки и отклик.</p>
        </div>

        <div class="stat-grid">
            <div class="stat-card"><div class="stat-card__value">${num(stats.companies)}</div>
                <div class="stat-card__label">компаний в каталоге</div></div>
            <div class="stat-card"><div class="stat-card__value">${num(stats.verifiedCompanies)}</div>
                <div class="stat-card__label">проверенных профилей</div></div>
            <div class="stat-card"><div class="stat-card__value">${num(stats.offers)}</div>
                <div class="stat-card__label">предложений товаров и услуг</div></div>
            <div class="stat-card"><div class="stat-card__value">${num(stats.cities)}</div>
                <div class="stat-card__label">городов присутствия</div></div>
        </div>

        <div class="cabinet-card">
            <h2>Для поставщика и подрядчика</h2>
            <div class="industry-grid" style="margin-top:20px">
                <div class="industry-card"><span class="industry-card__icon">01</span>
                    <span><span class="industry-card__title">Профиль компании</span>
                        <span class="industry-card__desc">Отрасль, город, описание, контакты, товары и услуги.
                            Профиль виден в каталоге и поиске.</span></span></div>
                <div class="industry-card"><span class="industry-card__icon">02</span>
                    <span><span class="industry-card__title">Заявки в ленте</span>
                        <span class="industry-card__desc">Смотрите, что нужно компаниям рядом: материалы,
                            перевозка, склад, подряд. Откликайтесь с ценой и условиями.</span></span></div>
                <div class="industry-card"><span class="industry-card__icon">03</span>
                    <span><span class="industry-card__title">Отклики и сделка</span>
                        <span class="industry-card__desc">Автор заявки принимает подходящее предложение,
                            заявка переходит в работу, контакты — в вашем кабинете.</span></span></div>
            </div>
        </div>

        <div class="cabinet-card" style="margin-top:16px">
            <h2>Для компании-заказчика</h2>
            <div class="industry-grid" style="margin-top:20px">
                <div class="industry-card"><span class="industry-card__icon">01</span>
                    <span><span class="industry-card__title">Опишите потребность</span>
                        <span class="industry-card__desc">Что нужно, объём, город, бюджет, срок. Чем точнее
                            заявка, тем точнее отклики.</span></span></div>
                <div class="industry-card"><span class="industry-card__icon">02</span>
                    <span><span class="industry-card__title">Сравните предложения</span>
                        <span class="industry-card__desc">Цена, сроки и рейтинг компании с отзывами других
                            участников — всё в одном экране.</span></span></div>
                <div class="industry-card"><span class="industry-card__icon">03</span>
                    <span><span class="industry-card__title">Закройте заявку</span>
                        <span class="industry-card__desc">Примите отклик, переведите заявку в работу и
                            оставьте отзыв о подрядчике.</span></span></div>
            </div>
        </div>

        <div class="cabinet-card" style="margin-top:16px">
            <h2>Частые вопросы</h2>
            <div class="stack mt-24">
                <div>
                    <h3>Размещение платное?</h3>
                    <p class="muted mb-0">В демонстрационной версии — нет. Площадка показывает полный сценарий:
                        регистрация, каталог, заявки и отклики.</p>
                </div>
                <div>
                    <h3>Что значит отметка «Проверена»?</h3>
                    <p class="muted mb-0">Администратор площадки подтвердил реквизиты и контакты компании.
                        Такие профили выше в выдаче каталога.</p>
                </div>
                <div>
                    <h3>Можно ли вести несколько компаний?</h3>
                    <p class="muted mb-0">Один аккаунт — одна карточка компании. Для группы компаний создайте
                        отдельные аккаунты представителей.</p>
                </div>
                <div>
                    <h3>Как связаться с исполнителем?</h3>
                    <p class="muted mb-0">В карточке компании указаны телефон, e-mail и сайт. Отклик на заявку
                        открывает диалог напрямую, без посредников.</p>
                </div>
            </div>
        </div>

        <div class="section">
            <div class="cta">
                <div>
                    <h2>Готовы занять место в цепочке?</h2>
                    <p>Регистрация занимает пару минут. Дальше — заявки, отклики и сделки без лишних
                        посредников.</p>
                </div>
                <div class="cta__actions">
                    <a class="btn btn--primary" href="#/register">Разместить компанию</a>
                    <a class="btn btn--ghost" href="#/catalog">Смотреть каталог</a>
                </div>
            </div>
        </div>
    </div>`;
}
