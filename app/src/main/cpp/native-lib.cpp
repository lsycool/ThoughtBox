#include <jni.h>
#include <string>

extern "C"
jstring
Java_lsycool_com_himaterial_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
