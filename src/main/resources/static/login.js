const form = document.querySelector("#loginForm");
const csrfField = document.querySelector("#csrfField");
const loginButton = document.querySelector("#loginButton");
const loginMessage = document.querySelector("#loginMessage");

function showQueryMessage() {
    const params = new URLSearchParams(window.location.search);

    if (params.has("error")) {
        loginMessage.textContent = "E-posta adresi veya parola hatalı.";
        loginMessage.className = "form-message error";
    } else if (params.has("logout")) {
        loginMessage.textContent = "Oturum güvenli şekilde kapatıldı.";
        loginMessage.className = "form-message success";
    }
}

async function prepareLogin() {
    showQueryMessage();

    try {
        const response = await fetch("/api/auth/csrf", {
            credentials: "same-origin",
            cache: "no-store"
        });

        if (!response.ok) throw new Error();
        const csrf = await response.json();
        csrfField.name = csrf.parameterName;
        csrfField.value = csrf.token;
        loginButton.disabled = false;
    } catch {
        loginMessage.textContent = "Giriş servisine ulaşılamıyor. Uygulama loglarını kontrol edin.";
        loginMessage.className = "form-message error";
    }
}

form.addEventListener("submit", () => {
    loginButton.disabled = true;
    loginMessage.textContent = "Oturum doğrulanıyor…";
    loginMessage.className = "form-message";
});

prepareLogin();
