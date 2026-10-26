import { api } from '../api.js';
import { esc, fmtDate, fmtShort, money, relative, plural, needStatusBadge, proposalStatusBadge, emptyState, toast } from '../ui.js';
import { store } from '../store.js';
import { navigate, href } from '../router.js';

export default async function need({ root, params }) {
    root.innerHTML = `<div class="container"><div class="page-head"><div class="skeleton" style="height:240px"></div></div></div>`;

    const needId = Number(params.id);
    let data = await api.need(needId);
    let proposals = [];

    try {
        proposals = await api.proposals(needId);
    } catch {
        proposals = [];
    }

    const render = () => {
        const proposalsHtml = proposals.length
            ? proposals.map((proposal) => `
                <article class="proposal">
                    <div class="proposal__head">
                        <a class="proposal__company" href="#/company/${esc(proposal.companySlug)}">
                            ${esc(proposal.companyName)}
                        </a>
                        ${proposalStatusBadge(proposal.status, proposal.statusTitle)}
                        <span class="proposal__price">${proposal.price != null ? money(proposal.price) : 'цена по запросу'}</span>
                    </div>
                    <p class="proposal__text">${esc(proposal.message || '')}</p>
                    <div class="row row--between">
                        <span class="muted small">${fmtDate(proposal.createdAt)}</span>
                        <span class="row" style="gap:6px">
                            ${data.mine || proposal.mine
                                ? `<button class="btn btn--outline btn--xs" type="button"
                                           data-chat="${proposal.companyId}">Переписка</button>`
                                : ''}
                            ${data.mine && proposal.status === 'NEW' ? `
                                <button class="btn btn--primary btn--xs" data-accept="${proposal.id}">Принять</button>
                                <button class="btn btn--outline btn--xs" data-reject="${proposal.id}">Отклонить</button>` : ''}
                        </span>
                    </div>
                </article>`).join('')
            : emptyState(data.mine ? 'Откликов пока нет' : 'Откликов пока нет',
                data.mine
                    ? 'Компании увидят заявку в ленте. Обычно первые отклики приходят в течение дня.'
                    : 'Станьте первым, кто предложит решение.');

        const actionPanel = actionPanelHtml(data);
        const proposalsTitle = data.mine
            ? `Отклики (${data.proposalsCount})`
            : (data.alreadyResponded ? 'Ваш отклик' : `Отклики (${data.proposalsCount})`);

        root.innerHTML = `<div class="container">
            <nav class="crumbs">
                <a href="#/">Главная</a><span class="crumbs__sep">/</span>
                <a href="#/needs">Заявки</a><span class="crumbs__sep">/</span>
                <a href="${href('/needs', { industry: data.industryCode })}">${esc(data.industryTitle)}</a>
                <span class="crumbs__sep">/</span><span>Заявка №${data.id}</span>
            </nav>

            <div class="page-head">
                <div class="row" style="gap:10px;margin-bottom:12px">
                    ${needStatusBadge(data.status, data.statusTitle)}
                    <span class="badge badge--neutral">${data.industryIcon || ''} ${esc(data.industryTitle)}</span>
                    ${data.mine ? '<span class="tag">моя заявка</span>' : ''}
                </div>
                <h1 style="font-size:clamp(1.7rem,3vw,2.4rem)">${esc(data.title)}</h1>
                <div class="row muted small" style="gap:14px">
                    ${data.city ? `<span>📍 ${esc(data.city)}</span>` : ''}
                    <span>Опубликована ${relative(data.createdAt)}</span>
                    ${data.deadline ? `<span>Срок: до ${fmtShort(data.deadline)}</span>` : ''}
                    <span>${data.proposalsCount} ${plural(data.proposalsCount, ['отклик', 'отклика', 'откликов'])}</span>
                </div>
            </div>

            <div class="detail-grid">
                <div>
                    <div class="panel">
                        <div class="panel__head"><h2>Описание</h2></div>
                        <p class="profile__text">${esc(data.description || 'Автор не добавил подробностей.')}</p>
                        <div class="divider"></div>
                        <dl class="kv">
                            <dt>Бюджет</dt><dd>${data.budget ? money(data.budget) : 'не указан'}</dd>
                            <dt>Срок</dt><dd>${data.deadline ? fmtDate(data.deadline) : 'не указан'}</dd>
                            <dt>Город</dt><dd>${data.city ? esc(data.city) : 'не указан'}</dd>
                            <dt>Отрасль</dt><dd>${esc(data.industryTitle)}</dd>
                        </dl>
                    </div>

                    <div class="panel" style="margin-top:16px">
                        <div class="panel__head">
                            <h2>${proposalsTitle}</h2>
                            <span class="muted small">${data.mine
                                ? 'Примите подходящее предложение — заявка перейдёт в работу'
                                : 'Отклики видны автору заявки'}</span>
                        </div>
                        ${proposalsHtml}
                    </div>
                </div>

                <aside>
                    ${actionPanel}
                    <div class="panel" style="margin-top:16px">
                        <div class="panel__head"><h3>Автор заявки</h3></div>
                        <div class="stack">
                            <div>
                                <div class="small muted">Контактное лицо</div>
                                <div style="font-weight:600">${esc(data.authorName)}</div>
                            </div>
                            ${data.authorCompanyName ? `
                                <div>
                                    <div class="small muted">Компания</div>
                                    <a class="link-btn" href="#/company/${esc(data.authorCompanySlug)}">
                                        ${esc(data.authorCompanyName)}</a>
                                </div>` : ''}
                        </div>
                    </div>
                </aside>
            </div>
            <div style="height:48px"></div>
        </div>`;

        root.querySelectorAll('[data-accept]').forEach((button) => {
            button.addEventListener('click', () => changeProposalStatus(Number(button.dataset.accept), 'ACCEPTED'));
        });
        root.querySelectorAll('[data-reject]').forEach((button) => {
            button.addEventListener('click', () => changeProposalStatus(Number(button.dataset.reject), 'REJECTED'));
        });

        root.querySelectorAll('[data-chat]').forEach((button) => {
            button.addEventListener('click', async () => {
                button.disabled = true;
                try {
                    const thread = await api.ensureThread(needId, Number(button.dataset.chat));
                    navigate(`/chat/${thread.id}`);
                } catch (error) {
                    toast(error.message, 'error');
                    button.disabled = false;
                }
            });
        });

        const respondForm = root.querySelector('#respond-form');
        if (respondForm) {
            respondForm.addEventListener('submit', async (event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const errorBox = form.querySelector('.form-error');
                errorBox.classList.add('hidden');
                const payload = Object.fromEntries(new FormData(form).entries());
                const button = form.querySelector('button[type="submit"]');
                button.disabled = true;
                try {
                    await api.respond(needId, {
                        message: payload.message,
                        price: payload.price ? Number(payload.price) : null
                    });
                    toast('Отклик отправлен автору заявки');
                    await reload();
                } catch (error) {
                    errorBox.textContent = error.message;
                    errorBox.classList.remove('hidden');
                } finally {
                    button.disabled = false;
                }
            });
        }

        const statusForm = root.querySelector('#status-form');
        if (statusForm) {
            statusForm.addEventListener('submit', async (event) => {
                event.preventDefault();
                const status = new FormData(statusForm).get('status');
                try {
                    data = await api.setNeedStatus(needId, status);
                    toast('Статус заявки обновлён');
                    render();
                } catch (error) {
                    toast(error.message, 'error');
                }
            });
        }

        root.querySelector('#delete-need')?.addEventListener('click', async () => {
            if (!window.confirm('Удалить заявку? Действие необратимо.')) return;
            try {
                await api.deleteNeed(needId);
                toast('Заявка удалена');
                navigate('/needs');
            } catch (error) {
                toast(error.message, 'error');
            }
        });
    };

    const reload = async () => {
        data = await api.need(needId);
        try {
            proposals = await api.proposals(needId);
        } catch {
            proposals = [];
        }
        render();
    };

    const changeProposalStatus = async (proposalId, status) => {
        try {
            await api.setProposalStatus(proposalId, status);
            toast(status === 'ACCEPTED' ? 'Отклик принят, заявка переведена в работу' : 'Отклик отклонён');
            await reload();
        } catch (error) {
            toast(error.message, 'error');
        }
    };

    render();
}

