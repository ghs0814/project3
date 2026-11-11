package com.example.project3.controller;

import com.example.project3.dto.LeaveApproveRequestDto;
import com.example.project3.dto.LeavePageResponseDto;
import com.example.project3.dto.LeaveResponseDto;
import com.example.project3.exception.BusinessException;
import com.example.project3.exception.ErrorCode;
import com.example.project3.service.LeaveRequestService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveRequestController.class)
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveRequestService leaveRequestService;

    // 1. 페이징 응답 확인
    @Test
    void shouldReturnPagedLeaves() throws Exception {

        // given
        LeavePageResponseDto response =
                new LeavePageResponseDto(
                        List.of(),
                        1,
                        10,
                        23L,
                        3L
                );

        when(leaveRequestService.findAll(
                any()
        )).thenReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/leaves")
                                .param("page", "1")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(23))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content").isArray());
    }

    // 2. 잘못된 페이지 번호
    @Test
    void shouldReturnValidationErrorForInvalidPage()
            throws Exception {

        // given
        // page=0은 @Min(1) 위반

        // when & then
        mockMvc.perform(
                        get("/api/leaves")
                                .param("page", "0")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("page"));
    }

    // 3. 존재하지 않는 휴가 신청
    @Test
    void shouldReturnNotFoundForMissingLeave()
            throws Exception {

        // given
        when(leaveRequestService.findById(999999L))
                .thenThrow(
                        new BusinessException(
                                ErrorCode.LEAVE_NOT_FOUND
                        )
                );

        // when & then
        mockMvc.perform(
                        get("/api/leaves/999999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("LEAVE_NOT_FOUND"))
                .andExpect(jsonPath("$.path")
                        .value("/api/leaves/999999"));
    }

    // 4. 정상 휴가 승인
    @Test
    void shouldReturnApprovedLeave() throws Exception {

        // given
        LeaveResponseDto response =
                new LeaveResponseDto(
                        10L,
                        1L,
                        4L,
                        LocalDate.of(2027, 5, 10),
                        LocalDate.of(2027, 5, 12),
                        "개인 일정",
                        "APPROVED",
                        LocalDateTime.of(2027, 5, 1, 9, 0),
                        LocalDateTime.of(2027, 5, 1, 10, 0),
                        null
                );

        when(leaveRequestService.approve(
                eq(10L),
                any(LeaveApproveRequestDto.class)
        )).thenReturn(response);

        // when & then
        mockMvc.perform(
                        post("/api/leaves/10/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "approverId": 4
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(10))
                .andExpect(jsonPath("$.status")
                        .value("APPROVED"))
                .andExpect(jsonPath("$.approverId").value(4));
    }

    // 5. 반려 사유 누락
    @Test
    void shouldRejectEmptyRejectionComment() throws Exception {

        // given
        String requestBody = """
                {
                    "approverId": 4,
                    "comment": ""
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/api/leaves/10/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("comment"));
    }

    // 6. 잘못된 JSON
    @Test
    void shouldReturnInvalidJsonError() throws Exception {

        // given
        String invalidJson = """
                {
                    "employeeId": 1,
                    "approverId":
                """;

        // when & then
        mockMvc.perform(
                        post("/api/leaves")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_JSON"));
    }
}