LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cpp
LOCAL_CFLAGS += -std=c++11
LOCAL_CPPFLAGS += -std=c++11
LOCAL_MODULE    := MahonyAHRS

LOCAL_C_INCLUDES += $(LOCAL_PATH)/MahonyAHRS
AHRS_PATH := ./MahonyAHRS
LOCAL_SRC_FILES += \
$(AHRS_PATH)/MahonyAHRS.cpp         \
./mahony_ahrs_main.cpp            

LOCAL_LDLIBS    += -llog -landroid 
LOCAL_CFLAGS += -g

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES) #export includes
LOCAL_EXPORT_LDLIBS := $(LOCAL_LDLIBS) #export linker cmds
LOCAL_EXPORT_CFLAGS := $(LOCAL_CFLAGS) #export c flgs
LOCAL_EXPORT_CPPFLAGS := $(LOCAL_CPPFLAGS) #export cpp flgs
LOCAL_EXPORT_CXXFLAGS := $(LOCAL_CXXFLAGS) #export cpp flgs

include $(BUILD_SHARED_LIBRARY)
