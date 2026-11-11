package com.example.project3.service;

import com.example.project3.domain.ApprovalHistory;
import com.example.project3.domain.Employee;
import com.example.project3.domain.LeaveRequest;

import com.example.project3.dto.LeaveApproveRequestDto;
import com.example.project3.dto.LeaveCancelRequestDto;
import com.example.project3.dto.LeaveCreateRequestDto;
import com.example.project3.dto.LeavePageResponseDto;
import com.example.project3.dto.LeaveRejectRequestDto;
import com.example.project3.dto.LeaveResponseDto;
import com.example.project3.dto.LeaveSearchConditionDto;

import com.example.project3.exception.BusinessException;
import com.example.project3.exception.ErrorCode;

import com.example.project3.mapper.ApprovalHistoryMapper;
import com.example.project3.mapper.EmployeeMapper;
import com.example.project3.mapper.LeaveRequestMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private ApprovalHistoryMapper approvalHistoryMapper;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    // 1. 정상 휴가 신청
    @Test
    void shouldCreateLeaveRequest() {

        // given
        LeaveCreateRequestDto request = createRequest();

        LeaveRequest savedLeave = leave("PENDING");

        when(employeeMapper.findById(anyLong()))
                .thenReturn(new Employee());

        when(leaveRequestMapper.insert(any(LeaveRequest.class)))
                .thenAnswer(invocation -> {

                    LeaveRequest entity = invocation.getArgument(0);
                    entity.setRequestId(10L);

                    return 1;
                });

        when(leaveRequestMapper.findById(10L))
                .thenReturn(savedLeave);

        // when
        LeaveResponseDto response =
                leaveRequestService.create(request);

        // then
        assertThat(response.getRequestId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo("PENDING");

        verify(leaveRequestMapper)
                .insert(any(LeaveRequest.class));
    }

    // 2. 잘못된 날짜 순서
    @Test
    void shouldRejectInvalidDateRange() {

        // given
        LeaveCreateRequestDto request = createRequest();

        request.setStartDate(LocalDate.of(2027, 5, 20));
        request.setEndDate(LocalDate.of(2027, 5, 10));

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> leaveRequestService.create(request)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);

        verify(leaveRequestMapper, never())
                .insert(any(LeaveRequest.class));
    }

    // 3. 휴가 기간 중복
    @Test
    void shouldRejectOverlappingLeaveRequest() {

        // given
        LeaveCreateRequestDto request = createRequest();

        when(employeeMapper.findById(anyLong()))
                .thenReturn(new Employee());

        when(leaveRequestMapper.existsOverlapping(
                request.getEmployeeId(),
                request.getStartDate(),
                request.getEndDate()
        )).thenReturn(true);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> leaveRequestService.create(request)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.OVERLAPPING_LEAVE);

        verify(leaveRequestMapper, never())
                .insert(any(LeaveRequest.class));
    }

    // 4. 정상 승인 및 승인 이력 저장
    @Test
    void shouldApprovePendingLeaveAndSaveHistory() {

        // given
        LeaveApproveRequestDto request =
                new LeaveApproveRequestDto();

        request.setApproverId(4L);

        LeaveRequest pendingLeave = leave("PENDING");
        LeaveRequest approvedLeave = leave("APPROVED");

        approvedLeave.setDecidedAt(
                LocalDateTime.of(2027, 5, 1, 10, 0)
        );

        when(leaveRequestMapper.findById(10L))
                .thenReturn(pendingLeave, approvedLeave);

        when(leaveRequestMapper.approve(10L, 4L))
                .thenReturn(1);

        mockSuccessfulHistoryInsert();

        // when
        LeaveResponseDto response =
                leaveRequestService.approve(10L, request);

        // then
        assertThat(response.getStatus())
                .isEqualTo("APPROVED");

        verify(leaveRequestMapper).approve(10L, 4L);

        ArgumentCaptor<ApprovalHistory> captor =
                ArgumentCaptor.forClass(ApprovalHistory.class);

        verify(approvalHistoryMapper).insert(captor.capture());

        ApprovalHistory history = captor.getValue();

        assertThat(history.getRequestId()).isEqualTo(10L);
        assertThat(history.getActorEmployeeId()).isEqualTo(4L);
        assertThat(history.getAction()).isEqualTo("APPROVED");
        assertThat(history.getComment()).isNull();
    }

    // 5. 승인 대기 상태가 아닌 신청
    @Test
    void shouldRejectApprovalWhenLeaveIsNotPending() {

        // given
        LeaveApproveRequestDto request =
                new LeaveApproveRequestDto();

        request.setApproverId(4L);

        when(leaveRequestMapper.findById(10L))
                .thenReturn(leave("APPROVED"));

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> leaveRequestService.approve(10L, request)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.NOT_PENDING);

        verify(leaveRequestMapper, never())
                .approve(anyLong(), anyLong());

        verify(approvalHistoryMapper, never())
                .insert(any(ApprovalHistory.class));
    }

    // 6. 지정된 승인자가 아닌 경우
    @Test
    void shouldRejectApprovalByWrongApprover() {

        // given
        LeaveApproveRequestDto request =
                new LeaveApproveRequestDto();

        request.setApproverId(5L);

        when(leaveRequestMapper.findById(10L))
                .thenReturn(leave("PENDING"));

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> leaveRequestService.approve(10L, request)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.APPROVER_MISMATCH);

        verify(approvalHistoryMapper, never())
                .insert(any(ApprovalHistory.class));
    }

    // 7. 조건부 UPDATE 실패
    @Test
    void shouldRejectApprovalWhenStateChanges() {

        // given
        LeaveApproveRequestDto request =
                new LeaveApproveRequestDto();

        request.setApproverId(4L);

        when(leaveRequestMapper.findById(10L))
                .thenReturn(leave("PENDING"));

        when(leaveRequestMapper.approve(10L, 4L))
                .thenReturn(0);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> leaveRequestService.approve(10L, request)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.LEAVE_STATE_CHANGED);

        verify(approvalHistoryMapper, never())
                .insert(any(ApprovalHistory.class));
    }

    // 8. 정상 반려 및 반려 사유 저장
    @Test
    void shouldRejectPendingLeaveAndSaveComment() {

        // given
        LeaveRejectRequestDto request =
                new LeaveRejectRequestDto();

        request.setApproverId(4L);
        request.setComment("  업무 일정 중복  ");

        LeaveRequest rejectedLeave = leave("REJECTED");
        rejectedLeave.setRejectionComment("업무 일정 중복");

        when(leaveRequestMapper.findById(10L))
                .thenReturn(leave("PENDING"), rejectedLeave);

        when(leaveRequestMapper.reject(
                10L, 4L, "업무 일정 중복"
        )).thenReturn(1);

        mockSuccessfulHistoryInsert();

        // when
        LeaveResponseDto response =
                leaveRequestService.reject(10L, request);

        // then
        assertThat(response.getStatus())
                .isEqualTo("REJECTED");

        assertThat(response.getRejectionComment())
                .isEqualTo("업무 일정 중복");

        ArgumentCaptor<ApprovalHistory> captor =
                ArgumentCaptor.forClass(ApprovalHistory.class);

        verify(approvalHistoryMapper).insert(captor.capture());

        assertThat(captor.getValue().getAction())
                .isEqualTo("REJECTED");

        assertThat(captor.getValue().getComment())
                .isEqualTo("업무 일정 중복");
    }

    // 9. 정상 취소 및 신청자 이력 저장
    @Test
    void shouldCancelPendingLeaveAndSaveHistory() {

        // given
        LeaveCancelRequestDto request =
                new LeaveCancelRequestDto();

        request.setEmployeeId(1L);

        when(leaveRequestMapper.findById(10L))
                .thenReturn(
                        leave("PENDING"),
                        leave("CANCELED")
                );

        when(leaveRequestMapper.cancel(10L, 1L))
                .thenReturn(1);

        mockSuccessfulHistoryInsert();

        // when
        LeaveResponseDto response =
                leaveRequestService.cancel(10L, request);

        // then
        assertThat(response.getStatus())
                .isEqualTo("CANCELED");

        assertThat(response.getDecidedAt()).isNull();

        ArgumentCaptor<ApprovalHistory> captor =
                ArgumentCaptor.forClass(ApprovalHistory.class);

        verify(approvalHistoryMapper).insert(captor.capture());

        assertThat(captor.getValue().getAction())
                .isEqualTo("CANCELED");

        assertThat(captor.getValue().getActorEmployeeId())
                .isEqualTo(1L);
    }

    // 10. 신청자가 아닌 직원의 취소
    @Test
    void shouldRejectCancellationByWrongEmployee() {

        // given
        LeaveCancelRequestDto request =
                new LeaveCancelRequestDto();

        request.setEmployeeId(2L);

        when(leaveRequestMapper.findById(10L))
                .thenReturn(leave("PENDING"));

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> leaveRequestService.cancel(10L, request)
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.APPLICANT_MISMATCH);

        verify(approvalHistoryMapper, never())
                .insert(any(ApprovalHistory.class));
    }

    // 11. 검색 페이징 결과
    @Test
    void shouldCalculateTotalPages() {

        // given
        LeaveSearchConditionDto condition =
                new LeaveSearchConditionDto();

        condition.setPage(2);
        condition.setSize(10);

        when(leaveRequestMapper.countByCondition(condition))
                .thenReturn(23L);

        when(leaveRequestMapper.findAllWithDetails(condition))
                .thenReturn(List.of());

        // when
        LeavePageResponseDto response =
                leaveRequestService.findAll(condition);

        // then
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(23L);
        assertThat(response.getTotalPages()).isEqualTo(3L);

        verify(leaveRequestMapper).countByCondition(condition);
        verify(leaveRequestMapper).findAllWithDetails(condition);
    }

    // 테스트용 휴가 등록 DTO 생성
    private LeaveCreateRequestDto createRequest() {

        LeaveCreateRequestDto request =
                new LeaveCreateRequestDto();

        request.setEmployeeId(1L);
        request.setApproverId(4L);
        request.setStartDate(LocalDate.of(2027, 5, 10));
        request.setEndDate(LocalDate.of(2027, 5, 12));
        request.setReason("개인 일정");

        return request;
    }

    // 테스트용 휴가 Domain 생성
    private LeaveRequest leave(String status) {

        LeaveRequest leave = new LeaveRequest();

        leave.setRequestId(10L);
        leave.setEmployeeId(1L);
        leave.setApproverId(4L);
        leave.setStartDate(LocalDate.of(2027, 5, 10));
        leave.setEndDate(LocalDate.of(2027, 5, 12));
        leave.setReason("개인 일정");
        leave.setStatus(status);

        return leave;
    }

    // Mock 이력 저장 성공 처리
    private void mockSuccessfulHistoryInsert() {

        when(approvalHistoryMapper.insert(
                any(ApprovalHistory.class)
        )).thenAnswer(invocation -> {

            ApprovalHistory history = invocation.getArgument(0);

            history.setHistoryId(100L);

            return 1;
        });
    }
}