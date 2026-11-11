package com.example.project3.mapper;

import com.example.project3.domain.ApprovalHistory;
import com.example.project3.dto.ApprovalHistoryResponseDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApprovalHistoryMapper {

    // 처리 이력 등록
    int insert(ApprovalHistory approvalHistory);

    // 휴가 신청별 처리 이력 조회
    List<ApprovalHistoryResponseDto> findByRequestId(
            @Param("requestId") Long requestId
    );
}