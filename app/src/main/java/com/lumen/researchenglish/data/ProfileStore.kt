package com.lumen.researchenglish.data

import android.content.Context

class ProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences("lumen_profile", Context.MODE_PRIVATE)

    fun getUserAvatarUri(): String =
        preferences.getString(USER_AVATAR_URI, "").orEmpty()

    fun setUserAvatarUri(uri: String) {
        preferences.edit().putString(USER_AVATAR_URI, uri).apply()
    }

    fun getReaderMode(): String =
        preferences.getString(READER_MODE, "light").orEmpty().ifBlank { "light" }

    fun setReaderMode(mode: String) {
        preferences.edit().putString(READER_MODE, mode).apply()
    }

    fun getVoiceType(): Int = preferences.getInt(VOICE_TYPE, DEFAULT_VOICE_TYPE)

    fun setVoiceType(voiceType: Int) {
        preferences.edit().putInt(VOICE_TYPE, voiceType).apply()
    }

    fun getSpeechRate(): Float = preferences.getFloat(SPEECH_RATE, 0f)

    fun setSpeechRate(rate: Float) {
        preferences.edit().putFloat(SPEECH_RATE, rate).apply()
    }

    fun getUpdateSource(): String =
        preferences.getString(UPDATE_SOURCE, DEFAULT_UPDATE_SOURCE)
            .orEmpty()
            .ifBlank { DEFAULT_UPDATE_SOURCE }

    fun setUpdateSource(source: String) {
        preferences.edit().putString(UPDATE_SOURCE, source.trim()).apply()
    }

    fun getVocabularyDeckId(): String =
        preferences.getString(VOCABULARY_DECK_ID, DEFAULT_VOCABULARY_DECK)
            .orEmpty()
            .ifBlank { DEFAULT_VOCABULARY_DECK }

    fun setVocabularyDeckId(deckId: String) {
        preferences.edit().putString(VOCABULARY_DECK_ID, deckId).apply()
    }

    fun getVocabularyDeckPosition(deckId: String): Int =
        preferences.getInt("$VOCABULARY_DECK_POSITION_PREFIX$deckId", 0).coerceAtLeast(0)

    fun setVocabularyDeckPosition(deckId: String, position: Int) {
        preferences.edit()
            .putInt("$VOCABULARY_DECK_POSITION_PREFIX$deckId", position.coerceAtLeast(0))
            .apply()
    }

    fun getLearningXp(): Int = preferences.getInt(LEARNING_XP, 0).coerceAtLeast(0)

    fun addLearningXp(amount: Int): Int {
        if (amount <= 0) return getLearningXp()
        val updated = (getLearningXp().toLong() + amount)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        preferences.edit().putInt(LEARNING_XP, updated).apply()
        return updated
    }

    fun getHighestReadPage(documentId: String, fallback: Int = 0): Int =
        preferences.getInt("$HIGHEST_READ_PAGE_PREFIX$documentId", fallback).coerceAtLeast(0)

    fun setHighestReadPage(documentId: String, page: Int) {
        preferences.edit()
            .putInt("$HIGHEST_READ_PAGE_PREFIX$documentId", page.coerceAtLeast(0))
            .apply()
    }

    fun isBookCompletionRewarded(documentId: String): Boolean =
        preferences.getBoolean("$BOOK_COMPLETED_PREFIX$documentId", false)

    fun markBookCompletionRewarded(documentId: String) {
        preferences.edit().putBoolean("$BOOK_COMPLETED_PREFIX$documentId", true).apply()
    }

    fun getChatHistoryLimit(): Int =
        preferences.getInt(CHAT_HISTORY_LIMIT, DEFAULT_CHAT_HISTORY_LIMIT).coerceIn(1, 30)

    fun setChatHistoryLimit(limit: Int) {
        preferences.edit().putInt(CHAT_HISTORY_LIMIT, limit.coerceIn(1, 30)).apply()
    }

    fun getMemoryUpdateFrequency(): Int =
        preferences.getInt(MEMORY_UPDATE_FREQUENCY, DEFAULT_MEMORY_UPDATE_FREQUENCY).coerceIn(1, 50)

    fun setMemoryUpdateFrequency(frequency: Int) {
        preferences.edit().putInt(MEMORY_UPDATE_FREQUENCY, frequency.coerceIn(1, 50)).apply()
    }

    fun getCurrentChatSessionId(): String =
        preferences.getString(CURRENT_CHAT_SESSION, DEFAULT_CHAT_SESSION)
            .orEmpty()
            .ifBlank { DEFAULT_CHAT_SESSION }

    fun setCurrentChatSessionId(id: String) {
        preferences.edit().putString(CURRENT_CHAT_SESSION, id).apply()
    }

    companion object {
        const val DEFAULT_VOICE_TYPE = 502004
        const val DEFAULT_UPDATE_SOURCE = "https://github.com/guiyanghai183/lumen-research-english"
        const val DEFAULT_VOCABULARY_DECK = "toefl"
        const val DEFAULT_CHAT_HISTORY_LIMIT = 7
        const val DEFAULT_MEMORY_UPDATE_FREQUENCY = 4
        const val DEFAULT_CHAT_SESSION = "default"
        private const val USER_AVATAR_URI = "user_avatar_uri"
        private const val READER_MODE = "reader_mode"
        private const val VOICE_TYPE = "voice_type"
        private const val SPEECH_RATE = "speech_rate"
        private const val UPDATE_SOURCE = "update_source"
        private const val VOCABULARY_DECK_ID = "vocabulary_deck_id"
        private const val VOCABULARY_DECK_POSITION_PREFIX = "vocabulary_deck_position_"
        private const val LEARNING_XP = "learning_xp"
        private const val HIGHEST_READ_PAGE_PREFIX = "highest_read_page_"
        private const val BOOK_COMPLETED_PREFIX = "book_completed_"
        private const val CHAT_HISTORY_LIMIT = "chat_history_limit"
        private const val MEMORY_UPDATE_FREQUENCY = "memory_update_frequency"
        private const val CURRENT_CHAT_SESSION = "current_chat_session"
    }
}
