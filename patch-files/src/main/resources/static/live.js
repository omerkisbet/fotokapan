const JITSI_DOMAIN = "meet.jit.si";

const cameraSelect = document.querySelector("#liveCamera");
const startButton = document.querySelector("#startLiveButton");
const leaveButton = document.querySelector("#leaveLiveButton");
const openJitsiTab = document.querySelector("#openJitsiTab");
const stage = document.querySelector("#jitsiStage");
const placeholder = document.querySelector("#jitsiPlaceholder");
const message = document.querySelector("#liveMessage");
const connectionStatus = document.querySelector("#liveConnectionStatus");
const connectionStatusCopy = connectionStatus.querySelector(".status-copy");
const cameraLabel = document.querySelector("#liveCameraLabel");
const modeLabel = document.querySelector("#liveModeLabel");
const participantCount = document.querySelector("#participantCount");
const modeButtons = [...document.querySelectorAll(".mode-button")];

let selectedMode = "viewer";
let selectedCamera = null;
let cameras = [];
let jitsiApi = null;
let participants = new Set();
let scriptPromise = null;

function setConnectionStatus(mode, text) {
    connectionStatus.className = `service-status is-${mode}`;
    connectionStatusCopy.textContent = text;
}

function setMessage(text, type = "") {
    message.textContent = text;
    message.className = `live-message${type ? ` ${type}` : ""}`;
}

async function fetchWithTimeout(url, options = {}, timeoutMs = 8000) {
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

function getRoomName() {
    return selectedCamera?.jitsiRoomName || "";
}

function getRoomUrl() {
    const roomName = getRoomName();
    return roomName ? `https://${JITSI_DOMAIN}/${encodeURIComponent(roomName)}` : "#";
}

function updateParticipantCount() {
    participantCount.textContent = String(participants.size);
}

function updateRoomLinks() {
    cameraLabel.textContent = selectedCamera
        ? `${selectedCamera.cameraCode} · ${selectedCamera.name}`
        : "Fotokapan seçilmedi";

    modeLabel.textContent = selectedMode === "publisher" ? "Yayıncı modu" : "İzleyici modu";
    openJitsiTab.href = getRoomUrl();
    startButton.disabled = !selectedCamera;
}

function selectMode(mode) {
    selectedMode = mode;
    modeButtons.forEach(button => {
        const selected = button.dataset.mode === mode;
        button.classList.toggle("is-selected", selected);
        button.setAttribute("aria-pressed", String(selected));
    });
    updateRoomLinks();
}

function selectCamera(cameraCode) {
    selectedCamera = cameras.find(camera => camera.cameraCode === cameraCode) || null;
    if (selectedCamera) cameraSelect.value = selectedCamera.cameraCode;
    updateRoomLinks();
}

function populateCameraSelect(requestedCameraCode) {
    cameraSelect.replaceChildren();

    if (cameras.length === 0) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = "Aktif fotokapan bulunamadı";
        cameraSelect.append(option);
        cameraSelect.disabled = true;
        selectCamera("");
        return;
    }

    cameras.forEach(camera => {
        const option = document.createElement("option");
        option.value = camera.cameraCode;
        option.textContent = `${camera.cameraCode} · ${camera.name} · ${statusLabel(camera.status)}`;
        cameraSelect.append(option);
    });

    cameraSelect.disabled = false;
    const requestedExists = cameras.some(camera => camera.cameraCode === requestedCameraCode);
    const onlineCamera = cameras.find(camera => camera.status === "ONLINE");
    const initialCamera = requestedExists
        ? requestedCameraCode
        : (onlineCamera || cameras[0]).cameraCode;

    selectCamera(initialCamera);
}

function statusLabel(status) {
    if (status === "ONLINE") return "Çevrimiçi";
    if (status === "MAINTENANCE") return "Bakımda";
    return "Çevrimdışı";
}

async function loadCameras(requestedCameraCode) {
    setConnectionStatus("checking", "Fotokapanlar yükleniyor");

    try {
        const response = await fetchWithTimeout("/api/cameras?activeOnly=true");
        if (!response.ok) throw new Error(await readError(response));

        cameras = await response.json();
        populateCameraSelect(requestedCameraCode);

        if (selectedCamera) {
            setConnectionStatus(
                selectedCamera.status === "ONLINE" ? "online" : "checking",
                `${selectedCamera.cameraCode} · ${statusLabel(selectedCamera.status)}`
            );
            setMessage("Fotokapan bilgileri MongoDB üzerinden yüklendi.", "success");
        } else {
            setConnectionStatus("offline", "Aktif fotokapan yok");
            setMessage("Canlı bağlantı için önce /api/cameras üzerinden bir fotokapan oluşturun.", "error");
        }
    } catch (error) {
        cameras = [];
        populateCameraSelect("");
        setConnectionStatus("offline", "Fotokapan servisine ulaşılamıyor");
        setMessage(error.message || "Fotokapan listesi alınamadı.", "error");
    }
}

function loadJitsiScript() {
    if (window.JitsiMeetExternalAPI) return Promise.resolve();
    if (scriptPromise) return scriptPromise;

    scriptPromise = new Promise((resolve, reject) => {
        const script = document.createElement("script");
        script.src = `https://${JITSI_DOMAIN}/external_api.js`;
        script.async = true;
        script.onload = resolve;
        script.onerror = () => reject(new Error("Jitsi API dosyası yüklenemedi. İnternet bağlantısını kontrol edin."));
        document.head.append(script);
    });

    return scriptPromise;
}

