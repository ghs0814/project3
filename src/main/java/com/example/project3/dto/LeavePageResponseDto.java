package com.example.project3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor // 반환용 dto
public class LeavePageResponseDto {

    // 현재 페이지 데이터
    private List<LeaveDetailResponseDto> content;

    // 현재 페이지 번호
    private int page;

    // 페이지 크기
    private int size;

    // 전체 검색 결과 건수
    private long totalElements;

    // 전체 페이지 수
    private long totalPages;
}