package com.lumen.researchenglish.domain

enum class TutorApiProvider(
    val storageValue: String,
    val displayName: String,
) {
    DEEPSEEK("deepseek", "DeepSeek V4 Flash"),
    QWEN("qwen", "Alibaba Qwen3.7 Flash"),
    ;

    companion object {
        fun fromStorage(value: String?): TutorApiProvider =
            entries.firstOrNull { it.storageValue == value } ?: DEEPSEEK
    }
}
