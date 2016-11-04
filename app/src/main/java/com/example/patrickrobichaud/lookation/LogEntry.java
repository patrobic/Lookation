package com.example.patrickrobichaud.lookation;

// fundamental building block of logs, each instance represents and entry and contains latitude, longitude and time
public class LogEntry {
    private Double latitude, longitude;
    private String time;

    // default constructor (not used)
    LogEntry() {
        latitude = 0.0;
        longitude = 0.0;
        time = "";
    }

    // parametrized constructor (used for storing and displaying entries)
    LogEntry(Double latitude, Double longitude, String time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    // get functions for private members
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getDate() { return time; }

    // set functions for private members
    public void setLatitude(Double latitude) {this.latitude = latitude; }
    public void setLongitude(Double longitude) {this.longitude = longitude; }
    public void setDate(String time) {this.time = time; }
}