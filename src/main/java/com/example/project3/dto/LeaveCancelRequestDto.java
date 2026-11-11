package com.example.project3.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeaveCancelRequestDto {

    @NotNull(message = "필수")
    @Positive(message = "1 이상")
    private long employeeId;

}
