package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.database.dao.BookVersionDao
import com.example.data.database.dao.ArchiveLogDao
import com.example.data.database.entity.BookVersionEntity
import com.example.data.database.entity.ArchiveLogEntity
import com.example.data.database.dao.CategoryDao
import com.example.data.database.dao.SeriesDao
import com.example.data.database.dao.AuthorDao
import com.example.data.database.dao.BookMetadataDao
import com.example.data.database.dao.BookNoteDao
import com.example.data.database.dao.BookmarkDao
import com.example.data.database.dao.ChapterDao
import com.example.data.database.dao.DocumentDao
import com.example.data.database.dao.OcrRegionDao
import com.example.data.database.dao.PageDao
import com.example.data.database.dao.PrintedPageNumberDao
import com.example.data.database.dao.TableDao
import com.example.data.database.dao.TocDao
import com.example.data.database.dao.AiConversationDao
import com.example.data.database.dao.AiInsightDao
import com.example.data.database.dao.AiModelDao
import com.example.data.database.dao.EmbeddingDao
import com.example.data.database.dao.KeywordDao
import com.example.data.database.dao.SummaryDao
import com.example.data.database.dao.TextChunkDao
import com.example.data.database.dao.TopicDao
import com.example.data.database.entity.AiCitationEntity
import com.example.data.database.entity.AiConversationEntity
import com.example.data.database.entity.AiInsightEntity
import com.example.data.database.entity.AiMessageEntity
import com.example.data.database.entity.AiModelEntity
import com.example.data.database.entity.AuthorEntity
import com.example.data.database.entity.BookAuthorCrossRef
import com.example.data.database.entity.BookCategoryCrossRef
import com.example.data.database.entity.BookMetadataEntity
import com.example.data.database.entity.BookNoteEntity
import com.example.data.database.entity.BookSeriesCrossRef
import com.example.data.database.entity.BookmarkEntity
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.Converters
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.EmbeddingEntity
import com.example.data.database.entity.KeywordEntity
import com.example.data.database.entity.OcrRegionEntity
import com.example.data.database.entity.PageEntity
import com.example.data.database.entity.PrintedPageNumberEntity
import com.example.data.database.entity.SeriesEntity
import com.example.data.database.entity.SummaryEntity
import com.example.data.database.entity.TableCellEntity
import com.example.data.database.entity.TableEntity
import com.example.data.database.entity.TextChunkEntity
import com.example.data.database.entity.TocEntryEntity
import com.example.data.database.entity.TopicEntity

