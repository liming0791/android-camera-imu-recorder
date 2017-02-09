//Copyright 2015 ICGJKU
package org.andresoviedo.app.camera;

import static android.opengl.GLES20.glGenTextures;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;

public class CameraManager implements Camera.PreviewCallback, SensorEventListener {

    public CameraManager(SharedPreferences sp)
    {
        preferences = sp;
    }

    //inform main application about new frame
    private FrameListener frameListener;

    public interface FrameListener {
        public void onFrameReady();
    }

    public void setFrameListener(FrameListener frameListener) {
        this.frameListener = frameListener;
    }

    SharedPreferences preferences;

    private int curpos = 0;
    private static int capacity = 200;
    private PersistentSensorEvent[] sensorHistory = new PersistentSensorEvent[capacity];

    public List<String> wbmodes;

    private Object cameraLock = new Object();
    private Object frameLock = new Object();

    private Camera camera;
    private CameraFrame frame;

    private SurfaceTexture renderTexture;

    private SurfaceHolder mHolder;

    //private RateCounter FPS = new RateCounter();

    private Size frameSize;

    private SensorManager sensorManager = null;
    private float[] rotMat = new float[9];
    private long frametimeoffset = -1;



    public void IncOffset()
    {
        frametimeoffset += 10*1000000;
        Log.d("inc offset", ""+frametimeoffset);
    }

    public void DecOffset()
    {
        frametimeoffset -= 10*1000000;
        Log.d("dec offset", ""+frametimeoffset);
    }

    public Size GetSize()
    {
        return frameSize;
    }

    private int GetBackFacingCamera()
    {
        //search for back facing camera
        int numCams = Camera.getNumberOfCameras();

        CameraInfo camInfo = new CameraInfo();
        for(int i = 0; i < numCams; i++)
        {
            Camera.getCameraInfo(i, camInfo);
            if (camInfo.facing == CameraInfo.CAMERA_FACING_BACK)
                return i;
        }
        Log.w("Camera", "No back facing camera.");

        return 0;
    }

    public boolean IsCameraStarted()
    {
        return camera!=null;
    }

    public void startCamera(Context context) {
        synchronized (cameraLock)
        {
            if(camera!=null) return; // already started

            //init sensor stuff
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

            //sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                Log.w("Sensor", "Android Version >= 4.3 detected, try using game rotation vector!");
                Sensor s = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
                if(s!=null)
                {
                    sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST);
                }
                else
                {
                    Log.w("Sensor", "Game rotation not available, using old rotation vector!");
                    //sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
                }
            }
            else
            {
                Log.w("Sensor", "Android Version < 4.3 detected, using old rotation vector!");
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
            }

            //search for back facing camera
            int camId = GetBackFacingCamera();

            //open camera
            camera = Camera.open(camId);
            if (camera == null)
                throw new RuntimeException("Could not open camera.");


            //setup camera

            //debug
            Camera.Parameters camparams = camera.getParameters();
            Log.i("Camera", "Camera params:" + camparams.flatten());

            int imageFormat = ImageFormat.NV21; //should be supported on almost any device

            //find fastest supported framerate
            List<int[]> supportedFPSRanges = camparams.getSupportedPreviewFpsRange();
            int[] fpsRange = supportedFPSRanges.get(supportedFPSRanges.size() - 1);

            int pwidth = 1920;
            int pheight = 1080;
			
			/*Log.d("BT200", Build.MODEL);
			if(Build.MODEL.equalsIgnoreCase("embt2"))
			{
				Log.d("BT200", "low resolution");
				pwidth = 640;
				pheight = 480;
			}*/


            Log.i("Camera", "Best FPS range: " + fpsRange[0] + "-" + fpsRange[1]);
            Log.i("Camera", "Size: " + pwidth + "x" + pheight);

            //List<Size> csizes = camera.getParameters().getSupportedPreviewSizes();

            wbmodes = camparams.getSupportedWhiteBalance();

            camparams.setPreviewSize(pwidth, pheight);
            camparams.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
            camparams.setPreviewFormat(imageFormat);

            if (camparams.isVideoStabilizationSupported())
                camparams.setVideoStabilization(false);

            //if(camparams.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            //	camparams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

