package com.lumen.researchenglish.data

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val uri: String,
    val coverPath: String?,
    val pageCount: Int,
    val lastPage: Int = 0,
    val progress: Float = 0f,
    val importedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "vocabulary_cards",
    indices = [Index(value = ["lemma"], unique = true)],
)
data class VocabularyCardEntity(
    @PrimaryKey val id: String,
    val term: String,
    val lemma: String,
    val translation: String,
    val context: String,
    val sourceTitle: String,
    val sourcePage: Int,
    val createdAt: Long,
    val dueAt: Long,
    val stability: Double = 0.4,
    val difficulty: Double = 5.0,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val lastReviewedAt: Long = 0,
)

@Entity(tableName = "review_logs")
data class ReviewLogEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val rating: Int,
    val reviewedAt: Long,
    val nextDueAt: Long,
    val intervalDays: Int,
)

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["conversationId"])],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'default'")
    val conversationId: String = ProfileStore.DEFAULT_CHAT_SESSION,
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY lastOpenedAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Update
    suspend fun update(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary_cards ORDER BY dueAt ASC")
    fun observeAll(): Flow<List<VocabularyCardEntity>>

    @Query("SELECT * FROM vocabulary_cards WHERE dueAt <= :now ORDER BY dueAt ASC LIMIT 100")
    fun observeDue(now: Long): Flow<List<VocabularyCardEntity>>

    @Query("SELECT COUNT(*) FROM vocabulary_cards WHERE dueAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM vocabulary_cards WHERE lemma = :lemma LIMIT 1")
    suspend fun findByLemma(lemma: String): VocabularyCardEntity?

    @Query("SELECT * FROM vocabulary_cards WHERE sourceTitle LIKE :sourcePrefix")
    suspend fun findBySourceTitlePrefix(sourcePrefix: String): List<VocabularyCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: VocabularyCardEntity)

    @Update
    suspend fun update(card: VocabularyCardEntity)

    @Insert
    suspend fun insertLog(log: ReviewLogEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_sessions ORDER BY pinned DESC, updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE pinned = 0 ORDER BY updatedAt DESC")
    suspend fun getUnpinnedSessions(): List<ChatSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Insert
    suspend fun insert(message: ChatMessageEntity)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun messageCount(conversationId: String): Int

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clear(conversationId: String)
}

@Database(
    entities = [
        DocumentEntity::class,
        VocabularyCardEntity::class,
        ReviewLogEntity::class,
        ChatMessageEntity::class,
        ChatSessionEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun chatDao(): ChatDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                pinned INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "ALTER TABLE chat_messages ADD COLUMN conversationId TEXT NOT NULL DEFAULT 'default'",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_chat_messages_conversationId ON chat_messages(conversationId)",
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO chat_sessions(id, title, pinned, createdAt, updatedAt)
            SELECT 'default', 'Earlier chat', 0,
                   COALESCE(MIN(createdAt), strftime('%s','now') * 1000),
                   COALESCE(MAX(createdAt), strftime('%s','now') * 1000)
            FROM chat_messages
            """.trimIndent(),
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE vocabulary_cards ADD COLUMN lastReviewedAt INTEGER NOT NULL DEFAULT 0",
        )
    }
}
