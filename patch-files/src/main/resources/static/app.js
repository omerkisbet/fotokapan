const state = {
    page: 0,
    size: 12,
    totalPages: 0,
    totalElements: 0,
    mediaType: "",
    cameraId: "",
    cameras: []
};

const gallery = document.querySelector("#gallery");
const emptyState = document.querySelector("#emptyState");
const template = document.querySelector("#mediaCardTemplate");
const pageInfo = document.querySelector("#pageInfo");
const previousPage = document.querySelector("#previousPage");
const nextPage = document.querySelector("#nextPage");
const uploadMessage = document.querySelector("#uploadMessage");
const serviceStatus = document.querySelector("#serviceStatus");
const serviceStatusCopy = serviceStatus.querySelector(".status-copy");
const fileInput = document.querySelector("#file");
const fileLabel = document.querySelector("#fileLabel");
const totalMediaCount = document.querySelector("#totalMediaCount");
const currentViewLabel = document.querySelector("#currentViewLabel");
const lastSync = document.querySelector("#lastSync");
const resultCount = document.querySelector("#resultCount");
const cameraDirectoryList = document.querySelector("#cameraDirectoryList");
const cameraDirectoryEmpty = document.querySelector("#cameraDirectoryEmpty");
const cameraRowTemplate = document.querySelector("#cameraRowTemplate");
const cameraOptions = document.querySelector("#cameraOptions");
const cameraIdInput = document.querySelector("#cameraId");
const cameraFilterInput = document.querySelector("#cameraFilter");
const introLiveLink = document.querySelector("#introLiveLink");

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "—";

    return new Intl.DateTimeFormat("tr-TR", {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(date);
}

function formatTime(value = new Date()) {
    return new Intl.DateTimeFormat("tr-TR", {
        hour: "2-digit",
        minute: "2-digit"
    }).format(value);
}

function formatSize(bytes) {
    if (!Number.isFinite(bytes) || bytes < 1) return "0 B";

    const units = ["B", "KB", "MB", "GB"];
    const index = Math.min(
        Math.floor(Math.log(bytes) / Math.log(1024)),
        units.length - 1
    );

    const value = bytes / (1024 ** index);
    return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}

function setServiceStatus(mode, text) {
    serviceStatus.className = `service-status is-${mode}`;
    serviceStatusCopy.textContent = text;
}

async function fetchWithTimeout(url, options = {}, timeoutMs = 5000) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), timeoutMs);

    try {
        return await fetch(url, {
            cache: "no-store",
            ...options,
            signal: controller.signal
        });
    } finally {
        window.clearTimeout(timeout);
    }
}

async function readError(response) {
    try {
        const body = await response.json();
        return body.message || body.detail || "İşlem başarısız oldu.";
    } catch {
        return "İşlem başarısız oldu.";
    }
}

function createPreview(media) {
    if (media.mediaType === "VIDEO") {
        const video = document.createElement("video");
        video.src = media.contentUrl;
        video.controls = true;
        video.preload = "metadata";
        video.playsInline = true;
        video.setAttribute("aria-label", media.originalFilename);
        return video;
    }

    const image = document.createElement("img");
    image.src = media.contentUrl;
    image.alt = media.description || media.originalFilename;
    image.loading = "lazy";
    image.decoding = "async";
    return image;
}

function createCard(media, position) {
    const fragment = template.content.cloneNode(true);
    const card = fragment.querySelector(".media-entry");
    const preview = fragment.querySelector(".media-preview");

    preview.prepend(createPreview(media));
    fragment.querySelector(".preview-open").href = media.contentUrl;
    fragment.querySelector(".entry-index").textContent = `KAYIT ${String(position + 1).padStart(2, "0")}`;
    fragment.querySelector(".media-title").textContent = media.originalFilename;
    fragment.querySelector(".media-badge").textContent = media.mediaType === "VIDEO" ? "VİDEO" : "FOTOĞRAF";
    fragment.querySelector(".camera-id").textContent = media.cameraId || "—";
    fragment.querySelector(".captured-at").textContent = formatDate(media.capturedAt);
    fragment.querySelector(".file-size").textContent = formatSize(media.size);
    fragment.querySelector(".description").textContent = media.description || "Bu kayıt için saha notu eklenmemiş.";
    fragment.querySelector(".download-link").href = media.downloadUrl;

    fragment.querySelector(".delete-button").addEventListener("click", async () => {
        const approved = window.confirm(`“${media.originalFilename}” kaydı ve dosyası kalıcı olarak silinsin mi?`);
        if (!approved) return;

        try {
            const response = await fetchWithTimeout(`/api/media/${media.id}`, { method: "DELETE" }, 8000);
            if (!response.ok) throw new Error(await readError(response));

            card.remove();
            await loadMedia();
        } catch (error) {
            window.alert(error.message || "Kayıt silinemedi.");
            setServiceStatus("offline", "API bağlantısı kesildi");
        }
    });

    return fragment;
}

