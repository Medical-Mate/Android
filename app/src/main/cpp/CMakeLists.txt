cmake_minimum_required(VERSION 3.22.1)
project(medicalmate_ondevice LANGUAGES CXX)

# 엔진은 여기서 빌드하지 않는다. AI 트랙이 릴리즈로 넘긴 산출물을 `fetchOnDeviceEngine`이
# 풀어 둔 것을 그대로 링크한다. 경로는 build.gradle.kts가 넘긴다.
if (NOT DEFINED ONDEVICE_ENGINE_DIR)
    message(FATAL_ERROR "ONDEVICE_ENGINE_DIR가 없다. build.gradle.kts가 넘겨야 한다.")
endif ()

set(ENGINE_LIB_DIR "${ONDEVICE_ENGINE_DIR}/jniLibs/${ANDROID_ABI}")
set(ENGINE_INCLUDE_DIR "${ONDEVICE_ENGINE_DIR}/include")

if (NOT EXISTS "${ENGINE_LIB_DIR}/libllama.so")
    message(FATAL_ERROR "엔진 라이브러리가 없다: ${ENGINE_LIB_DIR}. ./gradlew :app:fetchOnDeviceEngine")
endif ()

# IMPORTED로 받는다. 이 파일들은 이미 빌드된 것이고 여기서 고치지 않는다.
add_library(llama SHARED IMPORTED)
set_target_properties(llama PROPERTIES IMPORTED_LOCATION "${ENGINE_LIB_DIR}/libllama.so")

add_library(ggml SHARED IMPORTED)
set_target_properties(ggml PROPERTIES IMPORTED_LOCATION "${ENGINE_LIB_DIR}/libggml.so")

add_library(ggml-base SHARED IMPORTED)
set_target_properties(ggml-base PROPERTIES IMPORTED_LOCATION "${ENGINE_LIB_DIR}/libggml-base.so")

add_library(medicalmate_ondevice SHARED ondevice.cpp)

target_include_directories(medicalmate_ondevice PRIVATE "${ENGINE_INCLUDE_DIR}")
target_compile_features(medicalmate_ondevice PRIVATE cxx_std_17)
target_link_libraries(medicalmate_ondevice PRIVATE llama ggml ggml-base android log)
