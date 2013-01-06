LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := convolute
LOCAL_SRC_FILES := convolute.cpp
LOCAL_LDLIBS := -llog
LOCAL_CFLAGS += -ffast-math -fno-unsafe-math-optimizations -fomit-frame-pointer
LOCAL_CPPFLAGS += -std=gnu++11 -O3
LOCAL_LDFLAGS += -no-canonical-prefixes

include $(BUILD_SHARED_LIBRARY)
