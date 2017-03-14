package com.citrus.slam.util;

import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.example.slam_mahonyahrs.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by liming on 17-3-14.
 */
public class CalibrationHelper {

    static private float[] params = new float[5];

    static {
        CalibrationHelper.loadCalibrationParams();
    }

    static private void loadCalibrationParams()
    {
        if (params == null)
            params = new float[5];

        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File file = new File(sdcard, "/calibration/calibration.txt");

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            Log.i("CalibrationHelper", "Read file content: " + line);
            String[] numbers = line.split("\\s");
            Log.i("CalibrationHelper", "Split result: " + numbers.length + " " + numbers[0] + " " + numbers[1] + " " + numbers[2]);

            params[0] = Float.parseFloat(numbers[0].trim());
            params[1] = Float.parseFloat(numbers[1].trim());
            params[2] = Float.parseFloat(numbers[2].trim());
            params[3] = Float.parseFloat(numbers[3].trim());
            params[4] = Float.parseFloat(numbers[4].trim());
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("CalibrationHelper", "Load calibration params from file, "
                + params[0] + " " + params[1] + " " + params[2] + " " + params[3] + " " + params[4]);
    }

    /**
     * Get projection matrix from pre-calibrated intrinsic
     * @param w The width of camera preview in pixel
     * @param h The height of camera preview in pixel
     * @param near Near plane of OpenGL camera
     * @param far Far plane of OpenGL camera
     * @return 4x4 matrix , OpenGL Projection matrix
     */
    static public float[] getProjectionMatrix(float w, float h, float near, float far)
    {
        float res[] = new float[16];
        ProjectionMatrixRUB_BottomLeft(res, w, h, params[0]*w, params[1]*h,
                params[2]*w, params[3]*h, near, far);

        Log.d("CalibrationHelper", "Get Projection Matrix, w: "
                + w + " h: " + h + " fx: " + params[0]*w + " fy: " + params[1]*h
                + " cx: " + params[2]*w + " cy: " + params[3]*h);

        return res;
    }


    // Camera Axis:
    //   X - Right, Y - Up, Z - Back
    // Image Origin:
    //   Bottom Left
    // Caution: Principal point defined with respect to image origin (0,0) at
    //          top left of top-left pixel (not center, and in different frame
    //          of reference to projection function image)

    static private void ProjectionMatrixRUB_BottomLeft(float[] P, float w, float h, float fu, float fv,
                                        float u0, float v0, float zNear, float zFar )
    {
        // http://www.songho.ca/opengl/gl_projectionmatrix.html
        final float L = +(u0) * zNear / -fu;
        final float T = +(v0) * zNear / fv;
        final float R = -(w-u0) * zNear / -fu;
        final float B = -(h-v0) * zNear / fv;

        for (int i = 0; i < 16; i++ ) {
            P[i] = 0;
        }

        P[0*4+0] = 2 * zNear / (R-L);
        P[1*4+1] = 2 * zNear / (T-B);
        P[2*4+2] = -(zFar +zNear) / (zFar - zNear);
        P[2*4+0] = (R+L)/(R-L);
        P[2*4+1] = (T+B)/(T-B);
        P[2*4+3] = -1.0f;
        P[3*4+2] =  -(2*zFar*zNear)/(zFar-zNear);

    }

}
