import { api } from '../api.js';
import { esc, relative, plural, emptyState, toast } from '../ui.js';
import { store } from '../store.js';
import { navigate } from '../router.js';

let pollTimer = null;

export default async function chat({ root, params, query }) {
    if (!store.user) {
        navigate('/login?next=/chat');
        return;
    }
    clearInterval(pollTimer);

    const threads = await api.threads();
    let activeId = params.id ? Number(params.id) : (query.thread ? Number(query.thread) : null);
    if (activeId && !threads.some((thread) => thread.id === activeId)) {
        activeId = null;
    }
    // Без выбранного диалога открываем последний — так страница сразу полезна
    if (!activeId && threads.length) {
        activeId = threads[0].id;
    }

    let conversation = null;
    if (activeId) {
        try {
            conversation = await api.thread(activeId);
        } catch (error) {
            toast(error.message, 'error');
            activeId = null;
        }
    }

    const listHtml = threads.length
        ? threads.map((thread) => chatListItem(thread, thread.id === activeId)).join('')
        : `<div class="chat-empty">
               <p>Диалогов пока нет.</p>
               <p class="small">Переписка открывается, когда компания откликается на заявку,
                  или когда вы отвечаете на чужую заявку.</p>
               <a class="btn btn--dark btn--sm" href="#/needs">Смотреть заявки</a>
           </div>`;

    root.innerHTML = `<div class="container">
        <div class="page-head">
            <div class="row row--between" style="align-items:flex-end">
                <div>
                    <div class="eyebrow">Переписка</div>
                    <h1>Сообщения</h1>
                    <p>Диалоги по заявкам: детали, сроки и документы можно обсудить, не выходя с площадки.</p>
                </div>
                <a class="btn btn--outline btn--sm" href="#/cabinet">В кабинет</a>
            </div>
        </div>

        <div class="chat-layout">
            <aside class="chat-list" id="chat-list">${listHtml}</aside>
            <div id="chat-panel">${conversation ? panelHtml(conversation) : emptyPanelHtml(threads.length)}</div>
        </div>
        <div style="height:48px"></div>
    </div>`;

    root.querySelectorAll('[data-thread]').forEach((button) => {
        button.addEventListener('click', () => navigate(`/chat/${button.dataset.thread}`));
    });

    if (!conversation) return;

    const messagesBox = root.querySelector('#chat-messages');
    const form = root.querySelector('#chat-form');
    const input = form.querySelector('textarea');

    const renderMessages = (items, scroll = true) => {
        messagesBox.innerHTML = items.length
            ? items.map(messageHtml).join('')
            : '<div class="chat-empty">Сообщений пока нет — напишите первым.</div>';
        if (scroll) messagesBox.scrollTop = messagesBox.scrollHeight;
    };

    renderMessages(conversation.messages);

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const text = input.value.trim();
        if (!text) return;
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        try {
            const message = await api.sendMessage(conversation.thread.id, text);
            conversation.messages.push(message);
            input.value = '';
            renderMessages(conversation.messages);
            updatePreview(listMessage(conversation));
        } catch (error) {
            toast(error.message, 'error');
        } finally {
            button.disabled = false;
            input.focus();
        }
    });

    input.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    pollTimer = setInterval(async () => {
        try {
            const fresh = await api.thread(conversation.thread.id);
            if (fresh.messages.length !== conversation.messages.length) {
                conversation = fresh;
                renderMessages(fresh.messages);
            }
        } catch {
            clearInterval(pollTimer);
        }
    }, 10_000);
}

function chatListItem(thread, active) {
    const peer = thread.role === 'AUTHOR' ? thread.companyName : thread.authorName;
    return `<button class="chat-list__item ${active ? 'is-active' : ''}" data-thread="${thread.id}">
        <div class="chat-list__peer">${esc(peer)}</div>
        <div class="chat-list__need">${esc(thread.needTitle)}</div>
        <div class="chat-list__preview">${esc(thread.lastMessagePreview || 'Диалог открыт — напишите первым')}</div>
        <div class="chat-list__meta">
            <span>${relative(thread.lastMessageAt)}</span>
            <span>${thread.messagesCount} ${plural(thread.messagesCount, ['сообщение', 'сообщения', 'сообщений'])}</span>
        </div>
    </button>`;
}

function panelHtml(conversation) {
    const thread = conversation.thread;
    const peer = thread.role === 'AUTHOR' ? thread.companyName : thread.authorName;
    return `<div class="chat-panel">
        <div class="chat-panel__head">
            <h2>${esc(peer)}</h2>
            <p>Заявка: <a class="link-btn" href="#/need/${thread.needId}">${esc(thread.needTitle)}</a></p>
        </div>
        <div class="chat-messages" id="chat-messages"></div>
        <form class="chat-composer" id="chat-form">
            <textarea class="textarea" name="body" maxlength="2000" rows="2"
                      placeholder="Напишите сообщение…"
                      title="Enter — отправить, Shift+Enter — новая строка"></textarea>
            <button class="btn btn--primary" type="submit">Отправить</button>
        </form>
    </div>`;
}

function emptyPanelHtml(hasThreads) {
    return `<div class="chat-panel">
        <div class="chat-empty">
            <h3>${hasThreads ? 'Выберите диалог' : 'Здесь появятся переписки'}</h3>
            <p>${hasThreads
                ? 'Слева — все диалоги по заявкам. Сообщения видны только двум сторонам.'
                : 'Откликнитесь на заявку или дождитесь отклика — и диалог откроется автоматически.'}</p>
        </div>
    </div>`;
}

function messageHtml(message) {
    const meta = `<div class="chat-msg__meta">
            <span>${esc(message.senderName)}${message.senderCompanyName ? ` · ${esc(message.senderCompanyName)}` : ''}</span>
            <span>${relative(message.createdAt)}</span>
        </div>`;
    return `<div class="chat-msg ${message.mine ? 'is-mine' : ''}">
        <div class="chat-msg__bubble">${esc(message.body)}</div>
        ${meta}
    </div>`;
}

function listMessage(conversation) {
    return {
        id: conversation.thread.id,
        preview: conversation.messages.length
            ? conversation.messages[conversation.messages.length - 1].body
            : ''
    };
}

function updatePreview(message) {
    const item = document.querySelector(`[data-thread="${message.id}"] .chat-list__preview`);
    if (item) item.textContent = message.preview;
}
