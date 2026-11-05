package br.com.fiap.letssign.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.fiap.letssign.data.local.converter.Converters
import br.com.fiap.letssign.data.local.dao.AiDao
import br.com.fiap.letssign.data.local.dao.DocumentDao
import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.ALL_SIGNATURE_METHODS
import br.com.fiap.letssign.data.local.entity.AiMessageEntity
import br.com.fiap.letssign.data.local.entity.DocumentAiSummaryEntity
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.DocumentHistoryEntity
import br.com.fiap.letssign.data.local.entity.DocumentSignatoryEntity
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        DocumentEntity::class,
        DocumentSignatoryEntity::class,
        SignatureFieldEntity::class,
        DocumentHistoryEntity::class,
        DocumentAiSummaryEntity::class,
        AiMessageEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun documentDao(): DocumentDao
    abstract fun aiDao(): AiDao

    companion object {
        /** Acrescenta o nome original do PDF sem apagar os documentos existentes. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN fileName TEXT")
            }
        }

        /** Acrescenta CPF e a marca de CPF cadastrado pelo admin, sem apagar os usuários existentes. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN cpf TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN cpfLockedByAdmin INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Registra o modo de assinatura e o nome do signatário; assinaturas antigas ficam nulas (desenho). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE document_signatories ADD COLUMN signatureMethod TEXT")
                db.execSQL("ALTER TABLE document_signatories ADD COLUMN signerName TEXT")
            }
        }

        /** Guarda a assinatura CAdES e os dados do certificado ICP-Brasil nas assinaturas com certificado. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE document_signatories ADD COLUMN signatureCms TEXT")
                db.execSQL("ALTER TABLE document_signatories ADD COLUMN certificateSerial TEXT")
                db.execSQL("ALTER TABLE document_signatories ADD COLUMN certificateIssuer TEXT")
            }
        }

        /** Acrescenta os modos de assinatura aceitos por documento; documentos antigos aceitam todos. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN allowedSignatureMethods TEXT NOT NULL DEFAULT '$ALL_SIGNATURE_METHODS'")
            }
        }

        /** Cria as tabelas do assistente de IA (resumo em cache e chat de dúvidas). Não altera dados existentes. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `document_ai_summaries` (
                        `documentId` INTEGER NOT NULL,
                        `summary` TEXT NOT NULL,
                        `textHash` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`documentId`),
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `documentId` INTEGER NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_documentId` ON `ai_messages` (`documentId`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "letssign_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    // Versões anteriores à 3 ainda recriam o banco (sem dados reais antes da entrega).
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
