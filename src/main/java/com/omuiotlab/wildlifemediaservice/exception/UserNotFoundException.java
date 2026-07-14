package com.omuiotlab.wildlifemediaservice.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String identifier) {
        super("Kullanıcı bulunamadı: " + identifier);
    }
}
