package com.wegrow.hr_management.dto.response;

import lombok.Data;

@Data
public class SeniorityResponseDTO {
    private int years;
    private int months;
    private String formattedSeniority; 
}