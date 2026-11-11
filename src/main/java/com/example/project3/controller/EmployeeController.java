package com.example.project3.controller;

import com.example.project3.dto.EmployeeResponseDto;
import com.example.project3.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    // 직원 전체 조회
    @GetMapping
    public List<EmployeeResponseDto> findAll() {

        return employeeService.findAll();
    }

    // 직원 상세 조회
    @GetMapping("/{employeeId}")
    public EmployeeResponseDto findById(
            @PathVariable("employeeId") Long employeeId
    ) {

        return employeeService.findById(employeeId);
    }
}