function cameraStatusLabel(status) {
    if (status === "ONLINE") return "Çevrimiçi";
    if (status === "MAINTENANCE") return "Bakımda";
    return "Çevrimdışı";
}

function createCameraRow(camera, index) {
    const fragment = cameraRowTemplate.content.cloneNode(true);
    const row = fragment.querySelector(".camera-row");
    const archiveButton = fragment.querySelector(".camera-archive-link");

    row.dataset.status = camera.status || "OFFLINE";
    fragment.querySelector(".camera-row-index").textContent = String(index + 1).padStart(2, "0");
    fragment.querySelector(".camera-row-code").textContent = camera.cameraCode;
    fragment.querySelector(".camera-row-name").textContent = camera.name;
    fragment.querySelector(".camera-row-location").textContent = camera.location || "Konum bilgisi girilmemiş";
    fragment.querySelector(".camera-status-text").textContent = cameraStatusLabel(camera.status);
    fragment.querySelector(".camera-live-link").href = camera.liveUrl;

    archiveButton.addEventListener("click", () => {
        state.cameraId = camera.cameraCode;
        state.page = 0;
        cameraFilterInput.value = camera.cameraCode;
        window.history.replaceState({}, "", camera.archiveUrl);
        loadMedia();
        document.querySelector("#archiveTitle").scrollIntoView({ behavior: "smooth", block: "start" });
    });

    return fragment;
}

function renderCameraDirectory(cameras) {
    cameraDirectoryList.replaceChildren();
    cameraDirectoryEmpty.hidden = cameras.length !== 0;

    cameras.forEach((camera, index) => {
        cameraDirectoryList.append(createCameraRow(camera, index));
    });
}

function updateCameraInputs(cameras) {
    cameraOptions.replaceChildren();

    cameras.forEach(camera => {
        const option = document.createElement("option");
        option.value = camera.cameraCode;
        option.label = `${camera.name}${camera.location ? ` · ${camera.location}` : ""}`;
        cameraOptions.append(option);
    });

    const preferredCamera = cameras.find(camera => camera.status === "ONLINE") || cameras[0];
    if (preferredCamera) {
        introLiveLink.href = preferredCamera.liveUrl;
        if (!cameraIdInput.value) cameraIdInput.value = preferredCamera.cameraCode;
    } else {
        introLiveLink.href = "/live.html";
    }
}

async function loadCameras() {
    try {
        const response = await fetchWithTimeout("/api/cameras?activeOnly=true", {}, 8000);
        if (!response.ok) throw new Error(await readError(response));

        state.cameras = await response.json();
        renderCameraDirectory(state.cameras);
        updateCameraInputs(state.cameras);
    } catch (error) {
        state.cameras = [];
        cameraDirectoryList.replaceChildren();
        cameraDirectoryEmpty.hidden = false;
        cameraDirectoryEmpty.textContent = error.message || "Fotokapan listesi alınamadı.";
    }
}

function updateCollectionSummary(page) {
    const visibleCount = page.content.length;
    state.totalElements = Number(page.totalElements || 0);

    totalMediaCount.textContent = new Intl.NumberFormat("tr-TR").format(state.totalElements);
    resultCount.textContent = new Intl.NumberFormat("tr-TR").format(visibleCount);
    lastSync.textContent = formatTime();

    if (state.mediaType === "IMAGE") {
        currentViewLabel.textContent = "Fotoğraflar";
    } else if (state.mediaType === "VIDEO") {
        currentViewLabel.textContent = "Videolar";
    } else if (state.cameraId) {
        currentViewLabel.textContent = state.cameraId;
    } else {
        currentViewLabel.textContent = "Tüm kayıtlar";
    }

    const current = Math.min(page.number + 1, Math.max(page.totalPages, 1));
    const total = Math.max(page.totalPages, 1);
    pageInfo.textContent = `${String(current).padStart(2, "0")} / ${String(total).padStart(2, "0")}`;
}

async function loadMedia() {
    const params = new URLSearchParams({
        page: String(state.page),
        size: String(state.size)
    });

    if (state.mediaType) params.set("mediaType", state.mediaType);
    if (state.cameraId) params.set("cameraId", state.cameraId);

    gallery.replaceChildren();
    emptyState.hidden = true;

    try {
        const response = await fetchWithTimeout(`/api/media?${params.toString()}`, {}, 8000);
        if (!response.ok) throw new Error(await readError(response));

        const page = await response.json();
        state.totalPages = Number(page.totalPages || 0);

        page.content.forEach((media, index) => {
            const absolutePosition = (page.number * page.size) + index;
            gallery.append(createCard(media, absolutePosition));
        });

        emptyState.hidden = page.content.length !== 0;
        previousPage.disabled = Boolean(page.first);
        nextPage.disabled = Boolean(page.last) || page.totalPages === 0;

        updateCollectionSummary(page);
        setServiceStatus("online", "Servis ve veritabanı hazır");
    } catch (error) {
        emptyState.hidden = false;
        emptyState.querySelector("span").textContent = "Bağlantı kurulamadı";
        emptyState.querySelector("p").textContent = error.message || "Medya servisine erişilemiyor.";
        state.totalPages = 0;
        previousPage.disabled = true;
        nextPage.disabled = true;
        resultCount.textContent = "0";
        setServiceStatus("offline", "Servise ulaşılamıyor");
    }
}

