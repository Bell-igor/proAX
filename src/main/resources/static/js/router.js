// Мини-роутер на hash-навигации: #/catalog?industry=raw

const routes = [];

function compile(pattern) {
    const keys = [];
    const source = pattern
        .replace(/\/$/, '')
        .replace(/:[^/]+/g, (match) => {
            keys.push(match.slice(1));
            return '([^/]+)';
        });
    return { regex: new RegExp(`^${source || '/'}$`), keys };
}

export function route(pattern, handler) {
    routes.push({ ...compile(pattern), handler });
}

export function currentRoute() {
    const raw = window.location.hash.replace(/^#/, '') || '/';
    const [pathPart, queryPart = ''] = raw.split('?');
    const path = pathPart || '/';
    const query = {};
    new URLSearchParams(queryPart).forEach((value, key) => {
        query[key] = value;
    });
    return { path, query };
}

export async function resolveRoute(context) {
    const { path, query } = currentRoute();
    for (const candidate of routes) {
        const match = path.match(candidate.regex);
        if (!match) continue;
        const params = {};
        candidate.keys.forEach((key, index) => {
            params[key] = decodeURIComponent(match[index + 1] || '');
        });
        await candidate.handler({ ...context, params, query, path });
        return true;
    }
    return false;
}

export function navigate(path, { replace = false } = {}) {
    const target = `#${path.startsWith('/') ? path : `/${path}`}`;
    if (replace) {
        window.history.replaceState(null, '', target);
        window.dispatchEvent(new HashChangeEvent('hashchange'));
    } else if (window.location.hash === target) {
        window.dispatchEvent(new HashChangeEvent('hashchange'));
    } else {
        window.location.hash = target;
    }
}

export function href(path, query = {}) {
    const params = new URLSearchParams();
    Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') params.set(key, value);
    });
    const qs = params.toString();
    return `#${path}${qs ? `?${qs}` : ''}`;
}
