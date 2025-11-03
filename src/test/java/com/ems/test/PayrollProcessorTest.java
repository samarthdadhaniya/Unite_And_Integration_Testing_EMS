package com.ems.test;

import com.ems.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.Map;
import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PayrollProcessorTest {

    @Mock private AttendanceService attendanceService;
    @Mock private TaxService taxService;
    @Mock private PerformanceEvaluator performanceEvaluator;

    private PayrollProcessor payrollProcessor;
    private int currentMonth;
    private int currentYear;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        payrollProcessor = new PayrollProcessor(attendanceService, taxService, performanceEvaluator);
        currentMonth = LocalDate.now().getMonthValue();
        currentYear = LocalDate.now().getYear();
    }

    @Test
    void fullAttendance_highPerformance_shouldAddBonus() {
        Employee e = new Employee("E1", "Alice", 22000, 0.1, 95);

        when(attendanceService.getDaysPresent("E1", currentMonth, currentYear)).thenReturn(22);
        when(performanceEvaluator.evaluatePerformance(e)).thenReturn(90);
        when(taxService.calculateTax(e)).thenReturn(0.1);

        Map<String, Double> result = payrollProcessor.processPayroll(e);

        double gross = e.getBaseSalary() * (22.0 / 22.0); // full attendance
        double bonus = e.getBaseSalary() * 0.2; // performance >= 85
        double tax = e.getBaseSalary() * 0.1;
        double expectedNet = gross + bonus - tax;

        assertEquals(expectedNet, result.get("NetSalary"), 0.01);
    }

    @Test
    void partialAttendance_lowPerformance_shouldReduceSalary() {
        Employee e = new Employee("E2", "Bob", 22000, 0.1, 40);

        when(attendanceService.getDaysPresent("E2", currentMonth, currentYear)).thenReturn(11);
        when(performanceEvaluator.evaluatePerformance(e)).thenReturn(60);
        when(taxService.calculateTax(e)).thenReturn(0.1);

        Map<String, Double> result = payrollProcessor.processPayroll(e);

        double attendanceFactor = Math.min(1.0, 11.0 / 22.0);
        double gross = e.getBaseSalary() * attendanceFactor;
        double bonus = e.getBaseSalary() * 0.05; // performance < 70
        double tax = e.getBaseSalary() * 0.1;
        double expectedNet = gross + bonus - tax;

        assertEquals(expectedNet, result.get("NetSalary"), 0.01);
    }

    @Test
    void zeroAttendance_shouldReturnZeroSalary() {
        Employee e = new Employee("E3", "Carol", 22000, 0.1, 100);

        when(attendanceService.getDaysPresent("E3", currentMonth, currentYear)).thenReturn(0);
        when(performanceEvaluator.evaluatePerformance(e)).thenReturn(100);
        when(taxService.calculateTax(e)).thenReturn(0.0);

        Map<String, Double> result = payrollProcessor.processPayroll(e);

        assertEquals(0.0, result.get("GrossSalary"), 0.01);
        assertTrue(result.get("NetSalary") >= 0.0);
    }
}