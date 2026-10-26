// Общие визуальные примитивы и форматирование.

export const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[char]));

const numberFormat = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 0 });

export function money(value) {
    if (value === null || value === undefined || value === '') return '—';
    const number = Number(value);
    if (Number.isNaN(number)) return '—';
    const format = Number.isInteger(number)
        ? numberFormat
        : new Intl.NumberFormat('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return `${format.format(number)} ₽`;
}

export const num = (value) => numberFormat.format(Number(value || 0));

export function fmtDate(iso) {
    if (!iso) return '—';
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' }).format(date);
}

export function fmtShort(iso) {
    if (!iso) return '—';
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'short' }).format(date);
}

export function relative(iso) {
    if (!iso) return '';
    const date = new Date(iso);
    const diffDays = Math.floor((Date.now() - date.getTime()) / 86_400_000);
    if (diffDays <= 0) return 'сегодня';
    if (diffDays === 1) return 'вчера';
    if (diffDays < 7) return `${diffDays} дн. назад`;
    return fmtShort(iso);
}

export function plural(count, forms) {
    const n = Math.abs(count) % 100;
    const n1 = n % 10;
    if (n > 10 && n < 20) return forms[2];
    if (n1 > 1 && n1 < 5) return forms[1];
    if (n1 === 1) return forms[0];
    return forms[2];
}

