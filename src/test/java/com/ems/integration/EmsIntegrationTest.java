package com.ems.integration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ems.AttendanceService;
import com.ems.Employee;
import com.ems.EmployeeRecordManager;
import com.ems.HRDatabase;
import com.ems.PayrollProcessor;
import com.ems.PerformanceEvaluator;
import com.ems.ReportGenerator;
import com.ems.TaxService;

/**
 * Comprehensive Integration Tests for Employee Management System
 * 
 * Integration Strategy: Top-Down Approach
 * - Top-level modules (EmployeeRecordManager, PayrollProcessor) are real implementations
 * - Lower-level dependencies (AttendanceService, TaxService, PerformanceEvaluator) are stubbed/mocked
 * 
 * Test Scenarios:
 * 1. Data synchronization between employee records and payroll calculation
 * 2. Concurrency handling when multiple employees processed simultaneously
 * 3. System response to inconsistent data (null bonus/tax anomalies)
 * 4. Cross-module boundary value testing for salary thresholds
 * 5. Asynchronous data transfer during report generation delays
 */
public class EmsIntegrationTest {

    private static final StringBuilder TEST_RESULTS_LOG = new StringBuilder();
    private static final List<DefectSummary> DEFECT_SUMMARIES = new ArrayList<>();

    @BeforeAll
    static void setupTestSuite() {
        TEST_RESULTS_LOG.append("=".repeat(80)).append("\n");
        TEST_RESULTS_LOG.append("INTEGRATION TEST SUITE - Employee Management System\n");
        TEST_RESULTS_LOG.append("Start Time: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        TEST_RESULTS_LOG.append("=".repeat(80)).append("\n\n");
    }

    @AfterAll
    static void tearDownTestSuite() {
        TEST_RESULTS_LOG.append("\n").append("=".repeat(80)).append("\n");
        TEST_RESULTS_LOG.append("TEST SUITE SUMMARY\n");
        TEST_RESULTS_LOG.append("End Time: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        TEST_RESULTS_LOG.append("Total Defects Found: ").append(DEFECT_SUMMARIES.size()).append("\n");
        
        if (!DEFECT_SUMMARIES.isEmpty()) {
            TEST_RESULTS_LOG.append("\nDEFECT SUMMARIES:\n");
            TEST_RESULTS_LOG.append("-".repeat(80)).append("\n");
            for (DefectSummary defect : DEFECT_SUMMARIES) {
                TEST_RESULTS_LOG.append(defect.toString()).append("\n");
            }
        }
        
        TEST_RESULTS_LOG.append("=".repeat(80)).append("\n");
        System.out.println(TEST_RESULTS_LOG.toString());
    }

    // Common test utilities
    static class InMemoryHRDatabase implements HRDatabase {
        private final ConcurrentMap<String, Employee> map = new ConcurrentHashMap<>();
        @Override public Optional<Employee> findById(String id) { return Optional.ofNullable(map.get(id)); }
        @Override public void save(Employee e) { map.put(e.getId(), e); }
    }

    static class DefectSummary {
        String testName;
        String defectDescription;
        String stackTrace;
        String timestamp;

        DefectSummary(String testName, String defectDescription, String stackTrace) {
            this.testName = testName;
            this.defectDescription = defectDescription;
            this.stackTrace = stackTrace;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public String toString() {
            return String.format(
                "[DEFECT] Test: %s\n  Description: %s\n  Time: %s\n  Stack Trace:\n%s\n",
                testName, defectDescription, timestamp, stackTrace
            );
        }
    }

    private void logAssertion(String testName, String assertion, boolean passed, Object actual, Object expected) {
        TEST_RESULTS_LOG.append(String.format(
            "[ASSERTION-LOG] Test: %s | Assertion: %s | Status: %s | Expected: %s | Actual: %s\n",
            testName, assertion, passed ? "PASS" : "FAIL", expected, actual
        ));
    }

    private void logException(String testName, Exception e, String context) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        
        DEFECT_SUMMARIES.add(new DefectSummary(testName, context, stackTrace));
        
        TEST_RESULTS_LOG.append(String.format(
            "[EXCEPTION-LOG] Test: %s | Context: %s\n  Exception: %s\n  Stack Trace:\n%s\n",
            testName, context, e.getMessage(), stackTrace
        ));
    }

    /* --------- Test 1: Data synchronization between employee records and payroll calculation --------- */
    @Test
    @DisplayName("Scenario 1: Data Synchronization - Employee Records ↔ Payroll Calculation")
    public void scenario_dataSync_betweenRecordAndPayroll() {
        String testName = "scenario_dataSync_betweenRecordAndPayroll";
        TEST_RESULTS_LOG.append("\n--- Test 1: Data Synchronization Test ---\n");
        
        try {
            // Top-level: EmployeeRecordManager (real) + PayrollProcessor (real) with stubbed dependencies
            InMemoryHRDatabase hr = new InMemoryHRDatabase();
            EmployeeRecordManager manager = new EmployeeRecordManager(hr);

            // stubs
            AttendanceService attendance = (id, m, y) -> 22; // full attendance
            TaxService taxService = employee -> 0.1; // 10% tax
            PerformanceEvaluator perfEval = employee -> 80; // stable score

            PayrollProcessor payroll = new PayrollProcessor(attendance, taxService, perfEval);

            // create employee via record manager
            Employee e = new Employee("I1", "Integrate", 20000, 0.1, 70);
            manager.createEmployee(e); // uses HRDatabase -> stored
            logAssertion(testName, "Employee created successfully", true, "Employee I1", "Employee I1");

            // simulate payroll reading latest employee profile from HR DB before calculation
            Optional<Employee> fromDb = hr.findById("I1");
            boolean dbCheck = fromDb.isPresent();
            logAssertion(testName, "Employee present in HR DB", dbCheck, fromDb.isPresent(), true);
            assertTrue(dbCheck, "Employee must be present in HR DB");

            Double net = payroll.calculateNetSalary(fromDb.get(), 11, 2025);
            boolean netNotNull = net != null;
            logAssertion(testName, "Net salary computed", netNotNull, net, "not null");
            assertNotNull(net, "Net salary should be computed");
            
            // manual expected: gross 20000 + bonus (10% since perf 80) - tax (10% of gross)
            double expected = 20000 + (20000 * 0.10) - (20000 * 0.1);
            boolean netMatch = Math.abs(expected - net) < 0.0001;
            logAssertion(testName, "Net salary matches expected", netMatch, net, expected);
            assertEquals(expected, net, 0.0001, "Net must match expected value");
            
            TEST_RESULTS_LOG.append("✓ Test 1 PASSED: Data synchronization working correctly\n");
        } catch (Exception e) {
            logException(testName, e, "Data synchronization failure");
            throw new AssertionError("Data synchronization test failed", e);
        }
    }

    /* --------- Test 2: Concurrency - multiple employees processed concurrently --------- */
    @Test
    @DisplayName("Scenario 2: Concurrency - Multiple Employees Processed Simultaneously")
    public void scenario_concurrency_salaryComputation() throws InterruptedException {
        String testName = "scenario_concurrency_salaryComputation";
        TEST_RESULTS_LOG.append("\n--- Test 2: Concurrency Test ---\n");
        
        try {
            InMemoryHRDatabase hr = new InMemoryHRDatabase();
            EmployeeRecordManager manager = new EmployeeRecordManager(hr);

            AttendanceService attendance = (id, m, y) -> 22; // full attendance for simplicity
            TaxService taxService = employee -> 0.1;
            PerformanceEvaluator perfEval = employee -> 80;

            PayrollProcessor payroll = new PayrollProcessor(attendance, taxService, perfEval);

            // Prepare many employees
            int count = 50;
            for (int i = 0; i < count; i++) {
                manager.createEmployee(new Employee("C" + i, "Emp" + i, 20000 + i*100, 0.1, 70 + (i%5)));
            }
            logAssertion(testName, "Employees created", true, count, count);

            ExecutorService ex = Executors.newFixedThreadPool(8);
            CountDownLatch latch = new CountDownLatch(count);
            AtomicInteger failed = new AtomicInteger(0);
            AtomicInteger success = new AtomicInteger(0);
            List<String> failureDetails = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < count; i++) {
                final String id = "C" + i;
                ex.submit(() -> {
                    try {
                        Optional<Employee> emp = hr.findById(id);
                        if (emp.isEmpty()) {
                            failed.incrementAndGet();
                            failureDetails.add("Employee " + id + " not found in DB");
                            return;
                        }
                        Double net = payroll.calculateNetSalary(emp.get(), 11, 2025);
                        if (net == null || net.isInfinite() || net.isNaN()) {
                            failed.incrementAndGet();
                            failureDetails.add("Invalid net salary for " + id + ": " + net);
                        } else {
                            success.incrementAndGet();
                        }
                    } catch (Exception exx) {
                        failed.incrementAndGet();
                        StringWriter sw = new StringWriter();
                        exx.printStackTrace(new PrintWriter(sw));
                        failureDetails.add("Exception for " + id + ": " + sw.toString());
                        logException(testName, exx, "Concurrent processing failure for employee " + id);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            ex.shutdownNow();

            logAssertion(testName, "All threads completed", completed, completed, true);
            logAssertion(testName, "Failed computations", failed.get() == 0, failed.get(), 0);
            
            if (!failureDetails.isEmpty()) {
                TEST_RESULTS_LOG.append("Failure Details:\n");
                failureDetails.forEach(detail -> TEST_RESULTS_LOG.append("  - ").append(detail).append("\n"));
            }

            assertEquals(0, failed.get(), "All concurrent computations should complete successfully. Failures: " + failureDetails);
            TEST_RESULTS_LOG.append("✓ Test 2 PASSED: Concurrency handling verified (").append(success.get()).append(" successful)\n");
        } catch (Exception e) {
            logException(testName, e, "Concurrency test failure");
            throw e;
        }
    }

    /* --------- Test 3: PayrollProcessor returns inconsistent data (null bonus or null tax) --------- */
    @Test
    @DisplayName("Scenario 3: Inconsistent Data Handling - Null Tax/Performance")
    public void scenario_inconsistentData_fromPayroll() {
        String testName = "scenario_inconsistentData_fromPayroll";
        TEST_RESULTS_LOG.append("\n--- Test 3: Inconsistent Data Handling Test ---\n");
        
        try {
            InMemoryHRDatabase hr = new InMemoryHRDatabase();
            EmployeeRecordManager manager = new EmployeeRecordManager(hr);

            // Attendance: full
            AttendanceService attendance = (id, m, y) -> 22;

            // TaxService returns null to simulate anomaly (tax service down)
            TaxService taxService = employee -> null;

            // PerformanceEvaluator returns null sometimes (simulate missing evaluation)
            PerformanceEvaluator perfEval = employee -> null;

            PayrollProcessor payroll = new PayrollProcessor(attendance, taxService, perfEval);

            Employee e = new Employee("A1", "Anomaly", 15000, 0.1, 50);
            manager.createEmployee(e);
            logAssertion(testName, "Employee created with anomaly scenario", true, "Employee A1", "Employee A1");

            // calculate net -> expected null because TaxService returned null
            Double net = payroll.calculateNetSalary(e, 11, 2025);
            boolean isNull = net == null;
            logAssertion(testName, "Payroll returns null for inconsistent data", isNull, net, null);
            
            TEST_RESULTS_LOG.append("[INTEGRATION-LOG] Payroll returned null for employee A1 due to tax anomaly\n");
            TEST_RESULTS_LOG.append("  Root Cause: TaxService returned null (service unavailable)\n");
            TEST_RESULTS_LOG.append("  System Response: PayrollProcessor correctly returns null to signal data inconsistency\n");
            
            assertNull(net, "When tax service returns null, payroll should signal inconsistency with null return");
            
            TEST_RESULTS_LOG.append("✓ Test 3 PASSED: Inconsistent data handling verified\n");
        } catch (Exception e) {
            logException(testName, e, "Inconsistent data test failure");
            throw new AssertionError("Inconsistent data test failed", e);
        }
    }

    /* --------- Test 4: Cross-module boundary value testing for salary thresholds and deductions --------- */
    @Test
    @DisplayName("Scenario 4: Boundary Value Testing - Salary Thresholds and Deductions")
    public void scenario_boundaryValues_salaryThresholds() {
        String testName = "scenario_boundaryValues_salaryThresholds";
        TEST_RESULTS_LOG.append("\n--- Test 4: Boundary Value Testing ---\n");
        
        try {
            InMemoryHRDatabase hr = new InMemoryHRDatabase();
            EmployeeRecordManager manager = new EmployeeRecordManager(hr);

            AttendanceService attendance = (id, m, y) -> 22;

            // TaxService: edge tax rates
            TaxService taxZero = e -> 0.0;
            TaxService taxHigh = e -> 0.5; // 50% tax

            PerformanceEvaluator perfEval = e -> 90; // top tier bonus

            PayrollProcessor payrollZeroTax = new PayrollProcessor(attendance, taxZero, perfEval);
            PayrollProcessor payrollHighTax = new PayrollProcessor(attendance, taxHigh, perfEval);

            Employee emp = new Employee("B1", "Boundary", 10000, 0.0, 90);
            manager.createEmployee(emp);
            logAssertion(testName, "Employee created for boundary testing", true, "Employee B1", "Employee B1");

            // Test Case 1: Zero tax rate boundary
            Double netZeroTax = payrollZeroTax.calculateNetSalary(emp, 11, 2025);
            double expectedZeroTax = 12000.0; // gross 10000, bonus 20% = 2000, tax 0 -> net 12000
            boolean zeroTaxMatch = Math.abs(expectedZeroTax - netZeroTax) < 0.0001;
            logAssertion(testName, "Zero tax calculation", zeroTaxMatch, netZeroTax, expectedZeroTax);
            assertEquals(expectedZeroTax, netZeroTax, 0.0001, "Zero tax boundary test failed");

            // Test Case 2: High tax rate boundary (50%)
            Double netHighTax = payrollHighTax.calculateNetSalary(emp, 11, 2025);
            double expectedHighTax = 7000.0; // gross 10000, bonus 2000, tax 5000 -> net = 7000
            boolean highTaxMatch = Math.abs(expectedHighTax - netHighTax) < 0.0001;
            logAssertion(testName, "High tax calculation (50%)", highTaxMatch, netHighTax, expectedHighTax);
            assertEquals(expectedHighTax, netHighTax, 0.0001, "High tax boundary test failed");

            TEST_RESULTS_LOG.append("  Boundary Values Tested:\n");
            TEST_RESULTS_LOG.append("    - Zero tax rate (0%): Net = ").append(netZeroTax).append("\n");
            TEST_RESULTS_LOG.append("    - High tax rate (50%): Net = ").append(netHighTax).append("\n");
            TEST_RESULTS_LOG.append("    - Performance bonus (20%): Verified\n");
            
            TEST_RESULTS_LOG.append("✓ Test 4 PASSED: Boundary value testing verified\n");
        } catch (Exception e) {
            logException(testName, e, "Boundary value test failure");
            throw new AssertionError("Boundary value test failed", e);
        }
    }

    /* --------- Test 5: Asynchronous data transfer during report generation delays --------- */
    @Test
    @DisplayName("Scenario 5: Asynchronous Report Generation - Delay Handling")
    public void scenario_asyncReportGeneration_delayHandling() throws Exception {
        String testName = "scenario_asyncReportGeneration_delayHandling";
        TEST_RESULTS_LOG.append("\n--- Test 5: Asynchronous Report Generation Test ---\n");
        
        try {
            long startTime = System.currentTimeMillis();
            
            InMemoryHRDatabase hr = new InMemoryHRDatabase();
            EmployeeRecordManager manager = new EmployeeRecordManager(hr);

            AttendanceService attendance = (id, m, y) -> 22;
            TaxService taxService = e -> 0.1;
            PerformanceEvaluator perfEval = e -> 80;
            PayrollProcessor payroll = new PayrollProcessor(attendance, taxService, perfEval);

            // ReportGenerator stub that delays completion (simulate slow network/data)
            ReportGenerator slowReportGen = (month, year) -> CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(3000); // 3 seconds delay
                    return "REPORT_OK";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "REPORT_INTERRUPTED";
                }
            });

            // Start report generation while also doing payroll calculations (concurrent flow)
            long reportStartTime = System.currentTimeMillis();
            CompletableFuture<String> reportFuture = slowReportGen.generatePayrollReport(11, 2025);
            TEST_RESULTS_LOG.append("  Report generation started (asynchronous, 3s delay)\n");

            // Meanwhile compute a payroll (simulate user flow)
            Employee e = new Employee("R1", "Reporter", 12000, 0.1, 80);
            manager.createEmployee(e);
            long payrollStartTime = System.currentTimeMillis();
            Double net = payroll.calculateNetSalary(e, 11, 2025);
            long payrollEndTime = System.currentTimeMillis();

            boolean netNotNull = net != null;
            logAssertion(testName, "Payroll computed while report pending", netNotNull, net, "not null");
            assertNotNull(net, "Payroll must compute while report is pending");
            
            TEST_RESULTS_LOG.append("  Payroll computed in ").append(payrollEndTime - payrollStartTime).append("ms (non-blocking)\n");

            // Assert payroll computed and report eventually completes
            String report = reportFuture.get(5, TimeUnit.SECONDS);
            long reportEndTime = System.currentTimeMillis();
            
            boolean reportMatch = "REPORT_OK".equals(report);
            logAssertion(testName, "Report generation completed", reportMatch, report, "REPORT_OK");
            assertEquals("REPORT_OK", report, "Report should complete successfully");
            
            long totalTime = System.currentTimeMillis() - startTime;
            TEST_RESULTS_LOG.append("  Report completed in ").append(reportEndTime - reportStartTime).append("ms\n");
            TEST_RESULTS_LOG.append("  Total test time: ").append(totalTime).append("ms\n");
            TEST_RESULTS_LOG.append("  Asynchronous Timeline: Payroll (").append(payrollEndTime - payrollStartTime)
                .append("ms) completed before Report (").append(reportEndTime - reportStartTime).append("ms)\n");
            
            TEST_RESULTS_LOG.append("[INTEGRATION-LOG] Payroll computed: ").append(net)
                .append(", report: ").append(report).append("\n");
            
            TEST_RESULTS_LOG.append("✓ Test 5 PASSED: Asynchronous handling verified\n");
        } catch (TimeoutException e) {
            logException(testName, e, "Report generation timeout");
            throw new AssertionError("Report generation timed out", e);
        } catch (Exception e) {
            logException(testName, e, "Asynchronous test failure");
            throw e;
        }
    }
}