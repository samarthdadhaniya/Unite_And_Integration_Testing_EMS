package com.ems;

public class Employee {
    private String id;
    private String name;
    private double baseSalary;
    private double bonusRate;
    private int performanceScore;

    public Employee(String id, String name, double baseSalary, double bonusRate, int performanceScore) {
        this.id = id;
        this.name = name;
        this.baseSalary = baseSalary;
        this.bonusRate = bonusRate;
        this.performanceScore = performanceScore;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getBaseSalary() { return baseSalary; }
    public double getBonusRate() { return bonusRate; }
    public int getPerformanceScore() { return performanceScore; }
}