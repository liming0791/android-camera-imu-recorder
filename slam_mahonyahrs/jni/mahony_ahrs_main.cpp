#include <sys/time.h>

#include <string.h>
#include <iostream>     
#include <fstream>      
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>

#include "MahonyAHRS.h"

using namespace std;

extern "C"{

struct timeval tstart;
struct timeval tend;

static bool isFirstUpdate = true;
static int count = 0;

// Init
void Init(float* pimuval, float* q)
{
    MahonyAHRS::init(pimuval[0], pimuval[1], pimuval[2], q);
}

// Reset
JNIEXPORT void JNICALL
Java_com_citrus_slam_MahonyAHRS_QuaternionSensor_nativeReset( JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_INFO, "JNIMsg", "JNI nativeReset called");
    MahonyAHRS::reset();
    isFirstUpdate = true;
    count = 0;
}

// Update
JNIEXPORT void JNICALL
Java_com_citrus_slam_MahonyAHRS_QuaternionSensor_nativeUpdateIMU( JNIEnv* env, jobject thiz,
        jfloatArray imuval, jfloatArray q)
{

    float* pimuval = env->GetFloatArrayElements(imuval, 0);
    float* pq = env->GetFloatArrayElements(q, 0);

    float imufreq = 50.f;      // the frequency is important

    if (isFirstUpdate) {
        gettimeofday(&tstart, 0);
        isFirstUpdate = false;
        Init(pimuval, pq);
        __android_log_print(ANDROID_LOG_INFO, "JNIMsg",
                "JNI Init IMU called, init q : %f %f %f %f", pq[0], pq[1], pq[2], pq[3]);
    } else {
        // caculate the frequency
        gettimeofday(&tend, 0);
        imufreq = 1 / (((tend.tv_sec - tstart.tv_sec)*1000000u + tend.tv_usec - tstart.tv_usec)/1000000.f);
        gettimeofday(&tstart, 0);
    }

    MahonyAHRS::updateIMU(pimuval[3], pimuval[4], pimuval[5], pimuval[0], pimuval[1], pimuval[2],
            imufreq, pq[0], pq[1], pq[2], pq[3]);

    count++;
    if (count==100) {
        __android_log_print(ANDROID_LOG_INFO, "JNIMsg", "JNI nativeUpdateIMU called,"
                "imufreq: %f, q : %f %f %f %f", imufreq, pq[0], pq[1], pq[2], pq[3]);
        count=0;
    }

    env->ReleaseFloatArrayElements(imuval, pimuval, 0);
    env->ReleaseFloatArrayElements(q, pq, 0);
}

}
