package br.com.fiap.letssign.data.local.converter

import androidx.room.TypeConverter
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.local.entity.SignatoryRoleType
import br.com.fiap.letssign.data.local.entity.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromDocumentStatus(value: DocumentStatus): String = value.name

    @TypeConverter
    fun toDocumentStatus(value: String): DocumentStatus = DocumentStatus.valueOf(value)

    @TypeConverter
    fun fromSignatoryRoleType(value: SignatoryRoleType): String = value.name

    @TypeConverter
    fun toSignatoryRoleType(value: String): SignatoryRoleType = SignatoryRoleType.valueOf(value)
}