package com.commerceweb.dto; // DTO(Data Transfer Object) 클래스들이 위치한 패키지 선언

public class LoginRequest { // 로그인 요청에 필요한 데이터를 담는 DTO 클래스
    private String email;    // 사용자가 입력한 이메일(로그인 아이디 역할)
    private String password; // 사용자가 입력한 비밀번호

    // 기본 생성자(매개변수 없음) - Jackson 같은 역직렬화 라이브러리에서 필요할 수 있음
    public LoginRequest() {}

    // 전체 생성자 - email과 password를 한 번에 주입해서 객체 생성할 때 사용
    public LoginRequest(String email, String password) {
        this.email = email;         // 전달받은 email 값을 필드에 저장
        this.password = password;   // 전달받은 password 값을 필드에 저장
    }

    // Getter - email 값 조회
    public String getEmail() {
        return email; // 현재 객체의 email 필드를 반환
    }

    // Setter - email 값 설정(변경)
    public void setEmail(String email) {
        this.email = email; // 전달받은 email 값을 현재 객체의 email 필드에 저장
    }

    // Getter - password 값 조회
    public String getPassword() {
        return password; // 현재 객체의 password 필드를 반환
    }

    // Setter - password 값 설정(변경)
    public void setPassword(String password) {
        this.password = password; // 전달받은 password 값을 현재 객체의 password 필드에 저장
    }
}
