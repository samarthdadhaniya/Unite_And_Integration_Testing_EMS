package com.ems;

import java.util.Optional;

public class EmployeeRecordManager {
    private final HRDatabase database;

    public EmployeeRecordManager(HRDatabase database) {
        this.database = database;
    }

    public void createEmployee(Employee e) {
        // Validate employee data
        if (e == null) {
            throw new IllegalArgumentException("Employee cannot be null");
        }
        
        if (e.getId() == null || e.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee ID cannot be null or empty");
        }
        
        if (e.getName() == null || e.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name cannot be null or empty");
        }
        
        if (e.getBaseSalary() < 0) {
            throw new IllegalArgumentException("Base salary cannot be negative");
        }
        
        if (e.getBonusRate() < 0 || e.getBonusRate() > 1.0) {
            throw new IllegalArgumentException("Bonus rate must be between 0 and 1.0");
        }
        
        // Check for duplicate employee
        Optional<Employee> existing = database.findById(e.getId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Employee with ID " + e.getId() + " already exists");
        }
        
        database.save(e);
    }
}