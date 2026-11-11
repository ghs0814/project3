package com.example.project3.mapper;

import com.example.project3.domain.LeaveRequest;
import com.example.project3.dto.LeaveDetailResponseDto;
import com.example.project3.dto.LeaveSearchConditionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface LeaveRequestMapper {

    //휴가 등록
    int insert(LeaveRequest leaveRequest);


    //등록 후 id로 조회
    LeaveRequest findById(@Param("requestId") Long requestId);


    // 신규: 휴가 신청 전체 JOIN 조회
    // 2개 이상의 테이블을 join하는 경우에는 dto로 반환
    // 추가 정보가 포함된 join 쿼리이므로 dto로 반환한다.
    //List<LeaveDetailResponseDto> findAllWithDetails();
    List<LeaveDetailResponseDto> findAllWithDetails(
            LeaveSearchConditionDto condition
    );

    // 4. 신규: 검색 조건에 해당하는 전체 건수
    long countByCondition(
            LeaveSearchConditionDto condition
    );

    // 신규: 휴가 신청 상세 JOIN 조회
    LeaveDetailResponseDto findDetailById(
            @Param("requestId") Long requestId
    );
    // 5. 신규: 같은 직원의 휴가 기간 중복 검사
    boolean existsOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    //휴가 승인
    int approve(
            @Param("requestId") long requestId,
            @Param("approverId")  long approverId
    );

    //휴가 반려
    int reject(
            @Param("requestId") long requestId,
            @Param("approverId")  long approverId,
            @Param("comment") String comment
    );

    // 휴가 신청 취소
    int cancel(
            @Param("requestId") long requestId,
            @Param("employeeId") long employeeId
    );
}
