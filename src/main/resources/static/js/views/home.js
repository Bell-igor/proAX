import { api } from '../api.js';
import { esc, num, plural, companyCard, needRow, skeletonCards } from '../ui.js';
import { href } from '../router.js';

export default async function home({ root }) {
    root.innerHTML = `<section class="hero"><div class="container"><div class="hero__eyebrow">Загружаем площадку</div>
        <div class="skeleton" style="height:120px;max-width:640px;margin-bottom:24px"></div></div></section>
        <div class="container section">${skeletonCards(6)}</div>`;

    const [industries, featured, needs, stats] = await Promise.all([
        api.industries(),
        api.featured(),
        api.latestNeeds(),
        api.stats()
    ]);

    const chips = industries.slice(0, 6).map((industry) =>
        `<a class="chip-link" href="${href('/catalog', { industry: industry.code })}">${industry.icon} ${esc(industry.title)}</a>`
    ).join('');

    const industryCards = industries.slice(0, 8).map((industry) => `
        <a class="industry-card" href="${href('/catalog', { industry: industry.code })}">
            <span class="industry-card__icon">${industry.icon || '•'}</span>
            <span>
                <span class="industry-card__title">${esc(industry.title)}</span>
                <span class="industry-card__desc">${esc(industry.description || '')}</span>
                <span class="industry-card__count">${industry.companiesCount}
                    ${plural(industry.companiesCount, ['компания', 'компании', 'компаний'])}</span>
            </span>
        </a>`).join('');

    const featuredCards = featured.length
        ? featured.map((company) => companyCard(company)).join('')
        : '<p class="muted">Проверенные компании появятся после модерации.</p>';

    const needItems = needs.length
        ? needs.slice(0, 5).map((need) => needRow(need)).join('')
        : '<p class="muted">Заявок пока нет — станьте первым.</p>';

    root.innerHTML = `
    <section class="hero">
        <div class="container">
            <div class="hero__inner">
                <div>
                    <div class="hero__eyebrow">B2B-экосистема · производственная цепочка</div>
                    <h1 class="hero__title">Вся производственная цепочка — на <em>одной</em> площадке</h1>
                    <p class="hero__lead">Поставщики сырья, заводы, перевозчики, склады, юристы, финансы и сервисы —
                        в одном каталоге. Находите подрядчиков, публикуйте потребности и получайте предложения
                        напрямую, без посредников.</p>

                    <form class="hero__search" id="hero-search" role="search">
                        <input type="search" name="q" placeholder="Например, металлопрокат или склад"
                               aria-label="Поиск по каталогу">
                        <button class="btn btn--primary" type="submit">Найти</button>
                    </form>

                    <div class="hero__chips">${chips}</div>
                </div>

                <aside class="hero__aside">
                    <h3>Как устроена цепочка</h3>
                    <div class="chain">
                        <div class="chain__item"><span class="chain__num">01</span><span>Сырьё и материалы</span></div>
                        <div class="chain__item"><span class="chain__num">02</span><span>Производство и переработка</span></div>
                        <div class="chain__item"><span class="chain__num">03</span><span>Логистика и перевозки</span></div>
                        <div class="chain__item"><span class="chain__num">04</span><span>Складские помещения</span></div>
                        <div class="chain__item"><span class="chain__num">05</span><span>Юристы, финансы, сервисы</span></div>
                    </div>
                </aside>
            </div>

            <div class="hero__stats">
                <div class="stat">
                    <div class="stat__value">${num(stats.companies)}</div>
                    <div class="stat__label">компаний на площадке</div>
                </div>
                <div class="stat">
                    <div class="stat__value">${num(stats.industries)}</div>
                    <div class="stat__label">отраслей и ролей в цепочке</div>
                </div>
                <div class="stat">
                    <div class="stat__value">${num(stats.openNeeds)}</div>
                    <div class="stat__label">открытых заявок</div>
                </div>
                <div class="stat">
                    <div class="stat__value">${num(stats.proposals)}</div>
                    <div class="stat__label">откликов компаний</div>
                </div>
            </div>
        </div>
    </section>

    <section class="section">
        <div class="container">
            <div class="section-head">
                <div class="section-head__text">
                    <div class="eyebrow">Каталог</div>
                    <h2>Отрасли площадки</h2>
                    <p>Каждый участник занимает своё место в цепочке: от поставки ресурсов до юридического
                        сопровождения сделки. Выберите отрасль, чтобы увидеть компании.</p>
                </div>
                <a class="btn btn--outline" href="#/industries">Все ${industries.length} отраслей</a>
            </div>
            <div class="industry-grid">${industryCards}</div>
        </div>
    </section>

    <section class="section section--surface">
        <div class="container">
            <div class="section-head">
                <div class="section-head__text">
                    <div class="eyebrow">Проверенные участники</div>
                    <h2>Компании с подтверждённым профилем</h2>
                    <p>Профиль с отметкой «Проверена» прошёл модерацию: подтверждены реквизиты и контактные данные.</p>
                </div>
                <a class="btn btn--outline" href="#/catalog">Весь каталог</a>
            </div>
            <div class="company-grid">${featuredCards}</div>
        </div>
    </section>

    <section class="section">
        <div class="container">
            <div class="section-head">
                <div class="section-head__text">
                    <div class="eyebrow">Лента потребностей</div>
                    <h2>Свежие заявки</h2>
                    <p>Компании сами рассказывают, что им нужно: материалы, перевозка, склад, подрядчик или консультация.</p>
                </div>
                <a class="btn btn--outline" href="#/needs">Все заявки</a>
            </div>
            <div class="need-list">${needItems}</div>
        </div>
    </section>

    <section class="section section--surface" id="how">
        <div class="container">
            <div class="section-head">
                <div class="section-head__text">
                    <div class="eyebrow">Как это работает</div>
                    <h2>Три шага до сделки</h2>
                </div>
            </div>
            <div class="industry-grid">
                <div class="industry-card">
                    <span class="industry-card__icon">01</span>
                    <span>
                        <span class="industry-card__title">Соберите профиль компании</span>
                        <span class="industry-card__desc">Отрасль, город, описание, контакты и предложения —
                            то, по чему вас найдут в каталоге.</span>
                        <a class="link-btn small" href="#/register">Зарегистрировать компанию</a>
                    </span>
                </div>
                <div class="industry-card">
                    <span class="industry-card__icon">02</span>
                    <span>
                        <span class="industry-card__title">Опубликуйте потребность</span>
                        <span class="industry-card__desc">Заявка с бюджетом и сроком видна всем участникам,
                            отклики собираются в одном месте.</span>
                        <a class="link-btn small" href="#/needs">Смотреть заявки</a>
                    </span>
                </div>
                <div class="industry-card">
                    <span class="industry-card__icon">03</span>
                    <span>
                        <span class="industry-card__title">Выберите исполнителя</span>
                        <span class="industry-card__desc">Сравните предложения и рейтинг компаний, примите
                            отклик и ведите сделку напрямую.</span>
                        <a class="link-btn small" href="#/catalog">Открыть каталог</a>
                    </span>
                </div>
            </div>
        </div>
    </section>

    <section class="section section--tight">
        <div class="container">
            <div class="cta">
                <div>
                    <h2>Вашей компании ещё нет на ProAX?</h2>
                    <p>Разместите профиль за пару минут: без звонков менеджеров и платных тарифов на старте.
                        Первые отклики на заявки приходят в тот же день.</p>
                </div>
                <div class="cta__actions">
                    <a class="btn btn--primary" href="#/register">Разместить компанию</a>
                    <a class="btn btn--ghost" href="#/how">Как это работает</a>
                </div>
            </div>
        </div>
    </section>`;

    const search = root.querySelector('#hero-search');
    if (search) {
        search.addEventListener('submit', (event) => {
            event.preventDefault();
            const value = new FormData(search).get('q')?.toString().trim() || '';
            window.location.hash = href('/catalog', { q: value });
        });
    }
}
