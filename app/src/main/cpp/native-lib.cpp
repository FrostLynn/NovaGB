#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "NovaGB-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Native Core Framebuffer (160x144 ARGB)
static std::vector<uint32_t> g_framebuffer(160 * 144, 0xFF9BBC0F);
static std::vector<uint8_t> g_romData;
static uint8_t g_buttonMask = 0;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_novagb_emulator_core_NativeGbBridge_nativeLoadRom(
    JNIEnv* env,
    jobject /* this */,
    jbyteArray romBytes
) {
    jsize len = env->GetArrayLength(romBytes);
    g_romData.resize(len);
    env->GetByteArrayRegion(romBytes, 0, len, reinterpret_cast<jbyte*>(g_romData.data()));
    LOGI("Loaded ROM into native core, size: %d bytes", len);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_novagb_emulator_core_NativeGbBridge_nativeStepFrame(
    JNIEnv* /* env */,
    jobject /* this */
) {
    // 70224 cycles per frame
    return 70224;
}

extern "C" JNIEXPORT void JNICALL
Java_com_novagb_emulator_core_NativeGbBridge_nativeSetButtons(
    JNIEnv* /* env */,
    jobject /* this */,
    jint buttonMask
) {
    g_buttonMask = static_cast<uint8_t>(buttonMask);
}

extern "C" JNIEXPORT void JNICALL
Java_com_novagb_emulator_core_NativeGbBridge_nativeGetFramebuffer(
    JNIEnv* env,
    jobject /* this */,
    jintArray outBuffer
) {
    env->SetIntArrayRegion(
        outBuffer,
        0,
        160 * 144,
        reinterpret_cast<const jint*>(g_framebuffer.data())
    );
}