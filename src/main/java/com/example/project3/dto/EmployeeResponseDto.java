package com.example.project3.dto;

import com.example.project3.domain.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmployeeResponseDto {

    private Long employeeId;

    private String employeeName;

    private String department;

    //static factory method
    public static EmployeeResponseDto from(Employee employee) {

        return new EmployeeResponseDto(
                employee.getEmployeeId(),
                employee.getEmployeeName(),
                employee.getDepartment()
        );
    }
}