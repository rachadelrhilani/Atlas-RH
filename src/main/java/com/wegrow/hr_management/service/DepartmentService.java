package com.wegrow.hr_management.service;

import com.wegrow.hr_management.dto.request.DepartmentRequestDTO;
import com.wegrow.hr_management.dto.response.DepartmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {
    Page<DepartmentResponseDTO> findAll(Pageable pageable);
    DepartmentResponseDTO findById(Long id);
    DepartmentResponseDTO create(DepartmentRequestDTO dto);
    DepartmentResponseDTO update(Long id, DepartmentRequestDTO dto);
    void delete(Long id);
}