(() => {
    let csrf = null;
    let currentUser = null;

    async function loadCsrf() {
        if (csrf) return csrf;

        const response = await fetch("/api/auth/csrf", {
            credentials: "same-origin",
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error("Güvenlik anahtarı alınamadı.");
        }

        csrf = await response.json();
        return csrf;
    }

    async function withCsrf(options = {}) {
        const method = String(options.method || "GET").toUpperCase();
        const securedOptions = {
            credentials: "same-origin",
            ...options
        };

        if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
            const token = await loadCsrf();
            const headers = new Headers(options.headers || {});
            headers.set(token.headerName, token.token);
            securedOptions.headers = headers;
        }

        return securedOptions;
    }

    async function loadCurrentUser() {
        if (currentUser) return currentUser;

        const response = await fetch("/api/auth/me", {
            credentials: "same-origin",
            cache: "no-store"
        });

        if (response.status === 401) {
            window.location.replace("/login.html");
            throw new Error("Oturum bulunamadı.");
        }

        if (!response.ok) {
            throw new Error("Kullanıcı bilgisi alınamadı.");
        }

        currentUser = await response.json();
        renderAccount(currentUser);
        return currentUser;
    }

    function renderAccount(user) {
        document.querySelectorAll("[data-session-name]").forEach(element => {
            element.textContent = user.fullName;
        });

        document.querySelectorAll("[data-session-role]").forEach(element => {
            element.textContent = user.role === "ADMIN" ? "Yönetici" : "Müşteri";
        });

        document.body.dataset.userRole = user.role;

        document.querySelectorAll("[data-admin-only]").forEach(element => {
            element.hidden = user.role !== "ADMIN";
        });
    }

    async function logout() {
        try {
            const options = await withCsrf({ method: "POST" });
            await fetch("/logout", options);
        } finally {
            window.location.replace("/login.html?logout");
        }
    }

    document.addEventListener("click", event => {
        const button = event.target.closest("[data-logout]");
        if (!button) return;
        event.preventDefault();
        logout();
    });

    window.WildlifeSession = {
        loadCsrf,
        withCsrf,
        loadCurrentUser,
        logout,
        getCurrentUser: () => currentUser
    };
})();
