package com.example.project3.exception;

import com.example.project3.dto.ErrorResponseDto;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 사용자 정의 업무 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(
            BusinessException e,
            HttpServletRequest request
    ) {

        return buildResponse(
                e.getErrorCode(),
                request,
                List.of()
        );
    }
    //businessExcepion 커스텀 예외는 errorcode enum 객체를 가지도록 설정하고
    //이 예외가 발생하면, 그 객체를 buildResponse() 메서드로 넘겨서
    // 해당메서드에서, 그 에러코드 enum 객체의 정보를 가지고 return responseEntity
    //특정 예외 설정 -> 예외 호출 -> errorcode정보가지고 return

    // 2. DTO Validation 오류
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {

        List<ErrorResponseDto.FieldViolation> errors =
                e.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> new ErrorResponseDto.FieldViolation(
                                error.getField(),
                                error.getDefaultMessage()
                        ))
                        .toList();

        return buildResponse(
                ErrorCode.VALIDATION_ERROR,
                request,
                errors
        );
    }

    // 3. 메서드 파라미터 Validation 오류
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodValidation(
            HandlerMethodValidationException e,
            HttpServletRequest request
    ) {

        return buildResponse(
                ErrorCode.VALIDATION_ERROR,
                request,
                List.of()
        );
    }

    // 4. URL 파라미터 타입 오류
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {

        ErrorResponseDto.FieldViolation error =
                new ErrorResponseDto.FieldViolation(
                        e.getName(),
                        "요청한 값의 형식이 올바르지 않습니다."
                );

        return buildResponse(
                ErrorCode.INVALID_PARAMETER,
                request,
                List.of(error)
        );
    }

    // 5. JSON 본문 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidJson(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {

        return buildResponse(
                ErrorCode.INVALID_JSON,
                request,
                List.of()
        );
    }

    // 6. 존재하지 않는 경로
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResource(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {

        return buildResponse(
                ErrorCode.RESOURCE_NOT_FOUND,
                request,
                List.of()
        );
    }

    // 7. 지원하지 않는 HTTP 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {

        return buildResponse(
                ErrorCode.METHOD_NOT_ALLOWED,
                request,
                List.of()
        );
    }

    // 8. 지원하지 않는 Content-Type
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMediaType(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {

        return buildResponse(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                request,
                List.of()
        );
    }

    // 9. 기존 ResponseStatusException 호환
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatus(
            ResponseStatusException e,
            HttpServletRequest request
    ) {

        String message = e.getReason() != null
                ? e.getReason()
                : "HTTP 요청을 처리할 수 없습니다.";

        ErrorResponseDto body = new ErrorResponseDto(
                e.getStatusCode().value(),
                "HTTP_ERROR",
                message,
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(e.getStatusCode())
                .body(body);
    }

    // 10. 예상하지 못한 서버 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpectedException(
            Exception e,
            HttpServletRequest request
    ) {

        log.error(
                "예상하지 못한 서버 오류. path={}",
                request.getRequestURI(),
                e
        );

        return buildResponse(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request,
                List.of()
        );
    }

    // 11. 공통 응답 생성 메서드
    private ResponseEntity<ErrorResponseDto> buildResponse(
            ErrorCode errorCode,
            HttpServletRequest request,
            List<ErrorResponseDto.FieldViolation> errors
    ) {

        ErrorResponseDto body = new ErrorResponseDto(
                errorCode.getStatus().value(),
                errorCode.name(),
                errorCode.getMessage(),
                request.getRequestURI(),
                errors
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
    }
}