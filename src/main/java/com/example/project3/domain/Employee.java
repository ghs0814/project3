package com.example.project3.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class Employee {

    private Long employeeId;

    private String employeeName;

    private String department;
}