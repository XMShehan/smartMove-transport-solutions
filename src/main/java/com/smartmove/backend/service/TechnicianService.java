package com.smartmove.backend.service;

import com.smartmove.backend.data.Technician;
import com.smartmove.backend.data.TechnicianRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TechnicianService {
    private final TechnicianRepository repository;

    public TechnicianService(TechnicianRepository repository) {
        this.repository = repository;
    }

    public List<Technician> getAllTechnicians() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "technicianId"));
    }

    public Technician createTechnician(Technician technician) {
        return repository.save(technician);
    }
}
