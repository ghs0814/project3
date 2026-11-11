package com.example.project3.controller;

import com.example.project3.dto.*;
import com.example.project3.service.LeaveRequestService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leaves")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    //기안 신청
    @PostMapping
    public ResponseEntity<LeaveResponseDto> create(
            @Valid @RequestBody LeaveCreateRequestDto request
    ) {

        LeaveResponseDto response = leaveRequestService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    //
    // 2. 신규: 휴가 전체 조회
    @GetMapping
    public LeavePageResponseDto findAll(
            @Valid @ModelAttribute LeaveSearchConditionDto condition
    ) {

        return leaveRequestService.findAll(condition);
    }

    // 3. 신규: 휴가 상세 조회
    @GetMapping("/{requestId}")
    public LeaveDetailResponseDto findById(
            @PathVariable("requestId") Long requestId
    ) {

        return leaveRequestService.findById(requestId);
    }

    // 4. 휴가 승인
    @PostMapping("/{requestId}/approve")
    public LeaveResponseDto approve(
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody LeaveApproveRequestDto request
    ) {
    return leaveRequestService.approve(requestId, request);
    }

    //반려
    @PostMapping("/{requestId}/reject")
    public LeaveResponseDto reject(
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody LeaveRejectRequestDto request
    ){
        return leaveRequestService.reject(requestId, request);
    }

    //본인취소
    @PostMapping("/{requestId}/cancel")
    public LeaveResponseDto cancel(
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody LeaveCancelRequestDto request
    ){
        return leaveRequestService.cancel(requestId, request);
    }
    // 7. 신규: 휴가 신청 처리 이력 조회
    @GetMapping("/{requestId}/history")
    public List<ApprovalHistoryResponseDto> findHistoryByRequestId(
            @PathVariable("requestId") Long requestId
    ) {

        return leaveRequestService.findHistoryByRequestId(requestId);
    }
}