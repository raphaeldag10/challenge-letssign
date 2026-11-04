package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class RoomUserRepository(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun authenticate(email: String, passwordHash: String): UserEntity? {
        val user = userDao.getUserByEmail(email)
        return if (user != null && user.passwordHash == passwordHash) user else null
    }

    override suspend fun registerSignatory(signatory: UserEntity): Long {
        return userDao.insertUser(signatory)
    }

    override suspend fun getUserById(id: Long): UserEntity? {
        return userDao.getUserById(id)
    }

    override fun getAllSignatories(): Flow<List<UserEntity>> {
        return userDao.getAllSignatories()
    }
}