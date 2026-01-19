package com.commerceweb.dto;

/**
 * 에러 응답을 클라이언트(프론트/외부 API 호출자)에게 전달하기 위한 DTO(Data Transfer Object) 클래스.
 * DTO는 계층/시스템 사이에서 "데이터만" 운반하는 용도로 쓰는 단순 객체다.
 */
public class ErrorResponse {

    /**
     * 에러 메시지(사람이 읽을 수 있는 설명)를 담는 필드.
     * 보통 JSON 응답으로 직렬화되면 { "message": "..." } 형태로 내려가도록 사용한다.
     */
    private String message;

    /**
     * 생성자(Constructor).
     * ErrorResponse 객체를 만들 때 message 값을 강제로 넣게 해서,
     * 항상 의미 있는 에러 메시지를 가지도록 한다.
     *
     * @param message 클라이언트에 전달할 에러 메시지
     */
    public ErrorResponse(String message) {
        this.message = message;
    }

    /**
     * Getter 메서드.
     * private 필드인 message 값을 외부(예: Spring/Jackson 직렬화 과정)에서 읽을 수 있도록 제공한다.
     *
     * @return 에러 메시지
     */
    public String getMessage() {
        return message;
    }
}
