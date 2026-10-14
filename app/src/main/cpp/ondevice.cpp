// 온디바이스 엔진의 JNI 경계.
//
// 엔진을 열고 장치를 확인하는 것(#142)과 모델로 한 번 생성하는 것(#289)까지다. 프롬프트를
// 만드는 일, 문법을 고르는 일, 출력을 읽는 일은 모두 Kotlin에 있다. 여기는 받은 글을 토큰으로
// 바꿔 넣고 나온 토큰을 글로 돌려줄 뿐이다. 이 경계는 좁을수록 좋다.
//
// 글은 `jstring`이 아니라 UTF-8 바이트로 주고받는다. JNI의 문자열은 "변형 UTF-8"이라 이모지 같은
// 4바이트 글자를 서로게이트 두 개로 쪼갠다. 그대로 토크나이저에 넣으면 깨진 글자가 되고, 모델이
// 이모지를 내면 `NewStringUTF`가 앱을 죽인다.
//
// 발화 원문은 이 경계 안쪽으로만 들어간다. 밖으로 나가는 것은 뽑은 결과뿐이다.

#include <algorithm>
#include <cstdint>
#include <cstdlib>
#include <cmath>
#include <string>
#include <vector>

#include <android/log.h>
#include <jni.h>

#include "ggml-backend.h"
#include "llama.h"

namespace {

constexpr const char * TAG = "OnDeviceEngine";

// AI 트랙이 잰 설정 그대로다(handoff "대응표").
constexpr uint32_t CONTEXT_TOKENS = 4096;
constexpr uint32_t BATCH_TOKENS = 2048;
constexpr uint32_t MICRO_BATCH_TOKENS = 512;
constexpr int32_t ALL_LAYERS = 99;
constexpr llama_seq_id SEQUENCE = 0;

/**
 * 모델 하나와 그 컨텍스트.
 *
 * `cached_prefix`는 마지막으로 처리한 시스템 프롬프트이고 `state`는 그것을 처리한 직후의
 * 시퀀스 상태다. 같은 프롬프트로 다시 부르면 상태를 되돌리고 사용자 메시지만 넣는다.
 */
struct Session {
    llama_model * model = nullptr;
    llama_context * context = nullptr;
    std::string cached_prefix;
    std::vector<uint8_t> state;
};

std::string to_utf8(JNIEnv * env, jstring value) {
    const char * chars = env->GetStringUTFChars(value, nullptr);
    std::string copied = chars == nullptr ? std::string() : std::string(chars);
    if (chars != nullptr) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return copied;
}

std::string from_bytes(JNIEnv * env, jbyteArray bytes) {
    const jsize length = env->GetArrayLength(bytes);
    std::string copied(static_cast<size_t>(length), '\0');
    env->GetByteArrayRegion(bytes, 0, length, reinterpret_cast<jbyte *>(copied.data()));
    return copied;
}

jbyteArray to_bytes(JNIEnv * env, const std::string & value) {
    jbyteArray bytes = env->NewByteArray(static_cast<jsize>(value.size()));
    env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(value.size()), reinterpret_cast<const jbyte *>(value.data()));
    return bytes;
}

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

// 특수 토큰(`<|im_start|>` 등)을 글이 아니라 토큰으로 읽는다. 프롬프트를 앱이 그 모양으로
// 만들어 넘기기 때문이다.
std::vector<llama_token> tokenize(const llama_vocab * vocab, const std::string & text, bool add_special) {
    const int32_t needed = -llama_tokenize(vocab, text.data(), static_cast<int32_t>(text.size()),
                                           nullptr, 0, add_special, true);
    std::vector<llama_token> tokens(static_cast<size_t>(std::max(needed, 0)));
    const int32_t written = llama_tokenize(vocab, text.data(), static_cast<int32_t>(text.size()),
                                           tokens.data(), static_cast<int32_t>(tokens.size()), add_special, true);
    tokens.resize(static_cast<size_t>(std::max(written, 0)));
    return tokens;
}

// 한 번에 `BATCH_TOKENS`씩 넣는다. 시스템 프롬프트가 약 2,000토큰이라 대개 한 번이다.
bool decode_all(llama_context * context, std::vector<llama_token> & tokens) {
    for (size_t start = 0; start < tokens.size(); start += BATCH_TOKENS) {
        const auto count = static_cast<int32_t>(std::min<size_t>(BATCH_TOKENS, tokens.size() - start));
        if (llama_decode(context, llama_batch_get_one(tokens.data() + start, count)) != 0) {
            return false;
        }
    }
    return true;
}

/**
 * 시스템 프롬프트까지를 컨텍스트에 올린다.
 *
 * 같은 프롬프트를 이미 처리해 두었으면 그 상태를 되돌린다. 2B는 DeltaNet 상태라
 * `llama_memory_seq_rm`으로 뒤쪽 턴만 지우는 방식이 통하지 않는다("does not support partial
 * sequence removal"). 그래서 통째로 비운 뒤 떠 둔 상태를 다시 싣는다(AI#35).
 */
