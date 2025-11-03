package com.ems;

@FunctionalInterface
public interface AttendanceService {
    int getDaysPresent(String employeeId, int month, int year);
}