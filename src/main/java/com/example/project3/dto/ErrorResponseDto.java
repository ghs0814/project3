package com.example.project3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ErrorResponseDto {

    // HTTP 상태 코드
    private int status;

    // 애플리케이션 오류 코드
    private String code;

    // 오류 메시지
    private String message;

    // 요청 경로
    private String path;

    // 필드별 Validation 오류
    private List<FieldViolation> errors;

    @Getter
    @AllArgsConstructor
    public static class FieldViolation {

        private String field;

        private String message;
    }
}