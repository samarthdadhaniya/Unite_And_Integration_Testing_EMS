package com.ems.test;

import com.ems.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EmployeeRecordManagerTest {
    @Mock HRDatabase db;
    EmployeeRecordManager manager;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        manager = new EmployeeRecordManager(db);
    }

    @Test
    void validEmployee_shouldSave() {
        Employee e = new Employee("E1", "Sam", 30000, 0.1, 80);
        when(db.findById("E1")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> manager.createEmployee(e));
        verify(db).save(e);
    }

    @Test
    void duplicateEmployee_shouldThrowError() {
        Employee e = new Employee("E1", "Sam", 30000, 0.1, 80);
        when(db.findById("E1")).thenReturn(Optional.of(e));
        assertThrows(IllegalArgumentException.class, () -> manager.createEmployee(e));
    }

    @Test
    void invalidSalary_shouldThrowError() {
        Employee e = new Employee("E2", "Bob", -5000, 0.1, 50);
        assertThrows(IllegalArgumentException.class, () -> manager.createEmployee(e));
    }

    @Test
    void invalidTaxRate_shouldThrowError() {
        Employee e = new Employee("E3", "John", 30000, 1.2, 50);
        assertThrows(IllegalArgumentException.class, () -> manager.createEmployee(e));
    }
}