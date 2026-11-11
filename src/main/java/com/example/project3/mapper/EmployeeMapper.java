package com.example.project3.mapper;

import com.example.project3.domain.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper {

    long countAll();

    List<Employee> findAll();

    Employee findById(@Param("employeeId") Long employeeId);


}
