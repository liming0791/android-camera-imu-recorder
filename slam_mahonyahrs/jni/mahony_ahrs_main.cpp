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

bool isFirstUpdate = true;

int count = 0;

//init PTAM
JNIEXPORT void JNICALL
Java_com_citrus_slam_MahonyAHRS_QuaternionSensor_nativeReset( JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_INFO, "JNIMsg", "JNI nativeReset called");
    MahonyAHRS::reset();
}

//clean up
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
    } else {
        // caculate the frequency
        gettimeofday(&tend, 0);
        imufreq = 1 / (((tend.tv_sec - tstart.tv_sec)*1000000u + tend.tv_usec - tstart.tv_usec)/1000000.f);
        gettimeofday(&tstart, 0);
    }

    MahonyAHRS::updateIMU(pimuval[3], pimuval[4], pimuval[5], pimuval[0], pimuval[1], pimuval[2],
            imufreq, pq[0], pq[1], pq[2], pq[3]);

    env->ReleaseFloatArrayElements(imuval, pimuval, 0);
    env->ReleaseFloatArrayElements(q, pq, 0);

    count++;
    if (count==500) {
        __android_log_print(ANDROID_LOG_INFO, "JNIMsg", "JNI nativeUpdateIMU called, imufreq: %f", imufreq);
        count=0;
    }
}

}
