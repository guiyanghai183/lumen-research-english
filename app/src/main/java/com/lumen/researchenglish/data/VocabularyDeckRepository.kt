package com.lumen.researchenglish.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class DeckWord(
    val word: String,
    val partOfSpeech: String,
    val difficulty: Int,
    val theme: String,
    val synonyms: List<String>,
    val definition: String,
    val chineseDefinition: String = "",
    val phonetic: String = "",
    val example: String,
)

data class VocabularyDeck(
    val id: String,
    val name: String,
    val description: String,
    val words: List<DeckWord>,
)

class VocabularyDeckRepository(private val context: Context) {
    val decks: List<VocabularyDeck> by lazy {
        val toeflWords = loadToeflWords()
        val cet4Words = loadExamWords("cet4")
        val cet6Words = loadExamWords("cet6")
        val scienceThemes = setOf(
            "Agricultural Science",
            "Archaeology",
            "Astronomy",
            "Biology",
            "Chemistry",
            "Engineering",
            "Environmental Science",
            "Geology",
            "Materials Science",
            "Meteorology",
            "Oceanography",
            "Physics",
        )
        listOf(
            VocabularyDeck(
                id = "toefl",
                name = "TOEFL Essential",
                description = "1,000 high-frequency academic words with Chinese definitions",
                words = toeflWords,
            ),
            VocabularyDeck(
                id = "cet4",
                name = "CET-4",
                description = "${cet4Words.size} College English Test Band 4 words",
                words = cet4Words,
            ),
            VocabularyDeck(
                id = "cet6",
                name = "CET-6",
                description = "${cet6Words.size} College English Test Band 6 words",
                words = cet6Words,
            ),
            VocabularyDeck(
                id = "toefl-science",
                name = "TOEFL Science",
                description = "Science, engineering and environment",
                words = toeflWords.filter { it.theme in scienceThemes },
            ),
            VocabularyDeck(
                id = "toefl-humanities",
                name = "TOEFL Humanities",
                description = "History, society, law and the humanities",
                words = toeflWords.filterNot { it.theme in scienceThemes },
            ),
        )
    }

    fun deck(id: String): VocabularyDeck =
        decks.firstOrNull { it.id == id } ?: decks.first()

    private fun loadToeflWords(): List<DeckWord> {
        val translations = loadToeflChineseDefinitions()
        val raw = context.assets.open("vocabulary/toefl.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val synonymArray = item.optJSONArray("synonyms") ?: JSONArray()
                val synonyms = buildList {
                    for (synonymIndex in 0 until synonymArray.length()) {
                        add(synonymArray.optString(synonymIndex))
                    }
                }.filter { it.isNotBlank() }
                val word = item.optString("word").trim()
                val definition = item.optString("definition_en").trim()
                if (word.isBlank() || definition.isBlank()) continue
                val translation = translations[word.key()]
                add(
                    DeckWord(
                        word = word,
                        partOfSpeech = item.optString("pos").trim(),
                        difficulty = item.optInt("difficulty", 3),
                        theme = item.optString("theme").trim(),
                        synonyms = synonyms,
                        definition = definition,
                        chineseDefinition = translation?.definition.orEmpty(),
                        phonetic = translation?.phonetic.orEmpty(),
                        example = item.optString("example_sentence").trim(),
                    ),
                )
            }
        }
    }

    private fun loadToeflChineseDefinitions(): Map<String, ChineseDefinition> {
        val raw = context.assets.open("vocabulary/toefl_zh.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val dictionary = JSONObject(raw)
        return buildMap {
            val keys = dictionary.keys()
            while (keys.hasNext()) {
                val word = keys.next()
                val item = dictionary.optJSONObject(word) ?: continue
                val definition = normalizeEcdictDefinition(item.optString("zh"))
                val phonetic = item.optString("phonetic").trim()
                if (definition.isNotBlank() || phonetic.isNotBlank()) {
                    put(word.key(), ChineseDefinition(definition, phonetic))
                }
            }
            TOEFL_CHINESE_FALLBACKS.forEach { (word, translation) ->
                if (word.key() !in this) put(word.key(), translation)
            }
        }
    }

    private fun loadExamWords(tag: String): List<DeckWord> {
        val raw = context.assets.open("vocabulary/ecdict_exam.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                if (!item.hasTag(tag)) continue
                val word = item.optString("word").trim()
                val chineseDefinition = normalizeEcdictDefinition(item.optString("zh"))
                val definition = normalizeEcdictDefinition(item.optString("en"))
                if (word.isBlank() || (chineseDefinition.isBlank() && definition.isBlank())) continue
                add(
                    DeckWord(
                        word = word,
                        partOfSpeech = item.optString("pos").trim(),
                        difficulty = if (tag == "cet4") 2 else 3,
                        theme = tag.uppercase(Locale.ROOT).replace("CET", "CET-"),
                        synonyms = emptyList(),
                        definition = definition,
                        chineseDefinition = chineseDefinition,
                        phonetic = item.optString("phonetic").trim(),
                        example = "",
                    ),
                )
            }
        }
    }

    private fun JSONObject.hasTag(tag: String): Boolean {
        val tags = optJSONArray("tags") ?: return false
        return (0 until tags.length()).any { index ->
            tags.optString(index).equals(tag, ignoreCase = true)
        }
    }

    private fun String.key(): String = trim().lowercase(Locale.ROOT)

    /** ECDICT uses line breaks between senses; the generated JSON preserved them as ASCII '?'. */
    private fun normalizeEcdictDefinition(raw: String): String = raw
        .replace("?", "\n")
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n")

    private data class ChineseDefinition(
        val definition: String,
        val phonetic: String,
    )

    private companion object {
        // The WordLevel list contains the spelling "inasmuchas" but its ECDICT lookup key is absent.
        val TOEFL_CHINESE_FALLBACKS = mapOf(
            "inasmuchas" to ChineseDefinition("conj. 鉴于；因为；既然", ""),
        )
    }
}
