package com.ems;

import java.util.Optional;

public interface HRDatabase {
    Optional<Employee> findById(String id);
    void save(Employee employee);
}