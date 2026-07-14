const state = {
    customers: [],
    cameras: []
};

const customerForm = document.querySelector("#customerForm");
const customerList = document.querySelector("#customerList");
const cameraAssignments = document.querySelector("#cameraAssignments");
const customerMessage = document.querySelector("#customerMessage");
const assignmentMessage = document.querySelector("#assignmentMessage");
const customerItemTemplate = document.querySelector("#customerItemTemplate");
const assignmentItemTemplate = document.querySelector("#assignmentItemTemplate");

async function apiFetch(url, options = {}) {
    const securedOptions = await window.WildlifeSession.withCsrf(options);
    const response = await fetch(url, {
        credentials: "same-origin",
        cache: "no-store",
        ...securedOptions
    });

    if (response.status === 401) {
        window.location.replace("/login.html");
    }

    if (response.status === 403) {
        window.location.replace("/");
    }

    return response;
}

async function readError(response) {
    try {
        const body = await response.json();
        return body.message || body.detail || "İşlem başarısız oldu.";
    } catch {
        return "İşlem başarısız oldu.";
    }
}

function renderCustomers() {
    customerList.replaceChildren();

    if (state.customers.length === 0) {
        const empty = document.createElement("p");
        empty.className = "admin-empty";
        empty.textContent = "Henüz müşteri hesabı bulunmuyor.";
        customerList.append(empty);
        return;
    }

    state.customers.forEach(customer => {
        const fragment = customerItemTemplate.content.cloneNode(true);
        fragment.querySelector(".customer-name").textContent = customer.fullName;
        fragment.querySelector(".customer-email").textContent = customer.email;
        fragment.querySelector(".customer-id").textContent = customer.id;
        customerList.append(fragment);
    });
}

function createCustomerOptions(selectedCustomerId) {
    const fragment = document.createDocumentFragment();

    const empty = document.createElement("option");
    empty.value = "";
    empty.textContent = "Müşteri seçin";
    fragment.append(empty);

    state.customers.forEach(customer => {
        const option = document.createElement("option");
        option.value = customer.id;
        option.textContent = `${customer.fullName} · ${customer.email}`;
        option.selected = customer.id === selectedCustomerId;
        fragment.append(option);
    });

    return fragment;
}

function renderAssignments() {
    cameraAssignments.replaceChildren();

    if (state.cameras.length === 0) {
        const empty = document.createElement("p");
        empty.className = "admin-empty";
        empty.textContent = "Henüz fotokapan kaydı bulunmuyor.";
        cameraAssignments.append(empty);
        return;
    }

    state.cameras.forEach(camera => {
        const fragment = assignmentItemTemplate.content.cloneNode(true);
        const row = fragment.querySelector(".assignment-row");
        const select = fragment.querySelector(".assignment-select");
        const saveButton = fragment.querySelector(".assignment-save");

        fragment.querySelector(".assignment-code").textContent = camera.cameraCode;
        fragment.querySelector(".assignment-name").textContent = camera.name;
        fragment.querySelector(".assignment-location").textContent = camera.location || "Konum belirtilmemiş";
        select.append(createCustomerOptions(camera.customerId));

        saveButton.addEventListener("click", async () => {
            if (!select.value) {
                assignmentMessage.textContent = "Atama için bir müşteri seçin.";
                assignmentMessage.className = "form-message error";
                return;
            }

            saveButton.disabled = true;
            assignmentMessage.textContent = `${camera.cameraCode} atanıyor…`;
            assignmentMessage.className = "form-message";

            try {
                const response = await apiFetch(`/api/cameras/${encodeURIComponent(camera.cameraCode)}/customer`, {
                    method: "PATCH",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ customerId: select.value })
                });

                if (!response.ok) throw new Error(await readError(response));
                const updated = await response.json();
                camera.customerId = updated.customerId;
                row.dataset.saved = "true";
                assignmentMessage.textContent = `${camera.cameraCode} müşteri hesabına atandı.`;
                assignmentMessage.className = "form-message success";
            } catch (error) {
                assignmentMessage.textContent = error.message || "Atama kaydedilemedi.";
                assignmentMessage.className = "form-message error";
            } finally {
                saveButton.disabled = false;
            }
        });

        cameraAssignments.append(fragment);
    });
}

async function loadData() {
    const [customerResponse, cameraResponse] = await Promise.all([
        apiFetch("/api/admin/customers"),
        apiFetch("/api/cameras?activeOnly=false")
    ]);

    if (!customerResponse.ok) throw new Error(await readError(customerResponse));
    if (!cameraResponse.ok) throw new Error(await readError(cameraResponse));

    state.customers = await customerResponse.json();
    state.cameras = await cameraResponse.json();
    renderCustomers();
    renderAssignments();
}

customerForm.addEventListener("submit", async event => {
    event.preventDefault();
    const submitButton = customerForm.querySelector("button[type='submit']");
    submitButton.disabled = true;
    customerMessage.textContent = "Müşteri hesabı oluşturuluyor…";
    customerMessage.className = "form-message";

    try {
        const response = await apiFetch("/api/admin/customers", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                fullName: document.querySelector("#customerName").value.trim(),
                email: document.querySelector("#customerEmail").value.trim(),
                password: document.querySelector("#customerPassword").value
            })
        });

        if (!response.ok) throw new Error(await readError(response));
        const customer = await response.json();
        state.customers.push(customer);
        state.customers.sort((a, b) => a.fullName.localeCompare(b.fullName, "tr"));
        customerForm.reset();
        renderCustomers();
        renderAssignments();
        customerMessage.textContent = "Müşteri hesabı başarıyla oluşturuldu.";
        customerMessage.className = "form-message success";
    } catch (error) {
        customerMessage.textContent = error.message || "Müşteri hesabı oluşturulamadı.";
        customerMessage.className = "form-message error";
    } finally {
        submitButton.disabled = false;
    }
});

async function initialize() {
    const user = await window.WildlifeSession.loadCurrentUser();
    if (user.role !== "ADMIN") {
        window.location.replace("/");
        return;
    }

    await window.WildlifeSession.loadCsrf();

    try {
        await loadData();
    } catch (error) {
        assignmentMessage.textContent = error.message || "Yönetim verileri yüklenemedi.";
        assignmentMessage.className = "form-message error";
    }
}

initialize();
