package br.com.fiap.letssign.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.fiap.letssign.data.local.converter.Converters
import br.com.fiap.letssign.data.local.dao.DocumentDao
import br.com.fiap.letssign.data.local.dao.UserDao
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
        DocumentHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "letssign_database"
                )
                    // Antes da entrega não há dados reais; a versão 2 recria o banco.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
