package br.com.fiap.letssign.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fiap.letssign.data.local.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Aplica a MIGRATION_8_9 num banco criado na versão 8 e confere que as tabelas do assistente
 * existem e que os documentos antigos continuam lá. Roda em dispositivo ou emulador.
 */
@RunWith(AndroidJUnit4::class)
class AiMigrationInstrumentedTest {

    private val dbName = "ai-migration-test.db"
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase(dbName)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Só a tabela que o FK referencia; o restante do esquema v8 não é usado pela migração.
                    db.execSQL("CREATE TABLE documents (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL)")
                    db.execSQL("INSERT INTO documents (title) VALUES ('Contrato antigo')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(config)
    }

    @After
    fun tearDown() {
        helper.close()
        ApplicationProvider.getApplicationContext<android.content.Context>().deleteDatabase(dbName)
    }

    @Test
    fun migrationCreatesAssistantTablesAndKeepsExistingDocuments() {
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_8_9.migrate(db)

        assertTrue(tableExists(db, "document_ai_summaries"))
        assertTrue(tableExists(db, "ai_messages"))
        assertTrue(indexExists(db, "index_ai_messages_documentId"))

        db.execSQL("INSERT INTO ai_messages (documentId, userId, role, content, createdAt) VALUES (1, 1, 'USER', 'oi', 0)")
        db.execSQL("INSERT INTO document_ai_summaries (documentId, summary, textHash, createdAt) VALUES (1, 'resumo', 'h', 0)")

        db.query("SELECT COUNT(*) FROM documents").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
    }

    private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(name)).use { it.count == 1 }

    private fun indexExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name=?", arrayOf(name)).use { it.count == 1 }
}
