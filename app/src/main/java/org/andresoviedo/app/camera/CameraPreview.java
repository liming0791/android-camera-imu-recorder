package org.andresoviedo.app.camera;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private CameraManager mCameraManager;

    private Context context;

    public CameraPreview(Context context, CameraManager cameraManager) {
        super(context);

        this.context = context;
        this.mCameraManager = cameraManager;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = this.getHolder();
        mHolder.addCallback(this);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCameraManager.setSurfaceHolder(mHolder);
        mCameraManager.startCamera(context);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

}
