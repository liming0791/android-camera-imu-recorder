#include <sys/time.h>

#include <string.h>
#include <iostream>     
#include <fstream>      
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>

#include <cvd/image.h>
#include <cvd/image_io.h>
#include <cvd/byte.h>
#include <cvd/utility.h>
#include <cvd/convolution.h>
#include <cvd/vision.h>

#include "MahonyAHRS.h"
#include "SmallBlurryImage.h"
#include "ATANCamera.h"

using namespace std;

extern "C"{

struct timeval tstart;
struct timeval tbegin;
struct timeval tend;

static bool isFirstUpdate = true;
static int imuCount = 0;

static bool isFirstImage = true;
CVD::Image<CVD::byte> imageData;
CVD::ImageRef SBISize;
TooN::SO3<> pose_reset;
float state_reset[7];
int frameCount = 0;

SmallBlurryImage SBI;
SmallBlurryImage SI;
int countFromCorrection = 0;

/*
* Code for imu pose.
*/

// Init imu pose
void InitIMU(float* pimuval, float* q)
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
    isFirstImage = true;
    imuCount = 0;
    frameCount = 0;
}

// Update By IMU
JNIEXPORT void JNICALL
Java_com_citrus_slam_MahonyAHRS_QuaternionSensor_nativeUpdateIMU( JNIEnv* env, jobject thiz,
        jfloatArray imuval, jfloatArray q)
{

    float* pimuval = env->GetFloatArrayElements(imuval, 0);
    float* pq = env->GetFloatArrayElements(q, 0);

    float imufreq = 50.f;      // the frequency is important

    if (isFirstUpdate) {        // if first update imu, init it
        gettimeofday(&tstart, 0);
        gettimeofday(&tbegin, 0);
        isFirstUpdate = false;
        InitIMU(pimuval, pq);
        __android_log_print(ANDROID_LOG_INFO, "JNIMsg",
                "JNI Init IMU called, init q : %f %f %f %f", pq[0], pq[1], pq[2], pq[3]);
    } else {

        gettimeofday(&tend, 0);
        imufreq = 1 / (((tend.tv_sec - tstart.tv_sec)*1000000u
                + tend.tv_usec - tstart.tv_usec)/1000000.f);        // caculate the frequency
        gettimeofday(&tstart, 0);
    }

    // == test save imu data
    static FILE *imufile = fopen("/sdcard/imu_data/imu.csv","w");
    struct timeval tnow;
    gettimeofday(&tnow, 0);
    unsigned long timeStamp = ( (tnow.tv_sec - tbegin.tv_sec) * 1000000u + (tnow.tv_usec - tbegin.tv_usec) );
    fprintf(imufile, "%09lu000,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f\n", timeStamp,
            pimuval[3], pimuval[4], pimuval[5], pimuval[0], pimuval[1], pimuval[2]);

    __android_log_print(ANDROID_LOG_INFO, "JNIMsg",
                    "JNI nativeUpdateVision called,"
                    "save imu: %lu",
                    timeStamp);
    // == done

    env->ReleaseFloatArrayElements(imuval, pimuval, 0);
    env->ReleaseFloatArrayElements(q, pq, 0);
}

/*
* Following code is for vision correction.
*/

// Reset Vidsion
JNIEXPORT void JNICALL
Java_com_citrus_slam_MahonyAHRS_QuaternionSensor_nativeResetVision( JNIEnv* env, jobject thiz)
{
    isFirstImage = true;
}

// Update by vision
JNIEXPORT void JNICALL
Java_com_citrus_slam_MahonyAHRS_QuaternionSensor_nativeUpdateVision( JNIEnv* env, jobject thiz,
        jbyteArray imageArray, jint width, jint height)
{

    if (isFirstUpdate) return;      // if has not do imu update,
                                    // return

    // get image array data
    int len = env->GetArrayLength(imageArray);
    imageData.resize(CVD::ImageRef(width, height));
    env->GetByteArrayRegion(imageArray, 0, width*height, (jbyte*)imageData.data() );

    // == test save image
    static FILE *imagefile = fopen("/sdcard/camera_images/images.bin","wb");
    struct timeval tnow;
    gettimeofday(&tnow, 0);
    unsigned long timeStamp = ( (tnow.tv_sec - tbegin.tv_sec) * 1000000u + (tnow.tv_usec - tbegin.tv_usec) );
    fwrite(&timeStamp, sizeof(unsigned long), 1, imagefile);
    fwrite(&width, sizeof(int), 1, imagefile);
    fwrite(&height, sizeof(int), 1, imagefile);
    fwrite(imageData.data(), sizeof(unsigned char), width*height, imagefile);

    __android_log_print(ANDROID_LOG_INFO, "JNIMsg",
                    "JNI nativeUpdateVision called,"
                    "save image: %lu",
                    timeStamp);
    // == done

}

}
