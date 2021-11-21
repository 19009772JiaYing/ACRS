package com.google.android.gms.samples.vision.ocrreader;

public class Members {
    String memId;
    String memName;
    String memContact;
    String memRole;
    String memLicense;
    String memCompany;

    public Members(){

    }
    public Members(String memId, String memName, String memContact, String memRole, String memLicense, String memCompany) {
        this.memId = memId;
        this.memName = memName;
        this.memContact = memContact;
        this.memRole = memRole;
        this.memLicense = memLicense;
        this.memCompany = memCompany;
    }

    public String getMemId() {
        return memId;
    }

    public String getMemName() {
        return memName;
    }

    public String getMemContact() {
        return memContact;
    }

    public String getMemRole() {
        return memRole;
    }

    public String getMemLicense() {
        return memLicense;
    }

    public String getMemCompany() {
        return memCompany;
    }
}
