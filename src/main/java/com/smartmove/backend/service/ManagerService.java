package com.smartmove.backend.service;

import com.smartmove.backend.data.Manager;
import com.smartmove.backend.data.ManagerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManagerService {
    private final ManagerRepository repository;

    public ManagerService(ManagerRepository repository) {
        this.repository = repository;
    }

    public List<Manager> getAllManagers() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "managerId"));
    }

    public Manager createManager(Manager manager) {
        return repository.save(manager);
    }
}
