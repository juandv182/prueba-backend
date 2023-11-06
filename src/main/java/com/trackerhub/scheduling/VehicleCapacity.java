package com.trackerhub.scheduling;

public enum VehicleCapacity {
    CAR(25),
    MOTORCYCLE(8);

    public final int capacity;

    VehicleCapacity(int capacity) {
        this.capacity = capacity;
    }
}
