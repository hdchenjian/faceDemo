package com.example.luyao.myapplication;

public class GlobalParameter {
    private static int organization_id;
    private static String sid;
    private static boolean sessionExpired;

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
}
