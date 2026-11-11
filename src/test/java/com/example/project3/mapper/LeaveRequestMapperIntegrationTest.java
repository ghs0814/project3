package com.example.project3.mapper;

import com.example.project3.dto.ApprovalHistoryResponseDto;
import com.example.project3.dto.LeaveApproveRequestDto;
import com.example.project3.dto.LeaveDetailResponseDto;
import com.example.project3.dto.LeaveResponseDto;
import com.example.project3.dto.LeaveSearchConditionDto;

import com.example.project3.service.LeaveRequestService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LeaveRequestMapperIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LeaveRequestMapper leaveRequestMapper;

    @Autowired
    private ApprovalHistoryMapper approvalHistoryMapper;

    @Autowired
    private LeaveRequestService leaveRequestService;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // 1. 실제 PostgreSQL 검색 + 페이징
    @Test
    void shouldFilterAndPaginateLeaves() {

        // given
        Long employeeId = insertEmployee("페이징 신청자");
        Long approverId = insertEmployee("페이징 승인자");

        insertLeave(
                employeeId,
                approverId,
                LocalDate.of(2028, 1, 10),
                LocalDate.of(2028, 1, 12)
        );

        insertLeave(
                employeeId,
                approverId,
                LocalDate.of(2028, 2, 10),
                LocalDate.of(2028, 2, 12)
        );

        insertLeave(
                employeeId,
                approverId,
                LocalDate.of(2028, 3, 10),
                LocalDate.of(2028, 3, 12)
        );

        LeaveSearchConditionDto condition =
                new LeaveSearchConditionDto();

        condition.setEmployeeId(employeeId);
        condition.setApproverId(approverId);
        condition.setStatus("PENDING");
        condition.setSort("oldest");
        condition.setPage(2);
        condition.setSize(2);

        // when
        long totalElements =
                leaveRequestMapper.countByCondition(condition);

        List<LeaveDetailResponseDto> content =
                leaveRequestMapper.findAllWithDetails(condition);

        // then
        assertThat(totalElements).isEqualTo(3L);

        assertThat(content).hasSize(1);

        assertThat(content.get(0).getEmployeeId())
                .isEqualTo(employeeId);

        assertThat(content.get(0).getStartDate())
                .isEqualTo(LocalDate.of(2028, 3, 10));
    }

    // 2. 실제 DB에서 승인 + 이력 저장
    @Test
    void shouldApproveLeaveAndPersistHistory() {

        // given
        Long employeeId = insertEmployee("승인 신청자");
        Long approverId = insertEmployee("승인 담당자");

        Long requestId = insertLeave(
                employeeId,
                approverId,
                LocalDate.of(2028, 4, 10),
                LocalDate.of(2028, 4, 12)
        );

        LeaveApproveRequestDto request =
                new LeaveApproveRequestDto();

        request.setApproverId(approverId);

        // when
        LeaveResponseDto response =
                leaveRequestService.approve(requestId, request);

        List<ApprovalHistoryResponseDto> histories =
                approvalHistoryMapper.findByRequestId(requestId);

        // then
        assertThat(response.getStatus())
                .isEqualTo("APPROVED");

        assertThat(response.getDecidedAt())
                .isNotNull();

        assertThat(histories).hasSize(1);

        ApprovalHistoryResponseDto history = histories.get(0);

        assertThat(history.getRequestId())
                .isEqualTo(requestId);

        assertThat(history.getActorEmployeeId())
                .isEqualTo(approverId);

        assertThat(history.getAction())
                .isEqualTo("APPROVED");

        assertThat(history.getActedAt())
                .isNotNull();
    }

    // 3. DB의 휴가 기간 중복 제약조건 확인
    @Test
    void shouldRejectOverlappingLeaveAtDatabaseLevel() {

        // given
        Long employeeId = insertEmployee("중복 신청자");
        Long approverId = insertEmployee("중복 승인자");

        insertLeave(
                employeeId,
                approverId,
                LocalDate.of(2028, 5, 10),
                LocalDate.of(2028, 5, 12)
        );

        // when & then
        assertThatThrownBy(() -> insertLeave(
                employeeId,
                approverId,
                LocalDate.of(2028, 5, 11),
                LocalDate.of(2028, 5, 13)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    // 테스트용 직원 데이터 생성
    private Long insertEmployee(String name) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO employees (
                    employee_name,
                    department
                )
                VALUES (?, ?)
                RETURNING employee_id
                """,
                Long.class,
                "STEP14 " + name,
                "테스트부"
        );
    }

    // 테스트용 휴가 데이터 생성
    private Long insertLeave(
            Long employeeId,
            Long approverId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO leave_requests (
                    employee_id,
                    approver_id,
                    start_date,
                    end_date,
                    reason
                )
                VALUES (?, ?, ?, ?, ?)
                RETURNING request_id
                """,
                Long.class,
                employeeId,
                approverId,
                Date.valueOf(startDate),
                Date.valueOf(endDate),
                "STEP14 통합 테스트"
        );
    }
}