export function initials(name) {
    const clean = String(name || '').replace(/[«»"']/g, ' ').trim();
    const parts = clean.split(/\s+/).filter(Boolean);
    if (!parts.length) return '—';
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
}

export function monogram(company, large = false) {
    const accent = company.accentColor || '#C8F169';
    const sizeClass = large ? 'monogram--lg' : '';
    if (company.logoUrl) {
        return `<span class="monogram monogram--logo ${sizeClass}" style="--accent:${esc(accent)}">
                    <span class="monogram__initials">${esc(initials(company.name))}</span>
                    <img src="${esc(company.logoUrl)}" alt="${esc(company.name)}" loading="lazy"
                         onerror="this.remove()">
                </span>`;
    }
    return `<div class="monogram ${sizeClass}" style="--accent:${esc(accent)}">${esc(initials(company.name))}</div>`;
}

/** Бейдж платного тарифа: показывается, пока продвижение активно. */
export function planBadge(company) {
    if (!company || !company.promoted) return '';
    const code = String(company.planCode || '').toLowerCase();
    const label = company.planCode === 'PREMIUM' ? 'Premium' : 'PRO';
    return `<span class="badge badge--plan badge--${esc(code)}">${esc(label)}</span>`;
}

export function stars(rating) {
    const value = Math.round(Number(rating) || 0);
    let html = '<span class="stars" aria-hidden="true">';
    for (let i = 1; i <= 5; i += 1) {
        html += `<span class="${i <= value ? '' : 'is-off'}">★</span>`;
    }
    return `${html}</span>`;
}

export function ratingInline(company) {
    if (!company.reviewsCount) {
        return '<span class="small muted">нет отзывов</span>';
    }
    return `<span class="rating"><span class="rating__star">★</span>${Number(company.rating).toFixed(1)}</span>
            <span class="small muted">${company.reviewsCount} ${plural(company.reviewsCount, ['отзыв', 'отзыва', 'отзывов'])}</span>`;
}

export function verifiedBadge(verified) {
    return verified
        ? '<span class="badge badge--verified">✓ Проверена</span>'
        : '<span class="badge badge--pending">На проверке</span>';
}

export function needStatusBadge(status, title) {
    const map = { OPEN: 'badge--open', IN_PROGRESS: 'badge--progress', CLOSED: 'badge--closed' };
    return `<span class="badge ${map[status] || 'badge--neutral'}">${esc(title || status)}</span>`;
}

export function proposalStatusBadge(status, title) {
    const map = { NEW: 'badge--new', ACCEPTED: 'badge--accepted', REJECTED: 'badge--rejected' };
    return `<span class="badge ${map[status] || 'badge--neutral'}">${esc(title || status)}</span>`;
}

export function companyCard(company, options = {}) {
    const favorite = Boolean(options.favorite || company.favorite);
    const favButton = options.hideFavorite
        ? ''
        : `<button class="icon-btn js-fav ${favorite ? 'is-active' : ''}" data-id="${company.id}"
                   aria-label="${favorite ? 'Убрать из избранного' : 'В избранное'}"
                   title="${favorite ? 'Убрать из избранного' : 'В избранное'}">${favorite ? '♥' : '♡'}</button>`;
    return `<article class="company-card">
        <div class="company-card__top">
            ${monogram(company)}
            <div class="company-card__head">
                <h3 class="company-card__name"><a href="#/company/${esc(company.slug)}">${esc(company.name)}</a></h3>
                <div class="company-card__meta">${esc(company.industryTitle || '')}${company.city ? ` · ${esc(company.city)}` : ''}</div>
            </div>
            ${favButton}
        </div>
        <p class="company-card__desc">${esc(company.shortDescription || 'Компания пока не заполнила описание.')}</p>
        <div class="company-card__foot">
            <span class="row" style="gap:6px">${verifiedBadge(company.verified)}${planBadge(company)}</span>
            <span class="row" style="gap:6px">${ratingInline(company)}</span>
        </div>
    </article>`;
}

export function needRow(need) {
    const budget = need.budget ? money(need.budget) : 'Бюджет не указан';
    const responses = `${need.proposalsCount} ${plural(need.proposalsCount, ['отклик', 'отклика', 'откликов'])}`;
    const deadlineText = need.deadline ? ` · до ${fmtShort(need.deadline)}` : '';
    return `<article class="need-row">
        <div>
            <div class="need-row__meta">
                ${needStatusBadge(need.status, need.statusTitle)}
                <span>${esc(need.industryTitle || '')}</span>
                ${need.city ? `<span>· ${esc(need.city)}</span>` : ''}
                <span>· ${relative(need.createdAt)}${deadlineText}</span>
                ${need.mine ? '<span class="tag">моя заявка</span>' : ''}
            </div>
            <h3 class="need-row__title"><a href="#/need/${need.id}">${esc(need.title)}</a></h3>
            <p class="need-row__desc">${esc(need.description || '')}</p>
        </div>
        <div class="need-row__side">
            <div class="need-row__budget">${budget}</div>
            <div class="need-row__responses">${responses}</div>
        </div>
    </article>`;
}

export function emptyState(title, text, actionHtml = '') {
    return `<div class="empty"><h3>${esc(title)}</h3><p>${esc(text)}</p>${actionHtml}</div>`;
}

export function skeletonCards(count = 6) {
    return `<div class="company-grid">${Array.from({ length: count })
        .map(() => '<div class="skeleton skeleton-card"></div>').join('')}</div>`;
}

export function skeletonRows(count = 4) {
    return `<div class="stack">${Array.from({ length: count })
        .map(() => '<div class="skeleton" style="height:96px"></div>').join('')}</div>`;
}

// ------------------------------------------------------------------ тосты

export function toast(message, type = 'success') {
    const root = document.getElementById('toasts');
    if (!root) return;
    const node = document.createElement('div');
    node.className = `toast toast--${type}`;
    node.textContent = message;
    root.appendChild(node);
    setTimeout(() => {
        node.style.opacity = '0';
        node.style.transition = 'opacity .3s ease';
        setTimeout(() => node.remove(), 320);
    }, 4200);
}

// ------------------------------------------------------------------ модалка

let modalSubmitHandler = null;

export function openModal({ title, body, submitLabel = 'Сохранить', onSubmit, wide = false }) {
    const root = document.getElementById('modal-root');
    root.innerHTML = `<div class="modal-backdrop" data-close="1">
        <div class="modal ${wide ? 'modal--wide' : ''}" role="dialog" aria-modal="true" aria-label="${esc(title)}">
            <div class="modal__head">
                <h2>${esc(title)}</h2>
                <button class="modal__close" type="button" data-close="1" aria-label="Закрыть">✕</button>
            </div>
            <form id="modal-form" novalidate>
                <div class="form-error hidden" data-modal-error></div>
                ${body}
                <div class="form-actions">
                    <button class="btn btn--primary" type="submit">${esc(submitLabel)}</button>
                    <button class="btn btn--outline" type="button" data-close="1">Отмена</button>
                </div>
            </form>
        </div>
    </div>`;

    const form = root.querySelector('#modal-form');
    const errorBox = root.querySelector('[data-modal-error]');
    modalSubmitHandler = onSubmit;

    root.querySelectorAll('[data-close]').forEach((node) => {
        node.addEventListener('click', (event) => {
            if (event.target === node) closeModal();
        });
    });

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        errorBox.classList.add('hidden');
        const submit = form.querySelector('button[type="submit"]');
        const label = submit.textContent;
        submit.disabled = true;
        submit.textContent = 'Сохраняем…';
        try {
            await modalSubmitHandler(new FormData(form), form);
            closeModal();
        } catch (error) {
            errorBox.textContent = error.message || 'Не удалось сохранить';
            errorBox.classList.remove('hidden');
            markFields(form, error.fields);
        } finally {
            submit.disabled = false;
            submit.textContent = label;
        }
    });

    const firstField = form.querySelector('input:not([type="hidden"]), select, textarea');
    if (firstField) firstField.focus();
    document.body.style.overflow = 'hidden';
}

export function closeModal() {
    const root = document.getElementById('modal-root');
    if (root) root.innerHTML = '';
    document.body.style.overflow = '';
    modalSubmitHandler = null;
}

export function markFields(form, fields) {
    if (!form || !fields) return;
    form.querySelectorAll('.field__error').forEach((node) => node.remove());
    form.querySelectorAll('.has-error').forEach((node) => node.classList.remove('has-error'));
    Object.entries(fields).forEach(([name, message]) => {
        const input = form.querySelector(`[name="${name}"]`);
        if (!input) return;
        input.classList.add('has-error');
        const hint = document.createElement('div');
        hint.className = 'field__error';
        hint.textContent = message;
        input.closest('.field')?.appendChild(hint);
    });
}

export function formObject(form) {
    const data = {};
    new FormData(form).forEach((value, key) => {
        data[key] = typeof value === 'string' ? value.trim() : value;
    });
    return data;
}

export async function withBusy(button, action) {
    if (!button) return action();
    const label = button.textContent;
    button.disabled = true;
    button.textContent = 'Минуту…';
    try {
        return await action();
    } finally {
        button.disabled = false;
        button.textContent = label;
    }
}
