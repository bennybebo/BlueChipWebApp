package com.bluechip.demo.model;

public class Outcome {
    private String name; // e.g. (Over/Under)
    private int price; // e.g. (+150/-150)
    private double point; // e.g. 57.5 as in O/U 57.5 points

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public double getPoint() {
        return point;
    }

    public void setPoint(double point) {
        this.point = point;
    }
}
