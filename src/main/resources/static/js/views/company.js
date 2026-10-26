import { api } from '../api.js';
import { esc, fmtDate, monogram, stars, ratingInline, verifiedBadge, emptyState, money, planBadge } from '../ui.js';
import { store } from '../store.js';
import { href } from '../router.js';

export default async function company({ root, params }) {
    root.innerHTML = `<div class="container"><div class="page-head"><div class="skeleton" style="height:200px"></div></div></div>`;

    let data = await api.company(params.slug);
    let tab = 'offers';

    const render = () => {
        const reviewForm = reviewFormHtml(data);
        const offers = data.offers.length
            ? `<div class="offer-grid">${data.offers.map((offer) => `
                <article class="offer-card">
                    <div class="row" style="gap:8px">
                        <span class="tag">${esc(offer.typeTitle)}</span>
                        ${offer.active ? '' : '<span class="badge badge--closed">не активно</span>'}
                    </div>
                    <h3 class="offer-card__title">${esc(offer.title)}</h3>
                    <p class="offer-card__desc">${esc(offer.description || '')}</p>
                    <div class="offer-card__price">${offer.priceFrom != null ? money(offer.priceFrom) : 'по запросу'}
                        ${offer.unit ? `<small>${esc(offer.unit)}</small>` : ''}</div>
                </article>`).join('')}</div>`
            : emptyState('Пока нет предложений',
                'Компания ещё не добавила товары или услуги в каталог.');

        const reviews = data.reviews.length
            ? data.reviews.map((review) => `
                <article class="review">
                    <div class="review__head">
                        <div>
                            <div class="review__author">${esc(review.authorName)}</div>
                            <div class="review__date">${fmtDate(review.createdAt)}</div>
                        </div>
                        <div style="margin-left:auto">${stars(review.rating)}</div>
                    </div>
                    <p class="review__text">${esc(review.text || 'Без текста')}</p>
                </article>`).join('')
            : emptyState('Отзывов пока нет',
                'Станьте первым, кто расскажет о сотрудничестве с этой компанией.');

        const contact = [
            data.website ? `<div class="contacts__row">
                <span class="contacts__label">Сайт</span>
                <a href="${esc(data.website)}" target="_blank" rel="noopener">${esc(data.website.replace(/^https?:\/\//, ''))}</a>
            </div>` : '',
            data.phone ? `<div class="contacts__row">
                <span class="contacts__label">Телефон</span>
                <a href="tel:${esc(data.phone.replace(/[^+\d]/g, ''))}">${esc(data.phone)}</a></div>` : '',
            data.email ? `<div class="contacts__row">
                <span class="contacts__label">E-mail</span>
                <a href="mailto:${esc(data.email)}">${esc(data.email)}</a></div>` : '',
            data.foundedYear ? `<div class="contacts__row">
                <span class="contacts__label">Год основания</span>
                <span>${data.foundedYear}</span></div>` : '',
            data.sizeTitle ? `<div class="contacts__row">
                <span class="contacts__label">Размер компании</span>
                <span>${esc(data.sizeTitle)}</span></div>` : '',
            data.ownerName ? `<div class="contacts__row">
                <span class="contacts__label">Представитель</span>
                <span>${esc(data.ownerName)}</span></div>` : ''
        ].filter(Boolean).join('');

        root.innerHTML = `<div class="container">
            <nav class="crumbs">
                <a href="#/">Главная</a><span class="crumbs__sep">/</span>
                <a href="#/catalog">Каталог</a><span class="crumbs__sep">/</span>
                <a href="${href('/catalog', { industry: data.industryCode })}">${esc(data.industryTitle)}</a>
                <span class="crumbs__sep">/</span><span>${esc(data.name)}</span>
            </nav>

            <div class="section--tight">
                <div class="profile">
                    <div class="profile__top">
                        ${monogram(data, true)}
                        <div class="profile__head">
                            <h1 class="profile__name">${esc(data.name)} ${verifiedBadge(data.verified)} ${planBadge(data)}</h1>
                            <div class="profile__meta">
                                <a class="badge badge--neutral" href="${href('/catalog', { industry: data.industryCode })}">
                                    ${data.industryIcon || ''} ${esc(data.industryTitle)}
                                </a>
                                ${data.city ? `<span>📍 ${esc(data.city)}</span>` : ''}
                                <span>${ratingInline(data)}</span>
                                <span class="muted small">на площадке с ${fmtDate(data.createdAt)}</span>
                            </div>
                            <div class="profile__actions">
                                ${data.mine
                                    ? '<a class="btn btn--dark btn--sm" href="#/cabinet?tab=profile">Редактировать профиль</a>'
                                    : `<button class="btn ${data.favorite ? 'btn--outline' : 'btn--dark'} btn--sm" id="fav-toggle">
                                            ${data.favorite ? '♥ В избранном' : '♡ В избранное'}
                                       </button>`}
                                ${data.website ? `<a class="btn btn--outline btn--sm" href="${esc(data.website)}"
                                    target="_blank" rel="noopener">Перейти на сайт</a>` : ''}
                                ${data.phone ? `<a class="btn btn--outline btn--sm"
                                    href="tel:${esc(data.phone.replace(/[^+\d]/g, ''))}">Позвонить</a>` : ''}
                            </div>
                        </div>
                    </div>

                    <div class="profile__body">
                        <div>
                            <div class="eyebrow">О компании</div>
                            <p class="profile__text">${esc(data.description || 'Компания пока не заполнила описание.')}</p>
                        </div>
                        <aside class="contacts">
                            <h4>Контакты</h4>
                            ${contact || '<p class="muted small">Контакты не заполнены.</p>'}
                        </aside>
                    </div>
                </div>
            </div>

            <div class="tabs" role="tablist">
                <button class="tab ${tab === 'offers' ? 'is-active' : ''}" data-tab="offers" role="tab">
                    Предложения (${data.offers.length})
                </button>
                <button class="tab ${tab === 'reviews' ? 'is-active' : ''}" data-tab="reviews" role="tab">
                    Отзывы (${data.reviewsCount})
                </button>
            </div>

            <div id="tab-body">
                ${tab === 'offers' ? offers : `
                    <div class="detail-grid">
                        <div>${reviews}</div>
                        <div>${reviewForm}</div>
                    </div>`}
            </div>
            <div style="height:48px"></div>
        </div>`;

        root.querySelectorAll('[data-tab]').forEach((button) => {
            button.addEventListener('click', () => {
                tab = button.dataset.tab;
                render();
            });
        });

        root.querySelector('#fav-toggle')?.addEventListener('click', async (event) => {
            const button = event.currentTarget;
            button.disabled = true;
            try {
                if (data.favorite) {
                    await api.removeFavorite(data.id);
                    data.favorite = false;
                } else {
                    await api.addFavorite(data.id);
                    data.favorite = true;
                }
                render();
            } catch (error) {
                window.dispatchEvent(new CustomEvent('api-error', { detail: error }));
            } finally {
                button.disabled = false;
            }
        });

        const form = root.querySelector('#review-form');
        if (form) {
            form.addEventListener('submit', async (event) => {
                event.preventDefault();
                const button = form.querySelector('button[type="submit"]');
                const errorBox = form.querySelector('.form-error');
                errorBox.classList.add('hidden');
                button.disabled = true;
                try {
                    const payload = Object.fromEntries(new FormData(form).entries());
                    await api.addReview(data.slug, { rating: Number(payload.rating), text: payload.text || null });
                    data = await api.company(params.slug);
                    tab = 'reviews';
                    render();
                    window.dispatchEvent(new CustomEvent('toast', { detail: 'Спасибо! Отзыв опубликован.' }));
                } catch (error) {
                    errorBox.textContent = error.message;
                    errorBox.classList.remove('hidden');
                } finally {
                    button.disabled = false;
                }
            });
        }
    };

    render();
}

