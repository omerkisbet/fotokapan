package com.omuiotlab.wildlifemediaservice.config;

import com.omuiotlab.wildlifemediaservice.model.AppUser;
import com.omuiotlab.wildlifemediaservice.model.Camera;
import com.omuiotlab.wildlifemediaservice.model.CameraStatus;
import com.omuiotlab.wildlifemediaservice.repository.AppUserRepository;
import com.omuiotlab.wildlifemediaservice.repository.CameraRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Order(2)
public class CameraDataInitializer implements ApplicationRunner {

    private static final String DEMO_CAMERA_CODE = "CAM-TR-001";

    private final CameraRepository cameraRepository;
    private final AppUserRepository appUserRepository;

    @Value("${app.bootstrap.customer-email}")
    private String customerEmail;

    public CameraDataInitializer(
            CameraRepository cameraRepository,
            AppUserRepository appUserRepository
    ) {
        this.cameraRepository = cameraRepository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppUser demoCustomer = appUserRepository.findByEmailIgnoreCase(customerEmail)
                .orElse(null);

        Camera camera = cameraRepository.findByCameraCodeIgnoreCase(DEMO_CAMERA_CODE)
                .orElseGet(() -> Camera.builder()
                        .cameraCode(DEMO_CAMERA_CODE)
                        .name("Demo Fotokapan")
                        .location("Samsun · Test sahası")
                        .jitsiRoomName("OMUIoTLabWildlife_CAMTR001")
                        .status(CameraStatus.ONLINE)
                        .description("Jitsi canlı gözlem prototipi için oluşturulan örnek fotokapan.")
                        .active(true)
                        .lastSeenAt(Instant.now())
                        .build());

        if (demoCustomer != null && camera.getCustomerId() == null) {
            camera.setCustomerId(demoCustomer.getId());
        }

        cameraRepository.save(camera);
    }
}