            if(camparams.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED))
                camparams.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);

            if(camparams.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_INFINITY))
                camparams.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);

            if(!camparams.isAutoExposureLockSupported())
                Log.w("Camera", "No auto exposure lock!");

            if(camparams.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_OFF))
                camparams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

            String wbmode = preferences.getString("wbmode", "");
            if(!wbmode.equals(""))
            {
                camparams.setWhiteBalance(wbmode);
            }

            //set parameters
            camera.setDisplayOrientation(90);
            camera.setParameters(camparams);

            //create buffers for camera frames
            Size previewFrameSize = camera.getParameters().getPreviewSize();
            Log.i("actual size: ", ""+previewFrameSize.width+"x"+previewFrameSize.height);

            //int test = camera.getParameters().getPreviewFormat();
            frameSize = previewFrameSize;
            int bPP = ImageFormat.getBitsPerPixel(imageFormat);
            int bufferSize = (int) (previewFrameSize.height * previewFrameSize.width * ((double)bPP / 8.0) * 1.5);

            camera.addCallbackBuffer(new byte[bufferSize]);
            camera.addCallbackBuffer(new byte[bufferSize]);
            camera.addCallbackBuffer(new byte[bufferSize]);
            //camera.addCallbackBuffer(new byte[bufferSize]);


            camera.setPreviewCallbackWithBuffer(this);
            //camera.setPreviewCallback(this);


            if(renderTexture!=null)
            {
                try {
                    camera.setPreviewTexture(renderTexture);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (mHolder!=null) {
                Log.i("CameraManager", "setPreviewDisplay");
                try {
                    camera.setPreviewDisplay(mHolder);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stopCamera() {
        synchronized (cameraLock)
        {
            if(camera!=null)
            {
                camera.stopPreview();
                camera.release();
                camera = null;

                sensorManager.unregisterListener(this);
            }
        }
    }

    public void createRenderTexture() {
        synchronized (cameraLock)
        {
            int[] tempt = new int[1];
            glGenTextures(1, tempt, 0);
            int texture = tempt[0];

            renderTexture = new SurfaceTexture(texture);
            if(camera!=null)
            {
                try {
                    camera.setPreviewTexture(renderTexture);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setSurfaceHolder(SurfaceHolder mHolder) {
        synchronized (cameraLock) {
            this.mHolder = mHolder;
            if (camera != null) {
                try {
                    camera.setPreviewDisplay(this.mHolder);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void UpdateWhiteBalanceMode()
    {
        synchronized (cameraLock)
        {
            if(camera!=null)
            {
                String wbmode = preferences.getString("wbmode", "");
                if(!wbmode.equals(""))
                {
                    Camera.Parameters params = camera.getParameters();
                    params.setWhiteBalance(wbmode);
                    camera.setParameters(params);
                }
            }
        }
    }

    public void FixCameraSettings()
    {

        synchronized (cameraLock)
        {
            if(camera!=null)
            {
                //camera.stopPreview();
                Camera.Parameters params = camera.getParameters();

                boolean dolock = preferences.getBoolean("exposurelock", true);
                if(params.isAutoExposureLockSupported())
                    params.setAutoExposureLock(dolock);

                //params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);

                if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_INFINITY))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);

                //if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO))
                //	params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                //params.setAutoWhiteBalanceLock(true);

                camera.setParameters(params);

                camera.autoFocus(null); //todo show message to user to hold camera still until focus done?
                //camera.startPreview();
                Log.d("Camera", "Camera Settings Fixed!");

                Log.d("Camera","Exposure: "+params.getExposureCompensation());
                Log.d("Camera","WB: "+params.getWhiteBalance());
                Log.d("Camera","Scene: "+params.getSceneMode());

                //hack
                resetdiff=true;
            }
        }
    }

    public void changeBrightness(int change)
    {
        synchronized (cameraLock)
        {
            if(camera!=null)
            {
                //todo check if supported
                Camera.Parameters params = camera.getParameters();
                Log.d("Camera","Changed Brightness from: "+params.getExposureCompensation());
                int curex = params.getExposureCompensation()+change;
                if(curex >= params.getMinExposureCompensation() && curex <= params.getMaxExposureCompensation())
                    params.setExposureCompensation(curex);
                if(change!=0)
                    params.setAutoExposureLock(false);
                else
                    params.setAutoExposureLock(true);
                Log.d("Camera","Changed Brightness to: "+params.getExposureCompensation());
                camera.setParameters(params);
            }
        }
    }

    public boolean isCameraImageReady()
    {
        return frame!=null;
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        long ts = System.nanoTime();

        PersistentSensorEvent se = getClosestEvent(ts-frametimeoffset);
        SensorManager.getRotationMatrixFromVector( rotMat , se.values);
        //float[] currot = Arrays.copyOf(rotMat, 9);//accurate??

        synchronized (frameLock) {
            if (frame != null) {
                freeCameraFrame(frame);
            }
            frame = new CameraFrame(data, ts, rotMat);
        }

        if(frameListener!=null)
            frameListener.onFrameReady();

    }

    public CameraFrame getCameraFrame() {
        CameraFrame curframe = null;
        synchronized (frameLock) {
            if (frame != null) {
                curframe = frame;
                frame = null;
            }
        }
        return curframe;
    }

    public void freeCameraFrame(CameraFrame curframe) {
        synchronized (cameraLock) {
            if (camera != null) {
                camera.addCallbackBuffer(curframe.imdata);
            }
        }
    }

    long lastts=0;

    private PersistentSensorEvent getClosestEvent(long ts)
    {

        int spos = curpos;
        int cpos = (capacity+spos-1)%capacity;
        while(cpos != spos && sensorHistory[cpos]!= null && sensorHistory[cpos].timestamp > ts)
            cpos = (capacity+cpos-1)%capacity;

        if(sensorHistory[cpos]==null)
        {
            PersistentSensorEvent se = new PersistentSensorEvent();
            se.values = new float[3];
            return se;
        }
		
		/*if(spos > cpos)
			Log.d("diff pos", ""+(spos-cpos));
		else
			Log.d("diff pos", ""+(capacity+spos-cpos));*/

        if(cpos==(capacity+spos-1)%capacity || Math.abs(sensorHistory[cpos].timestamp-ts) < Math.abs(sensorHistory[(cpos+1)%capacity].timestamp-ts))
            return sensorHistory[cpos];
        else
            return sensorHistory[(cpos+1)%capacity];
    }

    //more sensor stuff
    BigInteger stsdiffsum = BigInteger.ZERO;
    long nummeas = 0;
    long stsdiff = 0;

    Vector<Long> diffs = new Vector<Long>(1000);

    int baddiffcount = 0;

    boolean resetdiff = false;

    @Override
    public void onSensorChanged(SensorEvent event) {
        //long diff = event.timestamp - lastts;
        //lastts = event.timestamp;
        //Log.d("TS Sensor", ""+(diff*0.000001));
        if(nummeas<1000)
        {
            long sts = System.nanoTime();
            //Log.d("timing", ""+sts+","+event.timestamp);
            sts = event.timestamp-sts;
            //stsdiffsum = stsdiffsum.add(BigInteger.valueOf(sts));
            nummeas++;
            diffs.add(sts);
            Collections.sort(diffs);
            stsdiff = diffs.get((int)((diffs.size()-1.0)/2.0));
            //stsdiffsum.divide(BigInteger.valueOf(nummeas)).longValue();

            if(nummeas==1000)
                Log.d("TS Sensor", "time calib done...");
        }
        else
        {
            if(Math.abs(System.nanoTime()-(event.timestamp-stsdiff))>(10*1000000))
                baddiffcount++;
            else
                baddiffcount=0;
        }
        if(baddiffcount>50 || resetdiff)
        {
            resetdiff=false;
            diffs.clear();
            nummeas=0;
            baddiffcount=0;
            Log.w("PTAM-Sensormanager","Sensor time out of sync! Reset...");
        }
        PersistentSensorEvent se = new PersistentSensorEvent();
        se.accuracy = event.accuracy;
        se.timestamp = event.timestamp-stsdiff;
        se.values = event.values.clone();
        sensorHistory[curpos] = se;
        curpos = (curpos+1)%capacity;
        //SensorManager.getRotationMatrixFromVector( rotMat , event.values);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

}