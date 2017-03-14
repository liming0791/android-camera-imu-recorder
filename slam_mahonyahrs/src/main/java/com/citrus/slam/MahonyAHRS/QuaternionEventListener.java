package com.citrus.slam.MahonyAHRS;

/**
 * Created by liming on 17-2-8.
 */
public interface QuaternionEventListener {

    /**
     * When a new quaternion is computed, this method will be called as a callback
     * @param q The quaternion representing pose, 4 elements float array as <w, x, y, z>
     */
    public void onQuaternionEvent(float[] q);
}