function reviewFormHtml(data) {
    if (data.mine) {
        return `<div class="panel"><h3>Это ваша компания</h3>
            <p class="muted small">Отзывы оставляют другие участники площадки. Следите за новыми отзывами в кабинете.</p>
            <a class="btn btn--outline btn--sm" href="#/cabinet">Перейти в кабинет</a></div>`;
    }
    if (!store.user) {
        return `<div class="panel"><h3>Оставить отзыв</h3>
            <p class="muted small">Войдите, чтобы оценить работу компании — это помогает остальным участникам выбирать подрядчика.</p>
            <a class="btn btn--dark btn--sm" href="${href('/login', { next: `/company/${data.slug}` })}">Войти</a></div>`;
    }
    return `<div class="panel">
        <h3>Оставить отзыв</h3>
        <form id="review-form" novalidate>
            <div class="form-error hidden"></div>
            <div class="field">
                <label class="field__label" for="review-rating">Оценка</label>
                <select class="select" id="review-rating" name="rating">
                    <option value="5">5 — отлично</option>
                    <option value="4">4 — хорошо</option>
                    <option value="3">3 — нормально</option>
                    <option value="2">2 — плохо</option>
                    <option value="1">1 — очень плохо</option>
                </select>
            </div>
            <div class="field">
                <label class="field__label" for="review-text">Комментарий</label>
                <textarea class="textarea" id="review-text" name="text" maxlength="2000"
                          placeholder="Как прошло сотрудничество: сроки, качество, коммуникация"></textarea>
            </div>
            <button class="btn btn--primary btn--block" type="submit">Опубликовать отзыв</button>
        </form>
        <p class="muted small mt-16 mb-0">Один отзыв на компанию. Отзыв нельзя оставить о своей компании.</p>
    </div>`;
}
