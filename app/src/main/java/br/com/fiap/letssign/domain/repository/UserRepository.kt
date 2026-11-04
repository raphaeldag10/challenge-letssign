package br.com.fiap.letssign.domain.repository

import br.com.fiap.letssign.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun authenticate(email: String, passwordHash: String): UserEntity?
    suspend fun registerSignatory(signatory: UserEntity): Long
    suspend fun getUserById(id: Long): UserEntity?
    fun getAllSignatories(): Flow<List<UserEntity>>
}