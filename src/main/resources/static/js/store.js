import { api } from './api.js';

export const store = {
    user: null
};

export async function loadUser() {
    try {
        store.user = await api.me();
    } catch {
        store.user = null;
    }
    return store.user;
}

export function isMember() {
    return Boolean(store.user);
}

export function isAdmin() {
    return Boolean(store.user && store.user.admin);
}

export function setUser(user) {
    store.user = user;
    document.dispatchEvent(new CustomEvent('auth-changed'));
}

export function dropUser() {
    if (!store.user) return;
    store.user = null;
    document.dispatchEvent(new CustomEvent('auth-changed'));
}
