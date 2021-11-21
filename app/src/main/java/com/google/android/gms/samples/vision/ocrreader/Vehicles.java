package com.google.android.gms.samples.vision.ocrreader;

public class Vehicles {
    private String vehId;
    private String vehLicense;
    private String vehBrand;
    private String vehModel;

    public Vehicles(){

    }

    public Vehicles(String vehId, String vehLicense, String vehBrand, String vehModel) {
        this.vehId = vehId;
        this.vehLicense = vehLicense;
        this.vehBrand = vehBrand;
        this.vehModel = vehModel;
    }

    public String getVehId() {
        return vehId;
    }

    public String getVehLicense() {
        return vehLicense;
    }

    public String getVehBrand() {
        return vehBrand;
    }

    public String getVehModel() {
        return vehModel;
    }
}
