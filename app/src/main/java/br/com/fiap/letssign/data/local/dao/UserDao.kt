package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.data.local.entity.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'SIGNATORY' ORDER BY fullName ASC")
    fun getAllSignatories(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role")
    suspend fun getUsersByRole(role: UserRole): List<UserEntity>
}