package com.example.project3.config;

import com.example.project3.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseStartupChecker implements ApplicationRunner {

    private final EmployeeMapper employeeMapper;

    @Override
    public void run(ApplicationArguments args) {

        long employeeCount = employeeMapper.countAll();

        log.info("====================================");
        log.info("Project3 MyBatis DB");
        log.info("직원 전체 수: {}", employeeCount);
        log.info("====================================");
    }
}