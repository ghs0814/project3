package com.example.project3.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400: 잘못된 업무 입력
    SAME_EMPLOYEE_APPROVER(
            HttpStatus.BAD_REQUEST,
            "신청자와 승인자는 동일할 수 없습니다."
    ),
    // 실제 내부 동작 원리 (개념상)
   /* public static final ErrorCode SAME_EMPLOYEE_APPROVER
    = new ErrorCode(HttpStatus.BAD_REQUEST, "신청자와 승인자는 동일할 수 없습니다.");*/
    INVALID_DATE_RANGE(
            HttpStatus.BAD_REQUEST,
            "휴가 종료일은 시작일보다 빠를 수 없습니다."
    ),

    // 404: 데이터 없음
    EMPLOYEE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "해당 직원을 찾을 수 없습니다."
    ),

    LEAVE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "해당 휴가 신청을 찾을 수 없습니다."
    ),

    // 409: 업무 상태 충돌
    OVERLAPPING_LEAVE(
            HttpStatus.CONFLICT,
            "해당 기간에 이미 신청 중이거나 승인된 휴가가 있습니다."
    ),

    APPROVER_MISMATCH(
            HttpStatus.CONFLICT,
            "지정된 승인자만 처리할 수 있습니다."
    ),

    APPLICANT_MISMATCH(
            HttpStatus.CONFLICT,
            "휴가 신청자만 취소할 수 있습니다."
    ),

    NOT_PENDING(
            HttpStatus.CONFLICT,
            "승인 대기 상태의 휴가만 처리할 수 있습니다."
    ),

    LEAVE_STATE_CHANGED(
            HttpStatus.CONFLICT,
            "휴가 상태가 변경되어 처리할 수 없습니다."
    ),

    // 요청 데이터 검증
    VALIDATION_ERROR(
            HttpStatus.BAD_REQUEST,
            "요청값 검증에 실패했습니다."
    ),

    INVALID_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "요청 파라미터가 올바르지 않습니다."
    ),

    INVALID_JSON(
            HttpStatus.BAD_REQUEST,
            "요청 본문을 읽을 수 없습니다."
    ),

    // 기타 HTTP 및 서버 오류
    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "요청한 경로를 찾을 수 없습니다."
    ),

    METHOD_NOT_ALLOWED(
            HttpStatus.METHOD_NOT_ALLOWED,
            "지원하지 않는 HTTP 메서드입니다."
    ),

    UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "지원하지 않는 요청 Content-Type입니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;

    private final String message;
}