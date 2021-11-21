package com.google.android.gms.samples.vision.ocrreader;

public class AuthorizedLog {

    String carOwnerID;
    String carOwnerName;
    String ownerLP;
    String ownerDesig;
    String currentDate;
    String currentTime;

    public AuthorizedLog(){

    }

    public AuthorizedLog(String carOwnerID, String carOwnerName, String ownerLP, String ownerDesig, String currentDate, String currentTime) {
        this.carOwnerID = carOwnerID;
        this.carOwnerName = carOwnerName;
        this.ownerLP = ownerLP;
        this.ownerDesig = ownerDesig;
        this.currentDate = currentDate;
        this.currentTime = currentTime;
    }

    public String getCarOwnerID() {
        return carOwnerID;
    }

    public String getcarOwnerName() {
        return carOwnerName;
    }

    public String getOwnerLP() {
        return ownerLP;
    }

    public String getOwnerDesig() {
        return ownerDesig;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public String getCurrentTime() {
        return currentTime;
    }
}
