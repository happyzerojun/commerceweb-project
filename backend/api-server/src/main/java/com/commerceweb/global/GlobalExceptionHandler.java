package com.commerceweb.global;

import com.commerceweb.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 서비스 계층 예외를 의미에 맞는 HTTP 상태로 바꾼다.
 * 이전에는 핸들러가 없어 "본인 주문 아님", "별점 범위 오류" 같은 클라이언트 잘못도 전부 500으로 나갔다.
 * Exception 전체를 잡지 않는 이유: 404·415 같은 Spring MVC 기본 처리까지 500으로 바뀌는 것을 막기 위해.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    /**
     * 서비스에서 의미 있는 예외로 바꾸지 못한 DB 제약 위반(동시 요청의 유니크 충돌 등)의 안전망.
     * 500 대신 409로 응답하고, 내부 SQL 메시지는 노출하지 않는다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> dataConflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("요청이 다른 데이터와 충돌합니다. 다시 시도해 주세요."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }
}
