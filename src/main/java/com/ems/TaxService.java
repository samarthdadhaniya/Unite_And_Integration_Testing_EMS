package com.ems;

@FunctionalInterface
public interface TaxService {
    Double calculateTax(Employee employee);
}