function disposeMeeting() {
    if (jitsiApi) {
        jitsiApi.dispose();
        jitsiApi = null;
    }

    participants = new Set();
    updateParticipantCount();
    stage.replaceChildren(placeholder);
    placeholder.hidden = false;
    leaveButton.disabled = true;
    cameraSelect.disabled = cameras.length === 0;
    startButton.disabled = !selectedCamera;

    if (selectedCamera) {
        setConnectionStatus(
            selectedCamera.status === "ONLINE" ? "online" : "checking",
            `${selectedCamera.cameraCode} · ${statusLabel(selectedCamera.status)}`
        );
    } else {
        setConnectionStatus("offline", "Aktif fotokapan yok");
    }
}

function createJitsiOptions() {
    const isPublisher = selectedMode === "publisher";

    return {
        roomName: getRoomName(),
        parentNode: stage,
        width: "100%",
        height: "100%",
        lang: "tr",
        userInfo: {
            displayName: isPublisher
                ? `${selectedCamera.cameraCode} · Fotokapan Yayını`
                : `${selectedCamera.cameraCode} · Müşteri İzleyici`
        },
        configOverwrite: {
            startWithAudioMuted: true,
            startWithVideoMuted: !isPublisher,
            disableSelfView: !isPublisher,
            resolution: 720,
            toolbarButtons: isPublisher
                ? ["microphone", "camera", "settings", "fullscreen", "hangup"]
                : ["fullscreen", "tileview", "hangup"]
        },
        interfaceConfigOverwrite: {
            MOBILE_APP_PROMO: false,
            SHOW_JITSI_WATERMARK: false,
            SHOW_WATERMARK_FOR_GUESTS: false
        }
    };
}

function registerMeetingEvents(api) {
    api.addListener("videoConferenceJoined", event => {
        participants.add(event.id || "local");
        updateParticipantCount();
        setConnectionStatus("online", selectedMode === "publisher" ? "Kamera yayında" : "Canlı yayına bağlı");
        setMessage(
            selectedMode === "publisher"
                ? "Yayıncı bağlantısı kuruldu. Kamera izni verdiğinizde müşteriler görüntüyü izleyebilir."
                : "İzleyici bağlantısı kuruldu. Yayıncı odaya katıldığında canlı görüntü başlayacaktır.",
            "success"
        );
    });

    api.addListener("participantJoined", event => {
        participants.add(event.id);
        updateParticipantCount();
    });

    api.addListener("participantLeft", event => {
        participants.delete(event.id);
        updateParticipantCount();
    });

    api.addListener("cameraError", event => {
        setConnectionStatus("offline", "Kamera erişim hatası");
        setMessage(event.message || "Tarayıcı kamera erişimine izin vermedi.", "error");
    });

    api.addListener("browserSupport", event => {
        if (!event.supported) {
            setConnectionStatus("offline", "Tarayıcı desteklenmiyor");
            setMessage("Bu tarayıcı Jitsi WebRTC bağlantısını desteklemiyor.", "error");
        }
    });

    api.addListener("videoConferenceLeft", () => {
        disposeMeeting();
        setMessage("Canlı bağlantı sonlandırıldı.");
    });

    api.addListener("readyToClose", disposeMeeting);
}

async function startMeeting() {
    if (jitsiApi || !selectedCamera) return;

    startButton.disabled = true;
    cameraSelect.disabled = true;
    setConnectionStatus("checking", "Jitsi bağlantısı kuruluyor");
    setMessage("Canlı yayın bileşeni yükleniyor…");

    try {
        await loadJitsiScript();
        placeholder.hidden = true;
        stage.replaceChildren();
        jitsiApi = new window.JitsiMeetExternalAPI(JITSI_DOMAIN, createJitsiOptions());
        registerMeetingEvents(jitsiApi);
        leaveButton.disabled = false;
    } catch (error) {
        disposeMeeting();
        setConnectionStatus("offline", "Jitsi servisine ulaşılamıyor");
        setMessage(error.message || "Canlı bağlantı başlatılamadı.", "error");
    }
}

modeButtons.forEach(button => {
    button.addEventListener("click", () => {
        if (jitsiApi) {
            const confirmed = window.confirm("Mod değiştirmek mevcut canlı bağlantıyı sonlandırır. Devam edilsin mi?");
            if (!confirmed) return;
            disposeMeeting();
        }
        selectMode(button.dataset.mode);
    });
});

cameraSelect.addEventListener("change", () => {
    if (jitsiApi) disposeMeeting();
    selectCamera(cameraSelect.value);

    const url = new URL(window.location.href);
    url.searchParams.set("camera", cameraSelect.value);
    url.searchParams.set("mode", selectedMode);
    window.history.replaceState({}, "", url);
});

startButton.addEventListener("click", startMeeting);
leaveButton.addEventListener("click", disposeMeeting);
window.addEventListener("beforeunload", () => jitsiApi?.dispose());

async function initialize() {
    const params = new URLSearchParams(window.location.search);
    const requestedCamera = params.get("camera") || "";
    const requestedMode = params.get("mode");

    selectMode(requestedMode === "publisher" ? "publisher" : "viewer");
    await loadCameras(requestedCamera);
    updateRoomLinks();
}

initialize();
