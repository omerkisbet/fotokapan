package com.omuiotlab.wildlifemediaservice.exception;

public class CameraNotFoundException extends RuntimeException {

    public CameraNotFoundException(String cameraCode) {
        super("Fotokapan bulunamadı: " + cameraCode);
    }
}
