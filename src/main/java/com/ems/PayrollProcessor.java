package com.ems;

import java.util.Map;

public class PayrollProcessor {

    private final AttendanceService attendanceService;
    private final TaxService taxService;
    private final PerformanceEvaluator performanceEvaluator;

    public PayrollProcessor(AttendanceService attendanceService,
                            TaxService taxService,
                            PerformanceEvaluator performanceEvaluator) {
        this.attendanceService = attendanceService;
        this.taxService = taxService;
        this.performanceEvaluator = performanceEvaluator;
    }

    public Double calculateNetSalary(Employee employee, int month, int year) {
        try {
            int daysPresent = attendanceService.getDaysPresent(employee.getId(), month, year);
            Double taxRate = taxService.calculateTax(employee);
            Integer performance = performanceEvaluator.evaluatePerformance(employee);

            if (taxRate == null || performance == null) return null;

            double base = employee.getBaseSalary();
            double attendanceFactor = Math.min(1.0, daysPresent / 22.0);
            double bonusRate = performance >= 85 ? 0.2 : (performance >= 70 ? 0.1 : 0.05);

            double gross = base * attendanceFactor;
            double bonus = base * bonusRate;
            double tax = base * taxRate;

            return gross + bonus - tax;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Double> processPayroll(Employee e) {
        Double netSalary = calculateNetSalary(e, java.time.LocalDate.now().getMonthValue(), 
                                              java.time.LocalDate.now().getYear());
        if (netSalary == null) {
            return java.util.Map.of("NetSalary", 0.0, "GrossSalary", 0.0, "Bonus", 0.0, "Tax", 0.0);
        }
        
        int daysPresent = attendanceService.getDaysPresent(e.getId(), 
            java.time.LocalDate.now().getMonthValue(), java.time.LocalDate.now().getYear());
        Double taxRate = taxService.calculateTax(e);
        Integer performance = performanceEvaluator.evaluatePerformance(e);
        
        double base = e.getBaseSalary();
        double attendanceFactor = Math.min(1.0, daysPresent / 22.0);
        double gross = base * attendanceFactor;
        double bonusRate = performance != null && performance >= 85 ? 0.2 : 
                          (performance != null && performance >= 70 ? 0.1 : 0.05);
        double bonus = base * bonusRate;
        double tax = taxRate != null ? base * taxRate : 0.0;
        
        return java.util.Map.of(
            "GrossSalary", gross,
            "Bonus", bonus,
            "Tax", tax,
            "NetSalary", netSalary
        );
    }
}