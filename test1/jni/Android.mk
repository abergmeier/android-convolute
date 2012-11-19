LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := fib
LOCAL_SRC_FILES := fib.cpp
LOCAL_LDLIBS := -llog
LOCAL_CFLAGS += -ffast-math -fno-unsafe-math-optimizations -fomit-frame-pointer
LOCAL_CPPFLAGS += -std=gnu++11 -O3
LOCAL_CPP_FEATURES += exceptions rtti
LOCAL_STATIC_LIBRARIES += cpufeatures

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)

