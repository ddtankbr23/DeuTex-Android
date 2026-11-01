#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_doomutils_deutex_MainActivity_nativeEngineStatus(JNIEnv* env, jobject) {
    return env->NewStringUTF("Native ARM64 bridge ready; DeuTex core integration pending");
}