function actionPanelHtml(data) {
    if (data.mine) {
        return `<div class="panel">
            <div class="panel__head"><h3>Управление заявкой</h3></div>
            <form id="status-form">
                <div class="field">
                    <label class="field__label" for="status-select">Статус</label>
                    <select class="select" id="status-select" name="status">
                        <option value="OPEN" ${data.status === 'OPEN' ? 'selected' : ''}>Открыта</option>
                        <option value="IN_PROGRESS" ${data.status === 'IN_PROGRESS' ? 'selected' : ''}>В работе</option>
                        <option value="CLOSED" ${data.status === 'CLOSED' ? 'selected' : ''}>Закрыта</option>
                    </select>
                </div>
                <button class="btn btn--dark btn--block" type="submit">Сохранить статус</button>
            </form>
            <button class="btn btn--danger btn--block mt-16" type="button" id="delete-need">Удалить заявку</button>
        </div>`;
    }

    if (!store.user) {
        return `<div class="panel">
            <div class="panel__head"><h3>Откликнуться</h3></div>
            <p class="muted small">Войдите как представитель компании, чтобы отправить предложение автору заявки.</p>
            <a class="btn btn--primary btn--block" href="${href('/login', { next: `/need/${data.id}` })}">Войти и откликнуться</a>
        </div>`;
    }

    if (data.alreadyResponded) {
        return `<div class="panel">
            <div class="panel__head"><h3>Ваш отклик отправлен</h3></div>
            <p class="muted small">Автор заявки увидит предложение и свяжется с вами. Отслеживайте статус в кабинете.</p>
            <a class="btn btn--outline btn--block" href="#/cabinet?tab=proposals">Мои отклики</a>
        </div>`;
    }

    if (!data.canRespond) {
        return `<div class="panel">
            <div class="panel__head"><h3>Отклики закрыты</h3></div>
            <p class="muted small">Заявка не принимает новые предложения.</p>
            <a class="btn btn--outline btn--block" href="#/needs">Другие заявки</a>
        </div>`;
    }

    return `<div class="panel">
        <div class="panel__head"><h3>Откликнуться на заявку</h3></div>
        <form id="respond-form" novalidate>
            <div class="form-error hidden"></div>
            <div class="field">
                <label class="field__label" for="respond-message">Предложение <span class="req">*</span></label>
                <textarea class="textarea" id="respond-message" name="message" maxlength="2000" required
                          placeholder="Что предлагаете, сроки, условия"></textarea>
            </div>
            <div class="field">
                <label class="field__label" for="respond-price">Цена, ₽</label>
                <input class="input" id="respond-price" name="price" type="number" min="0" step="1000">
            </div>
            <button class="btn btn--primary btn--block" type="submit">Отправить отклик</button>
        </form>
    </div>`;
}
