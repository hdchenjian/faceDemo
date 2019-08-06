package com.iim.recognition.caffe;

public class LoadLibraryModule {
    private static LoadLibraryModule loadLibraryModule;

    public native boolean recognition_start();
    public native boolean recognition_stop();
    public native int recognition_face(byte[] image_data, int[] face_region,
                                       float[] feature, long code_ret[], int width, int height);
    public native int detect_face(byte[] image_data, int[] face_region, int width, int height);
    public native int[] yuv2bitmap_native(byte[] data, int width, int height, int height_out);
    public native byte[] bitmap2rgb_native(int[] data);

    public native int[] rgb2bitmap_native(byte[] data);
    public native byte[] rgb2jpg_native(byte[] data, int width, int height);
    public native byte[] yuv2rgb_native(byte[] yuv, int width, int height);
    public native byte[] yv122rgb_native(byte[] yuv, int width, int height);
    public native int recognition_face(byte[] image_data, int[][] face_region,
                                       float[][] feature, int model_version, long code_ret[]);

    public LoadLibraryModule()
    {
        //System.loadLibrary("c++_shared");
        //System.loadLibrary("dnn_network_neon");

        System.loadLibrary("GLES_mali");
        System.loadLibrary("dnn_network");

        //System.loadLibrary("arm_compute");
        //System.loadLibrary("arm_compute_core");
        //System.loadLibrary("arm_compute_graph");
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
