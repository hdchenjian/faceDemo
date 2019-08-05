package com.example.luyao.myapplication;

import android.graphics.Bitmap;

public class GlobalParameter {
    private static int organization_id;
    private static String sid;
    private static boolean sessionExpired;
    private static int[] registration_image;
    private static Bitmap registration_image_bitmap;

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

    public static int[] getRegistration_image() {
        return registration_image;
    }

    public static void setRegistration_image(int[] registration_image) {
        GlobalParameter.registration_image = registration_image;
    }

    public static Bitmap getRegistration_image_bitmap() {
        return registration_image_bitmap;
    }

    public static void setRegistration_image_bitmap(Bitmap registration_image_bitmap) {
        GlobalParameter.registration_image_bitmap = registration_image_bitmap;
    }
}
