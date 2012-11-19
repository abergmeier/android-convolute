LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := fib
LOCAL_SRC_FILES := fib.cpp
LOCAL_LDLIBS := -llog -L$(LOCAL_PATH)/boost/lib/
LOCAL_CFLAGS += -I$(LOCAL_PATH)/boost/include/boost-1_49
LOCAL_CPPFLAGS += -std=gnu++11 -O3 -ffast-math -fno-unsafe-math-optimizations -fomit-frame-pointer
LOCAL_CPP_FEATURES += exceptions rtti
LOCAL_STATIC_LIBRARIES += cpufeatures
#-marm -march=armv7-a -mfpu=neon

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)

