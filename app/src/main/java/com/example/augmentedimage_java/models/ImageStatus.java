package com.example.augmentedimage_java.models;

public class ImageStatus {
    private String name;
    private boolean isDetected;

    public ImageStatus(String name, boolean isDetected) {
        this.name = name;
        this.isDetected = isDetected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDetected() {
        return isDetected;
    }

    public void setDetected(boolean detected) {
        isDetected = detected;
    }
}
