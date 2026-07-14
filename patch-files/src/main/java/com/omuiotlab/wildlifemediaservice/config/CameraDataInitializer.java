package com.omuiotlab.wildlifemediaservice.config;

import com.omuiotlab.wildlifemediaservice.model.Camera;
import com.omuiotlab.wildlifemediaservice.model.CameraStatus;
import com.omuiotlab.wildlifemediaservice.repository.CameraRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CameraDataInitializer implements ApplicationRunner {

    private static final String DEMO_CAMERA_CODE = "CAM-TR-001";

    private final CameraRepository cameraRepository;

    public CameraDataInitializer(CameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cameraRepository.existsByCameraCodeIgnoreCase(DEMO_CAMERA_CODE)) {
            return;
        }

        Camera demoCamera = Camera.builder()
                .cameraCode(DEMO_CAMERA_CODE)
                .name("Demo Fotokapan")
                .location("Samsun · Test sahası")
                .jitsiRoomName("OMUIoTLabWildlife_CAMTR001")
                .status(CameraStatus.ONLINE)
                .description("Jitsi canlı gözlem prototipi için oluşturulan örnek fotokapan.")
                .active(true)
                .lastSeenAt(Instant.now())
                .build();

        cameraRepository.save(demoCamera);
    }
}
