# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.ccrg/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc .cpp .cxx
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../installfiles
LOCAL_MODULE    := cvd

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS += -DHAVE_NEON=1
    LOCAL_CFLAGS += -DLOCAL_ARM_NEON=1
    LOCAL_ARM_NEON  := true
    LOCAL_SRC_FILES += ../installfiles/cvd_src/NEON/convert_rgb_to_y.cc ../installfiles/cvd_src/NEON/half_sample.cc                      
else
    LOCAL_SRC_FILES += ../installfiles/cvd_src/noarch/convert_rgb_to_y.cc ../installfiles/cvd_src/noarch/half_sample.cc                      
endif
LOCAL_CFLAGS += -pthread -D_REENTRANT=1 -DCVD_DISABLE_JPEG=1 -DCVD_DISABLE_TIFF=1

LOCAL_SRC_FILES += ../installfiles/cvd_src/bayer.cxx                      \
../installfiles/cvd_src/brezenham.cc                       \
../installfiles/cvd_src/colourspace_convert.cxx                       \
../installfiles/cvd_src/connected_components.cc                      \
../installfiles/cvd_src/convolution.cc                      \
../installfiles/cvd_src/cvd_timer.cc                      \
../installfiles/cvd_src/deinterlacebuffer.cc                      \
../installfiles/cvd_src/diskbuffer2.cc                      \
../installfiles/cvd_src/draw.cc                      \
../installfiles/cvd_src/draw_toon.cc                      \
../installfiles/cvd_src/eventobject.cpp                      \
../installfiles/cvd_src/exceptions.cc                      \
../installfiles/cvd_src/fast/fast_10_detect.cxx                      \
../installfiles/cvd_src/fast/fast_10_score.cxx                      \
../installfiles/cvd_src/fast/fast_11_detect.cxx                      \
../installfiles/cvd_src/fast/fast_11_score.cxx                      \
../installfiles/cvd_src/fast/fast_12_detect.cxx                      \
../installfiles/cvd_src/fast/fast_12_score.cxx                      \
../installfiles/cvd_src/fast/fast_7_detect.cxx                      \
../installfiles/cvd_src/fast/fast_7_score.cxx                      \
../installfiles/cvd_src/fast/fast_8_detect.cxx                      \
../installfiles/cvd_src/fast/fast_8_score.cxx                      \
../installfiles/cvd_src/fast/fast_9_detect.cxx                      \
../installfiles/cvd_src/fast/fast_9_score.cxx                      \
../installfiles/cvd_src/fast_corner.cxx                      \
../installfiles/cvd_src/fast_corner_9_nonmax.cxx                      \
../installfiles/cvd_src/image_io.cc                      \
../installfiles/cvd_src/Linux/videosource_nov4l1buffer.cc                      \
../installfiles/cvd_src/Linux/videosource_nov4lbuffer.cc                      \
../installfiles/cvd_src/morphology.cc                      \
../installfiles/cvd_src/noarch/convolve_gaussian.cc                      \
../installfiles/cvd_src/noarch/default_memalign.cpp                      \
../installfiles/cvd_src/noarch/gradient.cc                      \
../installfiles/cvd_src/noarch/median_3x3.cc                      \
../installfiles/cvd_src/noarch/two_thirds_sample.cc                      \
../installfiles/cvd_src/noarch/utility_byte_differences.cc                      \
../installfiles/cvd_src/noarch/utility_double_int.cc                      \
../installfiles/cvd_src/noarch/utility_float.cc                      \
../installfiles/cvd_src/noarch/yuv422_wrapper.cc                      \
../installfiles/cvd_src/nonmax_suppression.cxx                      \
../installfiles/cvd_src/thread/runnable_batch.cc                      \
../installfiles/cvd_src/OSX/videosource_noqtbuffer.cc                      \
../installfiles/cvd_src/noarch/slower_corner_10.cxx                      \
../installfiles/cvd_src/slower_corner_11.cxx                      \
../installfiles/cvd_src/slower_corner_12.cxx                      \
../installfiles/cvd_src/slower_corner_7.cxx                      \
../installfiles/cvd_src/slower_corner_8.cxx                      \
../installfiles/cvd_src/slower_corner_9.cxx                      \
../installfiles/cvd_src/synchronized.cpp                      \
../installfiles/cvd_src/tensor_voting.cc                      \
../installfiles/cvd_src/thread.cpp                      \
../installfiles/cvd_src/timeddiskbuffer.cc                      \
../installfiles/cvd_src/videosource.cpp                      \
../installfiles/cvd_src/videosource_nodvbuffer.cc                      \
../installfiles/cvd_src/videosource_novideofilebuffer.cc                      \
../installfiles/cvd_src/yuv411_to_stuff.cxx                      \
../installfiles/cvd_src/yuv420.cpp                      \
../installfiles/cvd_src/yuv422.cpp                      \
../installfiles/cvd_src/get_time_of_day_ns.cc                      \
../installfiles/cvd_src/image_io/bmp_write.cc                      \
../installfiles/cvd_src/image_io/bmp.cxx                      \
../installfiles/cvd_src/image_io/cvdimage.cxx                      \
../installfiles/cvd_src/image_io/fits.cc                      \
../installfiles/cvd_src/image_io/fitswrite.cc                      \
../installfiles/cvd_src/image_io/pnm_grok.cxx                      \
../installfiles/cvd_src/image_io/save_postscript.cxx                      \
../installfiles/cvd_src/image_io/text.cxx                      \
../installfiles/cvd_src/image_io/text_write.cc                      \
../installfiles/cvd_src/image_io/bmp_read.cc                      \
../installfiles/cvd_src/image_io/png.cc                      \
#../installfiles/cvd_src/gltext.cpp
#../installfiles/cvd_src/image_io/jpeg.cxx                      \
#../installfiles/cvd_src/glwindow.cc                      \


LOCAL_STATIC_LIBRARIES += TooN
LOCAL_STATIC_LIBRARIES += cpufeatures
LOCAL_STATIC_LIBRARIES += png
#LOCAL_STATIC_LIBRARIES += gl4es
LOCAL_LDLIBS    += -landroid -llog

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES) #export includes
LOCAL_EXPORT_LDLIBS := $(LOCAL_LDLIBS) #export linker cmds
LOCAL_EXPORT_CFLAGS := $(LOCAL_CFLAGS) #export c flgs
LOCAL_EXPORT_CPPFLAGS := $(LOCAL_CPPFLAGS) #export cpp flgs
LOCAL_EXPORT_CXXFLAGS := $(LOCAL_CXXFLAGS) #export cpp flgs

include $(BUILD_STATIC_LIBRARY)

$(call import-module,android/cpufeatures)
$(call import-add-path,../../)
$(call import-module,TooN)
$(call import-module,libpng)
#$(call import-module,gl4es)
