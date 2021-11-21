package com.google.android.gms.samples.vision.ocrreader;

public class UnauthorizedLog {

    String carOwnerID;
    String ownerLP;
    String currentDate;
    String currentTime;

    public UnauthorizedLog(){

    }


    public UnauthorizedLog(String carOwnerID , String ownerLP, String currentDate, String currentTime) {
        this.carOwnerID = carOwnerID;
        this.ownerLP = ownerLP;
        this.currentDate = currentDate;
        this.currentTime = currentTime;
    }

    public String getCarOwnerID() {
        return carOwnerID;
    }

    public String getOwnerLP() {
        return ownerLP;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public String getCurrentTime() {
        return currentTime;
    }
}
