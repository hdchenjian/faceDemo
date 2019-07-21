package com.iim.recognition.caffe;

public class LoadLibraryModule {
    private static LoadLibraryModule loadLibraryModule;

    public native boolean recognition_start();
    public native boolean recognition_stop();
    public native int recognition_face(byte[] image_data, int[] face_region,
                                       float[] feature, long code_ret[]);

    public native int[] rgb2bitmap_native(byte[] data);
    public native byte[] rgb2jpg_native(byte[] data, int width, int height);
    public native byte[] yuv2rgb_native(byte[] yuv, int width, int height);
    public native byte[] yv122rgb_native(byte[] yuv, int width, int height);
    public native int recognition_face(byte[] image_data, int[][] face_region,
                                       float[][] feature, int model_version, long code_ret[]);

    public LoadLibraryModule()
    {
        System.loadLibrary("dnn_network");
    }

    public static LoadLibraryModule getInstance(){
        if (loadLibraryModule == null) {
            synchronized (LoadLibraryModule.class) {
                if (loadLibraryModule == null) {
                    loadLibraryModule = new LoadLibraryModule();
                }
            }
        }
        return  loadLibraryModule;
    }
}
