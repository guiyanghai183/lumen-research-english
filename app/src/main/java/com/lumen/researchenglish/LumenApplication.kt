package com.lumen.researchenglish

import android.app.Application
import androidx.room.Room
import com.lumen.researchenglish.data.AppDatabase
import com.lumen.researchenglish.data.ChatRepository
import com.lumen.researchenglish.data.DocumentRepository
import com.lumen.researchenglish.data.MemoryRepository
import com.lumen.researchenglish.data.MIGRATION_1_2
import com.lumen.researchenglish.data.MIGRATION_2_3
import com.lumen.researchenglish.data.ProfileStore
import com.lumen.researchenglish.data.ReaderAnnotationStore
import com.lumen.researchenglish.data.SecretStore
import com.lumen.researchenglish.data.VocabularyRepository
import com.lumen.researchenglish.data.VocabularyDeckRepository
import com.lumen.researchenglish.network.UpdateInstaller
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LumenApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "lumen.db")
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }
    val secretStore by lazy { SecretStore(this) }
    val profileStore by lazy { ProfileStore(this) }
    val readerAnnotationStore by lazy { ReaderAnnotationStore(this) }
    val documentRepository by lazy { DocumentRepository(this, database.documentDao()) }
    val vocabularyRepository by lazy { VocabularyRepository(database.vocabularyDao()) }
    val vocabularyDeckRepository by lazy { VocabularyDeckRepository(this) }
    val chatRepository by lazy { ChatRepository(database.chatDao()) }
    val memoryRepository by lazy { MemoryRepository(this) }
    val updateInstaller by lazy { UpdateInstaller(this) }

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        memoryRepository.ensureDefaultMemory()
        applicationScope.launch {
            documentRepository.seedBundledBooks()
            chatRepository.ensureDefaultSession()
        }
        applicationScope.launch {
            vocabularyRepository.backfillDeckChineseDefinitions(
                sourcePrefix = "TOEFL",
                words = vocabularyDeckRepository.deck("toefl").words,
            )
            vocabularyRepository.backfillDeckChineseDefinitions(
                sourcePrefix = "CET-4",
                words = vocabularyDeckRepository.deck("cet4").words,
            )
            vocabularyRepository.backfillDeckChineseDefinitions(
                sourcePrefix = "CET-6",
                words = vocabularyDeckRepository.deck("cet6").words,
            )
        }
    }
}