fileInput.addEventListener("change", () => {
    const selectedFile = fileInput.files[0];
    fileLabel.textContent = selectedFile ? selectedFile.name : "Dosya seçin";
});

document.querySelector("#uploadForm").addEventListener("submit", async event => {
    event.preventDefault();

    const uploadButton = document.querySelector("#uploadButton");
    const file = fileInput.files[0];
    const capturedAt = document.querySelector("#capturedAt").value;

    if (!file) {
        uploadMessage.textContent = "Önce bir fotoğraf veya MP4 dosyası seçin.";
        uploadMessage.className = "form-message error";
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("cameraId", cameraIdInput.value.trim());
    formData.append("description", document.querySelector("#description").value.trim());
    if (capturedAt) formData.append("capturedAt", new Date(capturedAt).toISOString());

    uploadButton.disabled = true;
    uploadMessage.textContent = "Dosya arşive aktarılıyor…";
    uploadMessage.className = "form-message";

    try {
        const response = await fetchWithTimeout("/api/media", {
            method: "POST",
            body: formData
        }, 120000);

        if (!response.ok) throw new Error(await readError(response));

        const selectedCameraCode = cameraIdInput.value.trim();
        event.target.reset();
        cameraIdInput.value = selectedCameraCode;
        fileLabel.textContent = "Dosya seçin";
        state.page = 0;
        uploadMessage.textContent = "Medya kaydı başarıyla oluşturuldu.";
        uploadMessage.className = "form-message success";
        await loadMedia();
    } catch (error) {
        uploadMessage.textContent = error.message || "Yükleme sırasında bağlantı hatası oluştu.";
        uploadMessage.className = "form-message error";
        setServiceStatus("offline", "Yükleme servisine ulaşılamıyor");
    } finally {
        uploadButton.disabled = false;
    }
});

document.querySelector("#filterForm").addEventListener("submit", event => {
    event.preventDefault();
    state.mediaType = document.querySelector("#mediaType").value;
    state.cameraId = cameraFilterInput.value.trim();
    state.page = 0;

    const url = new URL(window.location.href);
    if (state.cameraId) url.searchParams.set("cameraCode", state.cameraId);
    else url.searchParams.delete("cameraCode");
    window.history.replaceState({}, "", url);

    loadMedia();
});

previousPage.addEventListener("click", () => {
    if (state.page > 0) {
        state.page -= 1;
        loadMedia();
        document.querySelector("#archiveTitle").scrollIntoView({ behavior: "smooth", block: "start" });
    }
});

nextPage.addEventListener("click", () => {
    if (state.page + 1 < state.totalPages) {
        state.page += 1;
        loadMedia();
        document.querySelector("#archiveTitle").scrollIntoView({ behavior: "smooth", block: "start" });
    }
});

async function checkService() {
    setServiceStatus("checking", "Bağlantı kontrol ediliyor");

    try {
        const healthResponse = await fetchWithTimeout(`/actuator/health?_=${Date.now()}`, {}, 4500);

        if (healthResponse.ok) {
            const health = await healthResponse.json();
            if (health.status === "UP") {
                setServiceStatus("online", "Servis ve veritabanı hazır");
                return;
            }
        }
    } catch {
        // Actuator erişimi başarısızsa gerçek API ile ikinci kontrol yapılır.
    }

    try {
        const apiResponse = await fetchWithTimeout(`/api/cameras?activeOnly=true&_=${Date.now()}`, {}, 5000);
        if (apiResponse.ok) {
            setServiceStatus("online", "Uygulama servisi hazır");
            return;
        }
    } catch {
        // Aşağıdaki offline durumuna düşülür.
    }

    setServiceStatus("offline", "Servise ulaşılamıyor");
}

function readInitialArchiveState() {
    const params = new URLSearchParams(window.location.search);
    const requestedCameraCode = params.get("cameraCode");
    if (!requestedCameraCode) return;

    state.cameraId = requestedCameraCode.trim();
    cameraFilterInput.value = state.cameraId;
}

async function initialize() {
    readInitialArchiveState();
    await Promise.allSettled([loadCameras(), checkService()]);
    await loadMedia();
}

initialize();
window.setInterval(checkService, 30000);
