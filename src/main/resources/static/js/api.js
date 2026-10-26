// Тонкая обёртка над fetch: JSON, cookies сессии, единый формат ошибок.

export class ApiError extends Error {
    constructor(message, status, fields) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.fields = fields || {};
    }
}

async function request(method, path, body) {
    const options = {
        method,
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json' }
    };
    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }
    let response;
    try {
        response = await fetch(path, options);
    } catch (networkError) {
        throw new ApiError('Нет связи с сервером. Проверьте, что приложение запущено.', 0, {});
    }
    const text = await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
    }
    if (!response.ok) {
        const message = (data && data.message) || `Ошибка ${response.status}`;
        throw new ApiError(message, response.status, (data && data.fields) || {});
    }
    return data;
}

async function upload(path, formData) {
    let response;
    try {
        response = await fetch(path, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' },
            body: formData
        });
    } catch {
        throw new ApiError('Нет связи с сервером. Проверьте, что приложение запущено.', 0, {});
    }
    const text = await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
    }
    if (!response.ok) {
        throw new ApiError((data && data.message) || `Ошибка ${response.status}`,
            response.status, (data && data.fields) || {});
    }
    return data;
}

export const api = {
    get: (path) => request('GET', path),
    post: (path, body) => request('POST', path, body),
    put: (path, body) => request('PUT', path, body),
    patch: (path, body) => request('PATCH', path, body),
    del: (path) => request('DELETE', path),

    // сессия
    async me() {
        const response = await fetch('/api/auth/me', { credentials: 'same-origin', headers: { 'Accept': 'application/json' } });
        if (!response.ok) return null;
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    },
    login: (data) => request('POST', '/api/auth/login', data),
    register: (data) => request('POST', '/api/auth/register', data),
    logout: () => request('POST', '/api/auth/logout', {}),

    // справочники
    industries: () => request('GET', '/api/industries'),
    cities: () => request('GET', '/api/cities'),
    stats: () => request('GET', '/api/stats'),

    // компании
    companies: (params = {}) => request('GET', `/api/companies?${toQuery(params)}`),
    featured: () => request('GET', '/api/companies/featured'),
    company: (slug) => request('GET', `/api/companies/${encodeURIComponent(slug)}`),
    updateCompany: (id, data) => request('PUT', `/api/companies/${id}`, data),
    reviews: (slug) => request('GET', `/api/companies/${encodeURIComponent(slug)}/reviews`),
    addReview: (slug, data) => request('POST', `/api/companies/${encodeURIComponent(slug)}/reviews`, data),

    // предложения
    offersFor: (companyId) => request('GET', `/api/companies/${companyId}/offers`),
    latestOffers: () => request('GET', '/api/offers/latest'),
    createOffer: (data) => request('POST', '/api/offers', data),
    updateOffer: (id, data) => request('PUT', `/api/offers/${id}`, data),
    deleteOffer: (id) => request('DELETE', `/api/offers/${id}`),
    toggleOffer: (id, active) => request('PATCH', `/api/offers/${id}/active`, { active }),

    // заявки
    needs: (params = {}) => request('GET', `/api/needs?${toQuery(params)}`),
    latestNeeds: () => request('GET', '/api/needs/latest'),
    need: (id) => request('GET', `/api/needs/${id}`),
    createNeed: (data) => request('POST', '/api/needs', data),
    setNeedStatus: (id, status) => request('PATCH', `/api/needs/${id}/status`, { status }),
    deleteNeed: (id) => request('DELETE', `/api/needs/${id}`),

    // отклики
    proposals: (needId) => request('GET', `/api/needs/${needId}/proposals`),
    respond: (needId, data) => request('POST', `/api/needs/${needId}/proposals`, data),
    setProposalStatus: (id, status) => request('PATCH', `/api/proposals/${id}`, { status }),

    // кабинет и избранное
    cabinet: () => request('GET', '/api/cabinet'),
    favorites: () => request('GET', '/api/favorites'),
    addFavorite: (id) => request('POST', `/api/favorites/${id}`, {}),
    removeFavorite: (id) => request('DELETE', `/api/favorites/${id}`),

    // логотип и документы
    uploadLogo: (formData) => upload('/api/cabinet/logo', formData),
    removeLogo: () => request('DELETE', '/api/cabinet/logo'),
    uploadDocument: (formData) => upload('/api/cabinet/documents', formData),
    deleteDocument: (id) => request('DELETE', `/api/cabinet/documents/${id}`),

    // переписка по заявкам
    threads: () => request('GET', '/api/threads'),
    thread: (id) => request('GET', `/api/threads/${id}`),
    sendMessage: (id, body) => request('POST', `/api/threads/${id}/messages`, { body }),
    ensureThread: (needId, companyId) => request('POST', '/api/threads/ensure', { needId, companyId }),

    // тарифы
    billing: () => request('GET', '/api/billing'),
    upgrade: (plan) => request('POST', '/api/billing/upgrade', { plan }),

    // юридические услуги и заявки на статус
    legalServices: (params = {}) => request('GET', `/api/legal/services?${toQuery(params)}`),
    legalStatuses: () => request('GET', '/api/legal/statuses'),
    createLegalApplication: (data) => request('POST', '/api/legal/applications', data),
    myLegalApplications: () => request('GET', '/api/legal/applications'),
    incomingLegalApplications: () => request('GET', '/api/legal/applications/incoming'),
    legalApplication: (id) => request('GET', `/api/legal/applications/${id}`),
    payLegal: (id) => request('POST', `/api/legal/applications/${id}/pay`, {}),
    cancelLegal: (id) => request('POST', `/api/legal/applications/${id}/cancel`, {}),
    requestLegalDocuments: (id, comment) =>
        request('POST', `/api/legal/applications/${id}/request-documents`, { comment }),
    scheduleLegal: (id, appointmentAt) =>
        request('POST', `/api/legal/applications/${id}/schedule`, { appointmentAt }),
    completeLegal: (id) => request('POST', `/api/legal/applications/${id}/complete`, {}),
    confirmLegal: (id, code) => request('POST', `/api/legal/applications/${id}/confirm`, { code }),
    uploadLegalDocument: (id, formData) => upload(`/api/legal/applications/${id}/documents`, formData),
    deleteLegalDocument: (id, documentId) =>
        request('DELETE', `/api/legal/applications/${id}/documents/${documentId}`),
    registerPerson: (data) => request('POST', '/api/auth/register-person', data),

    // админ
    adminPending: () => request('GET', '/api/admin/pending-companies'),
    adminVerify: (id, verified) => request('PATCH', `/api/admin/companies/${id}/verify`, { verified }),
    adminStats: () => request('GET', '/api/admin/stats'),
    adminNeeds: () => request('GET', '/api/admin/needs')
};

function toQuery(params) {
    const search = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
            search.set(key, value);
        }
    });
    return search.toString();
}
