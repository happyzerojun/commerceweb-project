package com.commerceweb.dto;

public class ErrorResponse {
    private String message;

    // 생성자
    public ErrorResponse(String message) {
        this.message = message;
    }

    // Getter
    public String getMessage() {
        return message;
    }
}