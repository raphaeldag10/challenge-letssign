package br.com.fiap.letssign

import android.app.Application
import android.content.Context
import br.com.fiap.letssign.data.file.FileStorage
import br.com.fiap.letssign.data.local.AppDatabase
import br.com.fiap.letssign.data.repository.DocumentService
import br.com.fiap.letssign.data.repository.RoomDocumentRepository
import br.com.fiap.letssign.data.repository.UserRepository

/** Dependências compartilhadas pelo app. Uma única instância por processo. */
class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)
    val userRepository = UserRepository(db.userDao())
    val documentRepository = RoomDocumentRepository(db)
    val files = FileStorage(context.applicationContext)
    val documentService = DocumentService(documentRepository, files)
}

class LetsSignApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
