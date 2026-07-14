package com.omuiotlab.wildlifemediaservice.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Bu e-posta adresi zaten kayıtlı: " + email);
    }
}
