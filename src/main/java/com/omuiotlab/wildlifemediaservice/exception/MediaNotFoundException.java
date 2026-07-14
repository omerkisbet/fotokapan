package com.omuiotlab.wildlifemediaservice.exception;

public class MediaNotFoundException extends RuntimeException {

    public MediaNotFoundException(String id) {
        super("Medya kaydı bulunamadı: " + id);
    }
}