bool load_prefix(Session & session, const std::string & prefix) {
    llama_memory_t memory = llama_get_memory(session.context);
    llama_memory_clear(memory, true);

    if (prefix == session.cached_prefix && !session.state.empty()) {
        if (llama_state_seq_set_data(session.context, session.state.data(), session.state.size(), SEQUENCE) > 0) {
            return true;
        }
        // 되돌리기가 실패하면 처음부터 다시 처리한다. 느릴 뿐 결과는 같다.
        __android_log_print(ANDROID_LOG_WARN, TAG, "상태를 되돌리지 못해 시스템 프롬프트를 다시 처리한다");
        llama_memory_clear(memory, true);
    }

    const llama_vocab * vocab = llama_model_get_vocab(session.model);
    std::vector<llama_token> tokens = tokenize(vocab, prefix, llama_vocab_get_add_bos(vocab));
    if (!decode_all(session.context, tokens)) {
        session.cached_prefix.clear();
        session.state.clear();
        return false;
    }

    session.state.resize(llama_state_seq_get_size(session.context, SEQUENCE));
    llama_state_seq_get_data(session.context, session.state.data(), session.state.size(), SEQUENCE);
    session.cached_prefix = prefix;
    return true;
}

/**
 * 문법에 맞는 것 가운데 가장 높은 토큰을 고른다. 온도 0이라 greedy다.
 *
 * **먼저 가장 높은 토큰 하나만 문법에 대어 본다.** 맞으면 그것을 쓰고, 어긋날 때만 어휘 전체를
 * 문법에 거른다. llama-server가 이렇게 한다. 처음에는 매 토큰 어휘 전체(약 24만)를 문법에
 * 걸렀는데, 그 일이 CPU에서 토큰마다 돌아 같은 기기에서 생성이 15.8에서 5 tok/s대로 떨어졌다.
 * 결과는 같다 — 가장 높은 토큰이 문법에 맞으면 걸러도 그것이 남는다.
 */
llama_token pick(llama_context * context, const llama_vocab * vocab, llama_sampler * grammar) {
    const float * logits = llama_get_logits_ith(context, -1);
    const int32_t vocab_size = llama_vocab_n_tokens(vocab);
    const llama_token best = static_cast<llama_token>(std::max_element(logits, logits + vocab_size) - logits);

    llama_token_data single = {best, logits[best], 0.0f};
    llama_token_data_array one = {&single, 1, -1, false};
    llama_sampler_apply(grammar, &one);
    if (std::isfinite(one.data[0].logit)) {
        return best;
    }

    std::vector<llama_token_data> candidates(static_cast<size_t>(vocab_size));
    for (llama_token id = 0; id < vocab_size; ++id) {
        candidates[static_cast<size_t>(id)] = {id, logits[id], 0.0f};
    }
    llama_token_data_array all = {candidates.data(), candidates.size(), -1, false};
    llama_sampler_apply(grammar, &all);
    const auto chosen =
            std::max_element(all.data, all.data + all.size, [](const llama_token_data & a, const llama_token_data & b) {
                return a.logit < b.logit;
            });
    return chosen->id;
}

