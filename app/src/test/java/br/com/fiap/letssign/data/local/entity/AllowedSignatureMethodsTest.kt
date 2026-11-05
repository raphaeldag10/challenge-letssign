package br.com.fiap.letssign.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class AllowedSignatureMethodsTest {

    @Test
    fun defaultStorageAllowsEveryMethod() {
        val document = DocumentEntity(title = "t", filePath = "f", createdById = 1)

        assertEquals(SignatureMethod.entries.toSet(), document.allowedMethods())
        assertEquals(ALL_SIGNATURE_METHODS, SignatureMethod.entries.toSet().toStorage())
    }

    @Test
    fun storageRoundTripsSubsetInEnumOrder() {
        val chosen = setOf(SignatureMethod.CERTIFICATE, SignatureMethod.DRAW)

        val stored = chosen.toStorage()

        assertEquals("DRAW,CERTIFICATE", stored)
        val document = DocumentEntity(title = "t", filePath = "f", createdById = 1, allowedSignatureMethods = stored)
        assertEquals(chosen, document.allowedMethods())
    }

    @Test
    fun unknownNamesAreIgnored() {
        val document = DocumentEntity(title = "t", filePath = "f", createdById = 1, allowedSignatureMethods = "TYPED,FOO")

        assertEquals(setOf(SignatureMethod.TYPED), document.allowedMethods())
    }
}
