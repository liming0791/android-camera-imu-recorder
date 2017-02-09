//Copyright 2015 ICGJKU
package org.andresoviedo.app.camera;

public class VideoSource {

    private CameraManager camera;
    private CameraFrame curFrame;

    public VideoSource(CameraManager ch) {
        camera = ch;
        curFrame = null;
    }

    public CameraManager getCamera() {
        return camera;
    }

    public byte[] getFrame() {
        if(curFrame!=null)
        {
            camera.freeCameraFrame(curFrame);
            curFrame = null;
        }

        curFrame = camera.getCameraFrame();

        if(curFrame!=null)
            return curFrame.imdata;
        else
            return null;
    }

    public int[] getSize() {
        int[] size = { camera.GetSize().width, camera.GetSize().height };
        return size;
    }

    public float[] getRotation() {
        //Log.d("test rotmat",""+curFrame.rotationMatrix[0]+","+curFrame.rotationMatrix[1]+","+curFrame.rotationMatrix[2]);
        return curFrame.rotationMatrix;
    }

    public void changeBrightness(int change){
        camera.changeBrightness(change);
    }
}