std::string piece(const llama_vocab * vocab, llama_token token) {
    char buffer[256];
    const int32_t length = llama_token_to_piece(vocab, token, buffer, sizeof(buffer), 0, false);
    if (length >= 0) {
        return std::string(buffer, static_cast<size_t>(length));
    }
    std::string larger(static_cast<size_t>(-length), '\0');
    llama_token_to_piece(vocab, token, larger.data(), static_cast<int32_t>(larger.size()), 0, false);
    return larger;
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
JNIEXPORT void JNICALL Java_com_mist_medicalmate_core_ondevice_OnDeviceNative_nativeOpen(
        JNIEnv * env, jclass, jstring native_lib_dir, jstring htp_dir) {
    const std::string htp = to_utf8(env, htp_dir);
    setenv("ADSP_LIBRARY_PATH", htp.c_str(), 1);

    const std::string libs = to_utf8(env, native_lib_dir);
    ggml_backend_load_all_from_path(libs.c_str());
    llama_backend_init();
}

/** 엔진 버전. 묶음이 바뀌었는지 확인할 때 쓴다. */
JNIEXPORT jstring JNICALL Java_com_mist_medicalmate_core_ondevice_OnDeviceNative_nativeVersion(
        JNIEnv * env, jclass) {
    return env->NewStringUTF(llama_version());
}

/**
 * 잡힌 장치 목록. `이름|종류|설명` 줄을 줄바꿈으로 잇는다.
 *
 * 구조체를 JNI로 넘기지 않는다. 읽고 로그에 남기는 값이고, 이 경계는 좁을수록 좋다.
 */
JNIEXPORT jstring JNICALL Java_com_mist_medicalmate_core_ondevice_OnDeviceNative_nativeDevices(
        JNIEnv * env, jclass) {
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

/**
 * 모델을 올리고 컨텍스트를 만든다. 실패하면 0이다.
 *
 * NPU(`HTP0`)가 잡혀 있으면 모든 층을 거기에 둔다. 장치 목록을 비워 두면 ggml이 잡힌 장치를
 * 전부 쓰는데, 그 안에 OpenCL이 끼면 층이 엉뚱한 곳으로 간다.
 *
 * `n_seq_max`는 1이어야 한다. 2B의 `ssm_out` 행렬곱이 시퀀스 차원을 가지면 Hexagon이 받지 않아
 * 층마다 CPU로 갔다 온다(graph splits 3 → 39, AI#35).
 */
JNIEXPORT jlong JNICALL Java_com_mist_medicalmate_core_ondevice_OnDeviceNative_nativeLoad(
        JNIEnv * env, jclass, jstring model_path) {
    const std::string path = to_utf8(env, model_path);

    llama_model_params model_params = llama_model_default_params();
    ggml_backend_dev_t devices[] = {ggml_backend_dev_by_name("HTP0"), nullptr};
    if (devices[0] != nullptr) {
        model_params.devices = devices;
        model_params.n_gpu_layers = ALL_LAYERS;
    } else {
        __android_log_print(ANDROID_LOG_WARN, TAG, "HTP0이 없어 CPU로 올린다");
        model_params.n_gpu_layers = 0;
    }

    llama_model * model = llama_model_load_from_file(path.c_str(), model_params);
    if (model == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "모델을 올리지 못했다: %s", path.c_str());
        return 0;
    }

    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = CONTEXT_TOKENS;
    context_params.n_batch = BATCH_TOKENS;
    context_params.n_ubatch = MICRO_BATCH_TOKENS;
    context_params.n_seq_max = 1;
    context_params.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_AUTO;

    llama_context * context = llama_init_from_model(model, context_params);
    if (context == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "컨텍스트를 만들지 못했다");
        llama_model_free(model);
        return 0;
    }

    auto * session = new Session();
    session->model = model;
    session->context = context;
    return reinterpret_cast<jlong>(session);
}

/**
 * 한 번 생성한다. 실패하면 null이다.
 *
 * @param prefix 시스템 프롬프트까지. 지난번과 같으면 떠 둔 상태를 되돌린다.
 * @param suffix 사용자 메시지와 어시스턴트 머리, 생각 끄기 블록까지.
 * @param grammar GBNF. 출력 모양을 강제한다. 문법이 닫히면 끝 토큰만 남는다.
 */
JNIEXPORT jbyteArray JNICALL Java_com_mist_medicalmate_core_ondevice_OnDeviceNative_nativeGenerate(
        JNIEnv * env, jclass, jlong handle, jbyteArray prefix_bytes, jbyteArray suffix_bytes,
        jbyteArray grammar_bytes, jint max_tokens) {
    auto * session = reinterpret_cast<Session *>(handle);
    if (session == nullptr) {
        return nullptr;
    }

    if (!load_prefix(*session, from_bytes(env, prefix_bytes))) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "시스템 프롬프트를 처리하지 못했다");
        return nullptr;
    }

    const llama_vocab * vocab = llama_model_get_vocab(session->model);
    std::vector<llama_token> suffix = tokenize(vocab, from_bytes(env, suffix_bytes), false);
    if (!decode_all(session->context, suffix)) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "사용자 메시지를 처리하지 못했다");
        return nullptr;
    }

    llama_sampler * grammar = llama_sampler_init_grammar(vocab, from_bytes(env, grammar_bytes).c_str(), "root");
    if (grammar == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "문법을 읽지 못했다");
        return nullptr;
    }

    std::string output;
    bool failed = false;
    for (jint i = 0; i < max_tokens; ++i) {
        llama_token token = pick(session->context, vocab, grammar);
        if (llama_vocab_is_eog(vocab, token)) {
            break;
        }
        llama_sampler_accept(grammar, token);
        output += piece(vocab, token);
        if (llama_decode(session->context, llama_batch_get_one(&token, 1)) != 0) {
            failed = true;
            break;
        }
    }
    llama_sampler_free(grammar);

    return failed ? nullptr : to_bytes(env, output);
}

/** 모델과 컨텍스트를 내린다. 메모리를 1GB 넘게 쥐고 있으므로 쓰지 않을 때 부른다. */
JNIEXPORT void JNICALL Java_com_mist_medicalmate_core_ondevice_OnDeviceNative_nativeFree(
        JNIEnv *, jclass, jlong handle) {
    auto * session = reinterpret_cast<Session *>(handle);
    if (session == nullptr) {
        return;
    }
    llama_free(session->context);
    llama_model_free(session->model);
    delete session;
}

} // extern "C"
