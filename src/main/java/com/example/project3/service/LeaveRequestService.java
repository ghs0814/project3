package com.example.project3.service;

import com.example.project3.domain.ApprovalHistory;
import com.example.project3.domain.LeaveRequest;

import com.example.project3.dto.ApprovalHistoryResponseDto;
import com.example.project3.dto.LeaveApproveRequestDto;
import com.example.project3.dto.LeaveCancelRequestDto;
import com.example.project3.dto.LeaveCreateRequestDto;
import com.example.project3.dto.LeaveDetailResponseDto;
import com.example.project3.dto.LeavePageResponseDto;
import com.example.project3.dto.LeaveRejectRequestDto;
import com.example.project3.dto.LeaveResponseDto;
import com.example.project3.dto.LeaveSearchConditionDto;

import com.example.project3.exception.BusinessException;
import com.example.project3.exception.ErrorCode;

import com.example.project3.mapper.ApprovalHistoryMapper;
import com.example.project3.mapper.EmployeeMapper;
import com.example.project3.mapper.LeaveRequestMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestMapper leaveRequestMapper;

    private final EmployeeMapper employeeMapper;

    private final ApprovalHistoryMapper approvalHistoryMapper;

    // 1. 휴가 신청 등록
    @Transactional
    public LeaveResponseDto create(LeaveCreateRequestDto request) {

        // 신청자와 승인자가 동일한지 확인
        if (request.getEmployeeId().equals(request.getApproverId())) {
            throw new BusinessException(
                    ErrorCode.SAME_EMPLOYEE_APPROVER
            );
        }

        // 휴가 날짜 순서 검증
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(
                    ErrorCode.INVALID_DATE_RANGE
            );
        }

        // 신청 직원 존재 여부
        if (employeeMapper.findById(request.getEmployeeId()) == null) {
            throw new BusinessException(
                    ErrorCode.EMPLOYEE_NOT_FOUND
            );
        }

        // 승인 직원 존재 여부
        if (employeeMapper.findById(request.getApproverId()) == null) {
            throw new BusinessException(
                    ErrorCode.EMPLOYEE_NOT_FOUND
            );
        }

        // 중복 휴가 기간 검증
        boolean overlapping = leaveRequestMapper.existsOverlapping(
                request.getEmployeeId(),
                request.getStartDate(),
                request.getEndDate()
        );

        if (overlapping) {
            throw new BusinessException(
                    ErrorCode.OVERLAPPING_LEAVE
            );
        }

        // Domain 객체 생성
        LeaveRequest leaveRequest = new LeaveRequest();

        leaveRequest.setEmployeeId(request.getEmployeeId());
        leaveRequest.setApproverId(request.getApproverId());
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason().trim());

        // INSERT
        int insertedRows;

        try {
            insertedRows = leaveRequestMapper.insert(leaveRequest);

        } catch (DataIntegrityViolationException e) {

            // PostgreSQL EXCLUDE 제약조건 위반
            if (isExclusionViolation(e)) {
                throw new BusinessException(
                        ErrorCode.OVERLAPPING_LEAVE,
                        e
                );
            }

            throw e;
        }

        if (insertedRows != 1) {
            throw new IllegalStateException(
                    "휴가 신청 등록에 실패했습니다."
            );
        }

        // 자동 생성 PK 확인
        Long requestId = leaveRequest.getRequestId();

        if (requestId == null) {
            throw new IllegalStateException(
                    "휴가 신청 ID를 가져오지 못했습니다."
            );
        }

        // 저장된 휴가 신청 재조회
        LeaveRequest savedLeaveRequest =
                leaveRequestMapper.findById(requestId);

        if (savedLeaveRequest == null) {
            throw new IllegalStateException(
                    "저장된 휴가 신청을 조회하지 못했습니다."
            );
        }

        return LeaveResponseDto.from(savedLeaveRequest);
    }

    // 2. 휴가 검색 + 페이징
    @Transactional(readOnly = true)
    public LeavePageResponseDto findAll(
            LeaveSearchConditionDto condition
    ) {

        // 검색 결과 전체 건수
        long totalElements =
                leaveRequestMapper.countByCondition(condition);

        // 현재 페이지 목록
        List<LeaveDetailResponseDto> content =
                leaveRequestMapper.findAllWithDetails(condition);

        // 전체 페이지 수 계산
        int size = condition.getSize();

        long totalPages = totalElements / size;

        if (totalElements % size != 0) {
            totalPages++;
        }

        return new LeavePageResponseDto(
                content,
                condition.getPage(),
                size,
                totalElements,
                totalPages
        );
    }

    // 3. 휴가 상세 조회
    @Transactional(readOnly = true)
    public LeaveDetailResponseDto findById(Long requestId) {

        LeaveDetailResponseDto leave =
                leaveRequestMapper.findDetailById(requestId);

        if (leave == null) {
            throw new BusinessException(
                    ErrorCode.LEAVE_NOT_FOUND
            );
        }

        return leave;
    }

    // 4. 휴가 승인 및 이력 저장
    @Transactional
    public LeaveResponseDto approve(
            Long requestId,
            LeaveApproveRequestDto request
    ) {

        // 휴가 신청 조회
        LeaveRequest leave = leaveRequestMapper.findById(requestId);

        if (leave == null) {
            throw new BusinessException(
                    ErrorCode.LEAVE_NOT_FOUND
            );
        }

        // 지정된 승인자 확인
        if (!leave.getApproverId().equals(request.getApproverId())) {
            throw new BusinessException(
                    ErrorCode.APPROVER_MISMATCH
            );
        }

        // 승인 대기 상태 확인
        if (!"PENDING".equals(leave.getStatus())) {
            throw new BusinessException(
                    ErrorCode.NOT_PENDING
            );
        }

        // 조건부 UPDATE
        int updatedRows = leaveRequestMapper.approve(
                requestId,
                request.getApproverId()
        );

        if (updatedRows != 1) {
            throw new BusinessException(
                    ErrorCode.LEAVE_STATE_CHANGED
            );
        }

        // 승인 이력 저장
        saveHistory(
                requestId,
                request.getApproverId(),
                "APPROVED",
                null
        );

        // 변경된 데이터 재조회
        LeaveRequest approvedLeave =
                leaveRequestMapper.findById(requestId);

        if (approvedLeave == null) {
            throw new IllegalStateException(
                    "승인된 휴가 신청을 조회하지 못했습니다."
            );
        }

        return LeaveResponseDto.from(approvedLeave);
    }

    // 5. 휴가 반려 및 이력 저장
    @Transactional
    public LeaveResponseDto reject(
            Long requestId,
            LeaveRejectRequestDto request
    ) {

        // 휴가 신청 조회
        LeaveRequest leave = leaveRequestMapper.findById(requestId);

        if (leave == null) {
            throw new BusinessException(
                    ErrorCode.LEAVE_NOT_FOUND
            );
        }

        // 지정된 승인자 확인
        if (!leave.getApproverId().equals(request.getApproverId())) {
            throw new BusinessException(
                    ErrorCode.APPROVER_MISMATCH
            );
        }

        // 승인 대기 상태 확인
        if (!"PENDING".equals(leave.getStatus())) {
            throw new BusinessException(
                    ErrorCode.NOT_PENDING
            );
        }

        // 반려 사유
        String comment = request.getComment().trim();

        // 조건부 UPDATE
        int updatedRows = leaveRequestMapper.reject(
                requestId,
                request.getApproverId(),
                comment
        );

        if (updatedRows != 1) {
            throw new BusinessException(
                    ErrorCode.LEAVE_STATE_CHANGED
            );
        }

        // 반려 이력 저장
        saveHistory(
                requestId,
                request.getApproverId(),
                "REJECTED",
                comment
        );

        // 변경된 데이터 재조회
        LeaveRequest rejectedLeave =
                leaveRequestMapper.findById(requestId);

        if (rejectedLeave == null) {
            throw new IllegalStateException(
                    "반려된 휴가 신청을 조회하지 못했습니다."
            );
        }

        return LeaveResponseDto.from(rejectedLeave);
    }

    // 6. 휴가 취소 및 이력 저장
    @Transactional
    public LeaveResponseDto cancel(
            Long requestId,
            LeaveCancelRequestDto request
    ) {

        // 휴가 신청 조회
        LeaveRequest leave = leaveRequestMapper.findById(requestId);

        if (leave == null) {
            throw new BusinessException(
                    ErrorCode.LEAVE_NOT_FOUND
            );
        }

        // 신청자 본인인지 확인
        if (!leave.getEmployeeId().equals(request.getEmployeeId())) {
            throw new BusinessException(
                    ErrorCode.APPLICANT_MISMATCH
            );
        }

        // 승인 대기 상태 확인
        if (!"PENDING".equals(leave.getStatus())) {
            throw new BusinessException(
                    ErrorCode.NOT_PENDING
            );
        }

        // 조건부 UPDATE
        int updatedRows = leaveRequestMapper.cancel(
                requestId,
                request.getEmployeeId()
        );

        if (updatedRows != 1) {
            throw new BusinessException(
                    ErrorCode.LEAVE_STATE_CHANGED
            );
        }

        // 취소 이력 저장
        saveHistory(
                requestId,
                request.getEmployeeId(),
                "CANCELED",
                null
        );

        // 변경된 데이터 재조회
        LeaveRequest canceledLeave =
                leaveRequestMapper.findById(requestId);

        if (canceledLeave == null) {
            throw new IllegalStateException(
                    "취소된 휴가 신청을 조회하지 못했습니다."
            );
        }

        return LeaveResponseDto.from(canceledLeave);
    }

    // 7. 휴가 처리 이력 조회
    @Transactional(readOnly = true)
    public List<ApprovalHistoryResponseDto> findHistoryByRequestId(
            Long requestId
    ) {

        LeaveRequest leave = leaveRequestMapper.findById(requestId);

        if (leave == null) {
            throw new BusinessException(
                    ErrorCode.LEAVE_NOT_FOUND
            );
        }

        return approvalHistoryMapper.findByRequestId(requestId);
    }

    // 8. 처리 이력 저장 공통 메서드
    private void saveHistory(
            Long requestId,
            Long actorEmployeeId,
            String action,
            String comment
    ) {

        ApprovalHistory history = new ApprovalHistory();

        history.setRequestId(requestId);
        history.setActorEmployeeId(actorEmployeeId);
        history.setAction(action);
        history.setComment(comment);

        int insertedRows = approvalHistoryMapper.insert(history);

        if (insertedRows != 1) {
            throw new IllegalStateException(
                    "처리 이력 저장에 실패했습니다."
            );
        }

        if (history.getHistoryId() == null) {
            throw new IllegalStateException(
                    "처리 이력 ID를 가져오지 못했습니다."
            );
        }
    }

    // 9. PostgreSQL EXCLUDE 제약조건 위반 확인
    private boolean isExclusionViolation(Throwable throwable) {

        Throwable cause = throwable;

        while (cause != null) {

            if (cause instanceof SQLException sqlException) {

                if ("23P01".equals(sqlException.getSQLState())) {
                    return true;
                }
            }

            cause = cause.getCause();
        }

        return false;
    }
}