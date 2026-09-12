// 온디바이스 엔진의 JNI 경계.
//
// 여기서 하는 일은 엔진을 열고 무엇으로 돌 수 있는지 알아보는 것까지다. 추론은 다음
// 묶음이고, 그 전에 이 기기에서 백엔드가 실제로 잡히는지부터 확인해야 한다. NPU가 안 잡히면
// 조용히 CPU로 떨어지는데 에러가 나지 않아 알아채기 어렵다(엔진 릴리즈 노트).
//
// 발화 원문은 이 경계 안쪽으로만 들어간다. 밖으로 나가는 것은 뽑은 결과뿐이다.

#include <cstdlib>
#include <string>
#include <vector>

#include <jni.h>

#include "ggml-backend.h"
#include "llama.h"

namespace {

std::string join(const std::vector<std::string> & values, const char * separator) {
    std::string joined;
    for (size_t i = 0; i < values.size(); ++i) {
        if (i > 0) {
            joined += separator;
        }
        joined += values[i];
    }
    return joined;
}

std::string to_utf8(JNIEnv * env, jstring value) {
    const char * chars = env->GetStringUTFChars(value, nullptr);
    std::string copied = chars == nullptr ? std::string() : std::string(chars);
    if (chars != nullptr) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return copied;
}

// `enum`을 붙여야 한다. 같은 이름의 함수 `ggml_backend_dev_type()`이 열거형 이름을 가린다.
const char * type_name(enum ggml_backend_dev_type type) {
    switch (type) {
        case GGML_BACKEND_DEVICE_TYPE_CPU:   return "CPU";
        case GGML_BACKEND_DEVICE_TYPE_GPU:   return "GPU";
        case GGML_BACKEND_DEVICE_TYPE_IGPU:  return "IGPU";
        case GGML_BACKEND_DEVICE_TYPE_ACCEL: return "ACCEL";
        default:                             return "OTHER";
    }
}

} // namespace

extern "C" {

/**
 * 백엔드를 연다.
 *
 * `ADSP_LIBRARY_PATH`를 먼저 세운다. DSP 로더가 이 환경 변수로 HTP 이미지를 찾고, 없으면
 * 조용히 CPU로 떨어진다. `setenv`는 백엔드를 싣기 전에 걸어야 한다.
 *
 * `ggml_backend_load_all_from_path`에 앱의 네이티브 라이브러리 폴더를 넘긴다. 기본 탐색 경로는
 * 실행 파일 옆을 보는데 안드로이드 앱에는 그런 것이 없다.
 */
JNIEXPORT void JNICALL Java_com_mist_medicalmate_intake_ondevice_OnDeviceEngine_nativeOpen(
        JNIEnv * env, jobject, jstring native_lib_dir, jstring htp_dir) {
    const std::string htp = to_utf8(env, htp_dir);
    setenv("ADSP_LIBRARY_PATH", htp.c_str(), 1);

    const std::string libs = to_utf8(env, native_lib_dir);
    ggml_backend_load_all_from_path(libs.c_str());
    llama_backend_init();
}

/** 엔진 버전. 묶음이 바뀌었는지 확인할 때 쓴다. */
JNIEXPORT jstring JNICALL Java_com_mist_medicalmate_intake_ondevice_OnDeviceEngine_nativeVersion(
        JNIEnv * env, jobject) {
    return env->NewStringUTF(llama_version());
}

/**
 * 잡힌 장치 목록. `이름|종류|설명` 줄을 줄바꿈으로 잇는다.
 *
 * 구조체를 JNI로 넘기지 않는다. 읽고 로그에 남기는 값이고, 이 경계는 좁을수록 좋다.
 */
JNIEXPORT jstring JNICALL Java_com_mist_medicalmate_intake_ondevice_OnDeviceEngine_nativeDevices(
        JNIEnv * env, jobject) {
    std::vector<std::string> lines;
    const size_t count = ggml_backend_dev_count();
    lines.reserve(count);
    for (size_t i = 0; i < count; ++i) {
        ggml_backend_dev_t device = ggml_backend_dev_get(i);
        std::string line = ggml_backend_dev_name(device);
        line += "|";
        line += type_name(ggml_backend_dev_type(device));
        line += "|";
        line += ggml_backend_dev_description(device);
        lines.push_back(line);
    }
    return env->NewStringUTF(join(lines, "\n").c_str());
}

} // extern "C"
