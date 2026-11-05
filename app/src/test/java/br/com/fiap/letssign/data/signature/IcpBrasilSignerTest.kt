package br.com.fiap.letssign.data.signature

import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.CertificatePolicies
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.PolicyInformation
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Date

class IcpBrasilSignerTest {

    private val cpf = "52998224725" // CPF de exemplo com dígitos verificadores válidos.
    private val password = "senha-teste".toCharArray()
    private val pdf = "%PDF-1.4 conteudo de teste".toByteArray()
    private val now = Date()
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun signsPdfWithIcpBrasilCertificate() {
        val (cert, pkcs12) = certificate(cpf = cpf, policy = "2.16.76.1.2.3.1", notAfter = Date(now.time + 365 * day))

        val result = IcpBrasilSigner.signPdf(pkcs12, password, cpf, pdf, now)

        assertEquals("JOAO DA SILVA", result.signerName)
        assertTrue(IcpBrasilSigner.verify(Base64.getDecoder().decode(result.cmsBase64), pdf, cert))
    }

    @Test
    fun signatureDoesNotVerifyForAlteredPdf() {
        val (cert, pkcs12) = certificate(cpf = cpf, policy = "2.16.76.1.2.1.1", notAfter = Date(now.time + 365 * day))
        val result = IcpBrasilSigner.signPdf(pkcs12, password, cpf, pdf, now)

        val altered = "%PDF-1.4 conteudo alterado".toByteArray()
        assertFalse(IcpBrasilSigner.verify(Base64.getDecoder().decode(result.cmsBase64), altered, cert))
    }

    @Test
    fun rejectsWrongPassword() {
        val (_, pkcs12) = certificate(cpf = cpf, policy = "2.16.76.1.2.3.1", notAfter = Date(now.time + 365 * day))

        assertThrows(IllegalArgumentException::class.java) {
            IcpBrasilSigner.signPdf(pkcs12, "outra".toCharArray(), cpf, pdf, now)
        }
    }

    @Test
    fun rejectsCpfDifferentFromProfile() {
        val (_, pkcs12) = certificate(cpf = cpf, policy = "2.16.76.1.2.3.1", notAfter = Date(now.time + 365 * day))

        assertThrows(IllegalArgumentException::class.java) {
            IcpBrasilSigner.signPdf(pkcs12, password, "11144477735", pdf, now)
        }
    }

    @Test
    fun rejectsCertificateNotFromIcpBrasil() {
        val (_, pkcs12) = certificate(cpf = cpf, policy = "1.2.3.4", notAfter = Date(now.time + 365 * day))

        assertThrows(IllegalArgumentException::class.java) {
            IcpBrasilSigner.signPdf(pkcs12, password, cpf, pdf, now)
        }
    }

    @Test
    fun rejectsExpiredCertificate() {
        val (_, pkcs12) = certificate(cpf = cpf, policy = "2.16.76.1.2.3.1", notAfter = Date(now.time - day))

        assertThrows(IllegalArgumentException::class.java) {
            IcpBrasilSigner.signPdf(pkcs12, password, cpf, pdf, now)
        }
    }

    /** Gera um certificado autoassinado com as extensões ICP-Brasil e o devolve junto do PKCS#12. */
    private fun certificate(cpf: String, policy: String, notAfter: Date): Pair<X509Certificate, ByteArray> {
        val keys: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val subject = X500Name("CN=JOAO DA SILVA:$cpf, O=ICP-Brasil Teste, C=BR")
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(42),
            Date(now.time - day),
            notAfter,
            subject,
            keys.public
        )
        builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature or KeyUsage.nonRepudiation))
        builder.addExtension(
            Extension.certificatePolicies,
            false,
            CertificatePolicies(PolicyInformation(ASN1ObjectIdentifier(policy)))
        )
        val cpfOther = DERSequence(
            arrayOf<ASN1Encodable>(
                ASN1ObjectIdentifier("2.16.76.1.3.1"),
                DERTaggedObject(true, 0, DERUTF8String("01011980$cpf"))
            )
        )
        builder.addExtension(
            Extension.subjectAlternativeName,
            false,
            GeneralNames(GeneralName(GeneralName.otherName, cpfOther))
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keys.private)
        val cert = JcaX509CertificateConverter().getCertificate(builder.build(signer))

        val store = KeyStore.getInstance("PKCS12")
        store.load(null, null)
        store.setKeyEntry("assinante", keys.private, password, arrayOf(cert))
        val out = ByteArrayOutputStream()
        store.store(out, password)
        return cert to out.toByteArray()
    }
}
