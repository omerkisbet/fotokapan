package com.omuiotlab.wildlifemediaservice.repository;

import com.omuiotlab.wildlifemediaservice.model.Camera;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CameraRepository extends MongoRepository<Camera, String> {

    Optional<Camera> findByCameraCodeIgnoreCase(String cameraCode);

    Optional<Camera> findByCameraCodeIgnoreCaseAndCustomerId(String cameraCode, String customerId);

    boolean existsByCameraCodeIgnoreCase(String cameraCode);

    List<Camera> findAllByOrderByNameAsc();

    List<Camera> findByActiveTrueOrderByNameAsc();

    List<Camera> findByCustomerIdOrderByNameAsc(String customerId);

    List<Camera> findByCustomerIdAndActiveTrueOrderByNameAsc(String customerId);
}
