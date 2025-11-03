package com.ems;

import java.util.concurrent.CompletableFuture;

public interface ReportGenerator {
    CompletableFuture<String> generatePayrollReport(int month, int year);
}