LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CFLAGS := 

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS += -DHAVE_NEON=1
    LOCAL_CFLAGS += -DLOCAL_ARM_NEON=1
    #LOCAL_CFLAGS += -D__ARM_NEON__=1
    LOCAL_ARM_NEON  := true
endif

LOCAL_MODULE    := libpng
LOCAL_SRC_FILES :=\
	png.c \
	pngerror.c \
	pngget.c \
	pngmem.c \
	pngpread.c \
	pngread.c \
	pngrio.c \
	pngrtran.c \
	pngrutil.c \
	pngset.c \
	pngtrans.c \
	pngwio.c \
	pngwrite.c \
	pngwtran.c \
	pngwutil.c \
	arm/arm_init.c \
	arm/filter_neon.S \
	arm/filter_neon_intrinsics.c
	
LOCAL_LDLIBS := -lz -landroid
LOCAL_EXPORT_LDLIBS := $(LOCAL_LDLIBS) #export linker cmds

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES) #export includes
$(info $(LOCAL_C_INCLUDES))

#include $(BUILD_SHARED_LIBRARY)
include $(BUILD_STATIC_LIBRARY)
