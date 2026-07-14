package com.omuiotlab.wildlifemediaservice.exception;

public class DuplicateCameraCodeException extends RuntimeException {

    public DuplicateCameraCodeException(String cameraCode) {
        super("Bu fotokapan kodu zaten kullanılıyor: " + cameraCode);
    }
}
