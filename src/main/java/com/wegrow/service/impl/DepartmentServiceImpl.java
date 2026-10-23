package com.wegrow.service.impl;

import com.wegrow.dto.request.DepartmentRequestDTO;
import com.wegrow.dto.response.DepartmentResponseDTO;
import com.wegrow.exception.ResourceAlreadyExistsException;
import com.wegrow.exception.ResourceNotFoundException;
import com.wegrow.mapper.DepartmentMapper;
import com.wegrow.model.Department;
import com.wegrow.repository.DepartmentRepository;
import com.wegrow.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentResponseDTO> findAll(Pageable pageable) {
        return departmentRepository.findAll(pageable).map(departmentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponseDTO findById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé avec l'ID : " + id));
        return departmentMapper.toDto(department);
    }

    @Override
    public DepartmentResponseDTO create(DepartmentRequestDTO dto) {
        if (departmentRepository.findByName(dto.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Un département nommé '" + dto.getName() + "' existe déjà.");
        }
        Department department = departmentMapper.toEntity(dto);
        return departmentMapper.toDto(departmentRepository.save(department));
    }

    @Override
    public DepartmentResponseDTO update(Long id, DepartmentRequestDTO dto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé avec l'ID : " + id));

        departmentRepository.findByName(dto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResourceAlreadyExistsException("Un département nommé '" + dto.getName() + "' existe déjà.");
            }
        });

        departmentMapper.updateEntityFromDto(dto, department);
        return departmentMapper.toDto(departmentRepository.save(department));
    }

    @Override
    public void delete(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé avec l'ID : " + id));

        if (department.getEmployees() != null && !department.getEmployees().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer un département qui contient des employés.");
        }

        departmentRepository.delete(department);
    }
}