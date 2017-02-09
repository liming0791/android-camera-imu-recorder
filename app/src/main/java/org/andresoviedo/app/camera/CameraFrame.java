//Copyright 2015 ICGJKU
package org.andresoviedo.app.camera;

import java.util.Arrays;

class CameraFrame {
    public byte[] imdata;
    public long timestamp;
    public float[] rotationMatrix;

    public CameraFrame(byte[] data, long timestamp, float[] rotMat) {
        this.imdata = data;
        this.timestamp = timestamp;
        this.rotationMatrix = Arrays.copyOf(rotMat, 9);
    }
}