@Database(
    entities = [
        DocumentEntity::class,
        PageEntity::class,
        OcrRegionEntity::class,
        AuthorEntity::class,
        BookMetadataEntity::class,
        ChapterEntity::class,
        TocEntryEntity::class,
        TableEntity::class,
        TableCellEntity::class,
        PrintedPageNumberEntity::class,
        BookmarkEntity::class,
        BookNoteEntity::class,
        AiModelEntity::class,
        TextChunkEntity::class,
        EmbeddingEntity::class,
        SummaryEntity::class,
        KeywordEntity::class,
        TopicEntity::class,
        AiConversationEntity::class,
        AiMessageEntity::class,
        AiCitationEntity::class,
        AiInsightEntity::class,
        CategoryEntity::class,
        BookCategoryCrossRef::class,
        SeriesEntity::class,
        BookSeriesCrossRef::class,
        BookAuthorCrossRef::class,
        BookVersionEntity::class,
        ArchiveLogEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
    abstract fun ocrRegionDao(): OcrRegionDao
    abstract fun authorDao(): AuthorDao
    abstract fun bookMetadataDao(): BookMetadataDao
    abstract fun chapterDao(): ChapterDao
    abstract fun tocDao(): TocDao
    abstract fun tableDao(): TableDao
    abstract fun printedPageNumberDao(): PrintedPageNumberDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun bookNoteDao(): BookNoteDao
    abstract fun aiModelDao(): AiModelDao
    abstract fun textChunkDao(): TextChunkDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun summaryDao(): SummaryDao
    abstract fun keywordDao(): KeywordDao
    abstract fun topicDao(): TopicDao
    abstract fun aiConversationDao(): AiConversationDao
    abstract fun aiInsightDao(): AiInsightDao
    abstract fun categoryDao(): CategoryDao
    abstract fun seriesDao(): SeriesDao
    abstract fun bookVersionDao(): BookVersionDao
    abstract fun archiveLogDao(): ArchiveLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN processedFilePath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE pages ADD COLUMN thumbnailPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE pages ADD COLUMN processingState TEXT NOT NULL DEFAULT 'NOT_PROCESSED'")
                db.execSQL("ALTER TABLE pages ADD COLUMN processingMode TEXT NOT NULL DEFAULT 'ORIGINAL'")
                db.execSQL("ALTER TABLE pages ADD COLUMN rotationDegrees INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pages ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN ocrStatus TEXT NOT NULL DEFAULT 'NOT_PROCESSED'")
                db.execSQL("ALTER TABLE pages ADD COLUMN ocrRawText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pages ADD COLUMN ocrCleanedText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pages ADD COLUMN ocrUserEditedText TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE pages ADD COLUMN ocrLanguage TEXT NOT NULL DEFAULT 'ara+eng'")
                db.execSQL("ALTER TABLE pages ADD COLUMN ocrConfidence REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE pages ADD COLUMN ocrErrorMessage TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE pages ADD COLUMN ocrUpdatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN pageType TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE pages ADD COLUMN ocrEngineUsed TEXT NOT NULL DEFAULT 'Tesseract'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ocr_regions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        pageId INTEGER NOT NULL,
                        regionType TEXT NOT NULL DEFAULT 'MAIN_TEXT',
                        left INTEGER NOT NULL,
                        top INTEGER NOT NULL,
                        right INTEGER NOT NULL,
                        bottom INTEGER NOT NULL,
                        readingOrder INTEGER NOT NULL DEFAULT 0,
                        confidence REAL NOT NULL DEFAULT 0.0,
                        rawText TEXT NOT NULL DEFAULT '',
                        cleanedText TEXT NOT NULL DEFAULT '',
                        userEditedText TEXT DEFAULT NULL,
                        engine TEXT NOT NULL DEFAULT 'Tesseract',
                        pageType TEXT NOT NULL DEFAULT 'PRINTED',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(pageId) REFERENCES pages(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ocr_regions_pageId ON ocr_regions(pageId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ocr_regions_pageId_readingOrder ON ocr_regions(pageId, readingOrder)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Extend documents table
                db.execSQL("ALTER TABLE documents ADD COLUMN authorId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE documents ADD COLUMN authorName TEXT NOT NULL DEFAULT 'غير محدد'")
                db.execSQL("ALTER TABLE documents ADD COLUMN category TEXT NOT NULL DEFAULT 'عام'")
                db.execSQL("ALTER TABLE documents ADD COLUMN originalPageCount INTEGER NOT NULL DEFAULT 0")

                // 2. Authors
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS authors (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        normalizedName TEXT NOT NULL,
                        biography TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_authors_normalizedName ON authors(normalizedName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_authors_name ON authors(name)")

                // 3. Book Metadata
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_metadata (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        subtitle TEXT NOT NULL DEFAULT '',
                        author TEXT NOT NULL DEFAULT '',
                        authorId INTEGER DEFAULT NULL,
                        coAuthors TEXT NOT NULL DEFAULT '',
                        translator TEXT NOT NULL DEFAULT '',
                        editor TEXT NOT NULL DEFAULT '',
                        publisher TEXT NOT NULL DEFAULT '',
                        publicationYear TEXT NOT NULL DEFAULT '',
                        edition TEXT NOT NULL DEFAULT '',
                        isbn TEXT NOT NULL DEFAULT '',
                        language TEXT NOT NULL DEFAULT 'ar',
                        category TEXT NOT NULL DEFAULT 'عام',
                        subject TEXT NOT NULL DEFAULT '',
                        tags TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        originalPageCount INTEGER NOT NULL DEFAULT 0,
                        detectionStatus TEXT NOT NULL DEFAULT 'UNKNOWN',
                        archiveDate INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(authorId) REFERENCES authors(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_book_metadata_documentId ON book_metadata(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_metadata_authorId ON book_metadata(authorId)")

                // 4. Chapters
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chapters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        parentChapterId INTEGER DEFAULT NULL,
                        title TEXT NOT NULL,
                        normalizedTitle TEXT NOT NULL DEFAULT '',
                        startPageIndex INTEGER NOT NULL,
                        endPageIndex INTEGER NOT NULL,
                        readingOrder INTEGER NOT NULL DEFAULT 0,
                        level INTEGER NOT NULL DEFAULT 1,
                        detectionSource TEXT NOT NULL DEFAULT 'AUTO_HEADING',
                        confidence REAL NOT NULL DEFAULT 1.0,
                        manuallyConfirmed INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_documentId ON chapters(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_documentId_readingOrder ON chapters(documentId, readingOrder)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_documentId_startPageIndex ON chapters(documentId, startPageIndex)")

                // 5. Table of contents
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS toc_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        targetPhysicalPageIndex INTEGER NOT NULL DEFAULT 0,
                        targetPrintedPage TEXT NOT NULL DEFAULT '',
                        linkedChapterId INTEGER DEFAULT NULL,
                        level INTEGER NOT NULL DEFAULT 1,
                        readingOrder INTEGER NOT NULL DEFAULT 0,
                        confidence REAL NOT NULL DEFAULT 1.0,
                        detectionStatus TEXT NOT NULL DEFAULT 'DETECTED',
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(linkedChapterId) REFERENCES chapters(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_toc_entries_documentId ON toc_entries(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_toc_entries_documentId_readingOrder ON toc_entries(documentId, readingOrder)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_toc_entries_linkedChapterId ON toc_entries(linkedChapterId)")

                // 6. Tables
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tables (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        pageId INTEGER NOT NULL,
                        pageIndex INTEGER NOT NULL DEFAULT 0,
                        title TEXT NOT NULL DEFAULT '',
                        `left` INTEGER NOT NULL DEFAULT 0,
                        top INTEGER NOT NULL DEFAULT 0,
                        `right` INTEGER NOT NULL DEFAULT 0,
                        bottom INTEGER NOT NULL DEFAULT 0,
                        rowCount INTEGER NOT NULL DEFAULT 0,
                        columnCount INTEGER NOT NULL DEFAULT 0,
                        confidence REAL NOT NULL DEFAULT 0.0,
                        rawStructureJson TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(pageId) REFERENCES pages(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tables_documentId ON tables(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tables_pageId ON tables(pageId)")

                // 7. Table cells
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS table_cells (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tableId INTEGER NOT NULL,
                        rowIndex INTEGER NOT NULL DEFAULT 0,
                        columnIndex INTEGER NOT NULL DEFAULT 0,
                        rowSpan INTEGER NOT NULL DEFAULT 1,
                        colSpan INTEGER NOT NULL DEFAULT 1,
                        text TEXT NOT NULL DEFAULT '',
                        confidence REAL NOT NULL DEFAULT 0.0,
                        FOREIGN KEY(tableId) REFERENCES tables(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_table_cells_tableId ON table_cells(tableId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_table_cells_tableId_rowIndex_columnIndex ON table_cells(tableId, rowIndex, columnIndex)")

                // 8. Printed page numbers
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS printed_page_numbers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        pageId INTEGER NOT NULL,
                        physicalPageIndex INTEGER NOT NULL,
                        printedPageNumber TEXT NOT NULL,
                        confidence REAL NOT NULL DEFAULT 1.0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(pageId) REFERENCES pages(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_printed_page_numbers_documentId ON printed_page_numbers(documentId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_printed_page_numbers_pageId ON printed_page_numbers(pageId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_printed_page_numbers_documentId_physicalPageIndex ON printed_page_numbers(documentId, physicalPageIndex)")

                // 9. Bookmarks
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        pageId INTEGER NOT NULL,
                        pageIndex INTEGER NOT NULL DEFAULT 0,
                        chapterTitle TEXT NOT NULL DEFAULT '',
                        title TEXT NOT NULL DEFAULT '',
                        note TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(pageId) REFERENCES pages(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_documentId ON bookmarks(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_pageId ON bookmarks(pageId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_documentId_pageIndex ON bookmarks(documentId, pageIndex)")

                // 10. Book Notes
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        pageId INTEGER DEFAULT NULL,
                        pageIndex INTEGER DEFAULT NULL,
                        chapterId INTEGER DEFAULT NULL,
                        selectedText TEXT NOT NULL DEFAULT '',
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_notes_documentId ON book_notes(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_notes_documentId_pageIndex ON book_notes(documentId, pageIndex)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_pageId ON bookmarks(pageId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. ai_models
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_models (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        modelId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        format TEXT NOT NULL,
                        quantization TEXT NOT NULL,
                        filePath TEXT,
                        fileSize INTEGER NOT NULL DEFAULT 0,
                        sha256 TEXT,
                        contextLength INTEGER NOT NULL DEFAULT 2048,
                        supportedTasks TEXT NOT NULL DEFAULT 'SUMMARIZATION,QA,TOPICS,KEYWORDS',
                        isBuiltIn INTEGER NOT NULL DEFAULT 1,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        installedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ai_models_modelId ON ai_models(modelId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_models_type ON ai_models(type)")

                // 2. text_chunks
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS text_chunks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        pageId INTEGER NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        chapterId INTEGER,
                        sectionTitle TEXT,
                        physicalPageNumber INTEGER NOT NULL DEFAULT 1,
                        printedPageNumber TEXT,
                        chunkIndex INTEGER NOT NULL DEFAULT 0,
                        text TEXT NOT NULL,
                        normalizedText TEXT NOT NULL DEFAULT '',
                        tokenCount INTEGER NOT NULL DEFAULT 0,
                        startOffset INTEGER NOT NULL DEFAULT 0,
                        endOffset INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(pageId) REFERENCES pages(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_text_chunks_documentId ON text_chunks(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_text_chunks_pageId ON text_chunks(pageId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_text_chunks_chapterId ON text_chunks(chapterId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_text_chunks_documentId_pageIndex ON text_chunks(documentId, pageIndex)")

                // 3. embeddings
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS embeddings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        chunkId INTEGER,
                        targetType TEXT NOT NULL DEFAULT 'CHUNK',
                        targetId INTEGER NOT NULL DEFAULT 0,
                        modelId TEXT NOT NULL DEFAULT 'raqeem-embed-ar-v1',
                        modelVersion TEXT NOT NULL DEFAULT '1.0',
                        dimensions INTEGER NOT NULL DEFAULT 128,
                        vectorBlob BLOB NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_embeddings_documentId ON embeddings(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_embeddings_chunkId ON embeddings(chunkId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_embeddings_targetType_targetId ON embeddings(targetType, targetId)")

                // 4. summaries
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS summaries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        targetType TEXT NOT NULL DEFAULT 'BOOK',
                        targetId INTEGER NOT NULL DEFAULT 0,
                        title TEXT NOT NULL DEFAULT '',
                        content TEXT NOT NULL,
                        keyIdeasJson TEXT NOT NULL DEFAULT '[]',
                        sourcePagesJson TEXT NOT NULL DEFAULT '[]',
                        sourceChunksJson TEXT NOT NULL DEFAULT '[]',
                        modelUsed TEXT NOT NULL DEFAULT 'raqeem-arabic-rag-v1',
                        modelVersion TEXT NOT NULL DEFAULT '1.0',
                        isUserEdited INTEGER NOT NULL DEFAULT 0,
                        status TEXT NOT NULL DEFAULT 'COMPLETED',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_summaries_documentId ON summaries(documentId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_summaries_documentId_targetType_targetId ON summaries(documentId, targetType, targetId)")

                // 5. keywords
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS keywords (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        term TEXT NOT NULL,
                        normalizedTerm TEXT NOT NULL,
                        occurrenceCount INTEGER NOT NULL DEFAULT 1,
                        relatedChapterIdsJson TEXT NOT NULL DEFAULT '[]',
                        relatedPagesJson TEXT NOT NULL DEFAULT '[]',
                        status TEXT NOT NULL DEFAULT 'AUTO_DETECTED',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_keywords_documentId ON keywords(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_keywords_documentId_normalizedTerm ON keywords(documentId, normalizedTerm)")

                // 6. topics
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS topics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        topic TEXT NOT NULL,
                        normalizedTopic TEXT NOT NULL,
                        confidence REAL NOT NULL DEFAULT 0.85,
                        sourceDescription TEXT NOT NULL DEFAULT '',
                        relatedChapterIdsJson TEXT NOT NULL DEFAULT '[]',
                        relatedPagesJson TEXT NOT NULL DEFAULT '[]',
                        status TEXT NOT NULL DEFAULT 'AUTO_DETECTED',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_documentId ON topics(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_documentId_normalizedTopic ON topics(documentId, normalizedTopic)")

                // 7. ai_conversations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_conversations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER,
                        title TEXT NOT NULL DEFAULT 'محادثة جديدة',
                        chapterId INTEGER,
                        pageIndex INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_conversations_documentId ON ai_conversations(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_conversations_updatedAt ON ai_conversations(updatedAt)")

                // 8. ai_messages
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        conversationId INTEGER NOT NULL,
                        sender TEXT NOT NULL,
                        content TEXT NOT NULL,
                        sourceQuality TEXT NOT NULL DEFAULT 'GOOD',
                        isGrounded INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(conversationId) REFERENCES ai_conversations(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_messages_conversationId ON ai_messages(conversationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_messages_createdAt ON ai_messages(createdAt)")

                // 9. ai_citations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_citations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        messageId INTEGER NOT NULL,
                        documentId INTEGER NOT NULL,
                        documentTitle TEXT NOT NULL,
                        chapterId INTEGER,
                        chapterTitle TEXT,
                        pageIndex INTEGER NOT NULL,
                        physicalPage INTEGER NOT NULL,
                        printedPage TEXT,
                        quoteSnippet TEXT NOT NULL,
                        relevanceScore REAL NOT NULL DEFAULT 1.0,
                        FOREIGN KEY(messageId) REFERENCES ai_messages(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_citations_messageId ON ai_citations(messageId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_citations_documentId_pageIndex ON ai_citations(documentId, pageIndex)")

                // 10. ai_insights
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_insights (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        insightType TEXT NOT NULL,
                        primaryId INTEGER NOT NULL DEFAULT 0,
                        secondaryId INTEGER,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        score REAL NOT NULL DEFAULT 0.0,
                        isSaved INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_insights_documentId ON ai_insights(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_insights_insightType ON ai_insights(insightType)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. categories
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        normalizedName TEXT NOT NULL,
                        colorHex TEXT NOT NULL DEFAULT '#1A4D2E',
                        iconName TEXT NOT NULL DEFAULT 'category',
                        description TEXT NOT NULL DEFAULT '',
                        parentCategoryId INTEGER DEFAULT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_normalizedName ON categories(normalizedName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_name ON categories(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_parentCategoryId ON categories(parentCategoryId)")

                // 2. book_categories
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_categories (
                        documentId INTEGER NOT NULL,
                        categoryId INTEGER NOT NULL,
                        assignedAt INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(documentId, categoryId),
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_categories_documentId ON book_categories(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_categories_categoryId ON book_categories(categoryId)")

                // 3. series
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS series (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        normalizedTitle TEXT NOT NULL,
                        authorName TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL DEFAULT '',
                        totalVolumes INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_series_normalizedTitle ON series(normalizedTitle)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_series_title ON series(title)")

                // 4. book_series
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_series (
                        documentId INTEGER NOT NULL,
                        seriesId INTEGER NOT NULL,
                        volumeNumber INTEGER NOT NULL DEFAULT 1,
                        volumeTitle TEXT NOT NULL DEFAULT '',
                        addedAt INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(documentId, seriesId),
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(seriesId) REFERENCES series(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_series_documentId ON book_series(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_series_seriesId ON book_series(seriesId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_series_seriesId_volumeNumber ON book_series(seriesId, volumeNumber)")

                // 5. book_authors
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_authors (
                        documentId INTEGER NOT NULL,
                        authorId INTEGER NOT NULL,
                        role TEXT NOT NULL DEFAULT 'AUTHOR',
                        assignedAt INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(documentId, authorId, role),
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(authorId) REFERENCES authors(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_authors_documentId ON book_authors(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_authors_authorId ON book_authors(authorId)")

                // 6. Fast search & sorting indexes on documents
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_updatedAt ON documents(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_title ON documents(title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_authorId ON documents(authorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_isFavorite ON documents(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_category ON documents(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_createdAt ON documents(createdAt)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. book_versions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_versions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        versionNumber INTEGER NOT NULL,
                        versionTag TEXT NOT NULL,
                        description TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        checksumSha256 TEXT NOT NULL DEFAULT '',
                        metadataSnapshotJson TEXT NOT NULL DEFAULT '',
                        pagesSnapshotJson TEXT NOT NULL DEFAULT '',
                        changeSummary TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_versions_documentId ON book_versions(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_versions_documentId_versionNumber ON book_versions(documentId, versionNumber)")

                // 2. archive_logs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS archive_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        actionType TEXT NOT NULL,
                        documentId INTEGER,
                        status TEXT NOT NULL,
                        details TEXT NOT NULL DEFAULT '',
                        checksumSha256 TEXT NOT NULL DEFAULT '',
                        fileSize INTEGER NOT NULL DEFAULT 0,
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_archive_logs_timestamp ON archive_logs(timestamp)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "raqeem_database"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9
                    )
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
