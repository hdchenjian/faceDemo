package com.example.luyao.myapplication;

import android.graphics.Bitmap;

public class GlobalParameter {
    private static int organization_id;
    private static String sid;
    private static boolean sessionExpired;
    private static Bitmap registration_image;

    public static int getOrganization_id() {
        return organization_id;
    }

    public static void setOrganization_id(int organization_id) {
        GlobalParameter.organization_id = organization_id;
    }

    public static String getSid() {
        return sid;
    }

    public static void setSid(String sid) {
        GlobalParameter.sid = sid;
    }

    public static boolean isSessionExpired() {
        return sessionExpired;
    }

    public static void setSessionExpired(boolean sessionExpired) {
        GlobalParameter.sessionExpired = sessionExpired;
    }

    public static Bitmap getRegistration_image() {
        return registration_image;
    }

    public static void setRegistration_image(Bitmap registration_image) {
        GlobalParameter.registration_image = registration_image;
    }
}
