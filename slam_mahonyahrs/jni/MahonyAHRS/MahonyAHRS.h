//=====================================================================================================
// MahonyAHRS.h
//=====================================================================================================
//
// Madgwick's implementation of Mayhony's AHRS algorithm.
// See: http://www.x-io.co.uk/node/8#open_source_ahrs_and_imu_algorithms
//
// Date			Author			Notes
// 29/09/2011	SOH Madgwick    Initial release
// 02/10/2011	SOH Madgwick	Optimised for reduced CPU load
//
//=====================================================================================================
#ifndef MahonyAHRS_h
#define MahonyAHRS_h


// Function declarations

class MahonyAHRS
{
    public:
        static void init(float ax, float ay, float az, float* q);
        static void reset();
        static void update(float gx, float gy, float gz, 
                float ax, float ay, float az, 
                float mx, float my, float mz,
                float sampleFreq,
                float& _q0, float& _q1, float& _q2, float& _q3);
        static void updateIMU(float gx, float gy, float gz, 
                float ax, float ay, float az,
                float sampleFreq,
                float& _q0, float& _q1, float& _q2, float& _q3);

    private:
        static float q0, q1, q2, q3;
        static float integralFBx, integralFBy, integralFBz;
        static float twoKi, twoKp;
        static float invSqrt(float x);
        
};

#endif
//=====================================================================================================
// End of file
//=====================================================================================================
