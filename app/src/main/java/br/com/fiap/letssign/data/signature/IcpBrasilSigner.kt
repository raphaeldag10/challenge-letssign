package br.com.fiap.letssign.data.signature

import br.com.fiap.letssign.data.repository.Cpf
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1String
import org.bouncycastle.asn1.ASN1TaggedObject
import org.bouncycastle.asn1.DERSet
import org.bouncycastle.asn1.cms.Attribute
import org.bouncycastle.asn1.cms.AttributeTable
import org.bouncycastle.asn1.ess.ESSCertIDv2
import org.bouncycastle.asn1.ess.SigningCertificateV2
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.CertificatePolicies
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Collections
import java.util.Date

/**
 * Resultado de uma assinatura com certificado ICP-Brasil.
 * [cmsBase64] é a assinatura CAdES-BES destacada do PDF original (DER em Base64).
 */
data class CertificateSignature(
    val cmsBase64: String,
    val signerName: String,
    val serial: String,
    val issuer: String
)

/**
 * Assinatura digital ICP-Brasil (certificado A1 em PKCS#12) no formato CAdES-BES destacado,
 * conforme o perfil de assinatura do padrão ICP-Brasil (DOC-ICP-15, baseado em ETSI EN 319 122).
 *
 * Escopo do MVP: confere validade, uso para assinatura, política ICP-Brasil e CPF do certificado.
 * Não valida a cadeia até a AC Raiz, não consulta LCR/OCSP e não inclui carimbo do tempo.
 */
object IcpBrasilSigner {
    /** Prefixo dos OIDs de políticas de certificado da ICP-Brasil (2.16.76.1.2.x). */
    private const val ICP_POLICY_PREFIX = "2.16.76.1.2."

    /** OID da extensão do CPF do titular (otherName em subjectAlternativeName). */
    private const val ICP_CPF_OID = "2.16.76.1.3.1"

    /**
     * Assina [pdf] com o certificado do arquivo [pkcs12]. Lança IllegalArgumentException com mensagem
     * para o usuário quando o arquivo, a senha ou o certificado não atendem às regras.
     */
    fun signPdf(
        pkcs12: ByteArray,
        password: CharArray,
        expectedCpf: String,
        pdf: ByteArray,
        now: Date = Date()
    ): CertificateSignature {
        val (key, cert) = loadCertificate(pkcs12, password)
        validate(cert, expectedCpf, now)
        val cms = sign(key, cert, pdf)
        check(verify(cms, pdf, cert)) { "Falha ao verificar a assinatura gerada" }
        return CertificateSignature(
            cmsBase64 = Base64.getEncoder().encodeToString(cms),
            signerName = commonName(cert),
            serial = cert.serialNumber.toString(16).uppercase(),
            issuer = cert.issuerX500Principal.name
        )
    }

    /** Confere a assinatura CMS destacada de [pdf] com a chave pública de [cert]. */
    fun verify(cmsDer: ByteArray, pdf: ByteArray, cert: X509Certificate): Boolean =
        try {
            val signed = CMSSignedData(CMSProcessableByteArray(pdf), cmsDer)
            val signer = signed.signerInfos.signers.firstOrNull()
            signer != null && signer.verify(JcaSimpleSignerInfoVerifierBuilder().build(cert))
        } catch (e: CMSException) {
            // Inclui o PDF alterado (digest não confere): a assinatura é inválida, não um erro de código.
            false
        }

    private fun loadCertificate(pkcs12: ByteArray, password: CharArray): Pair<PrivateKey, X509Certificate> {
        val store = KeyStore.getInstance("PKCS12")
        try {
            store.load(ByteArrayInputStream(pkcs12), password)
        } catch (e: Exception) {
            throw IllegalArgumentException("Senha incorreta ou arquivo de certificado inválido")
        }
        val alias = Collections.list(store.aliases()).firstOrNull { store.isKeyEntry(it) }
            ?: throw IllegalArgumentException("O arquivo não contém certificado com chave privada")
        val key = runCatching { store.getKey(alias, password) }.getOrNull() as? PrivateKey
            ?: throw IllegalArgumentException("Não foi possível ler a chave privada do certificado")
        val cert = store.getCertificate(alias) as? X509Certificate
            ?: throw IllegalArgumentException("Certificado inválido")
        require(key.algorithm == "RSA") { "Use um certificado com chave RSA" }
        return key to cert
    }

    private fun validate(cert: X509Certificate, expectedCpf: String, now: Date) {
        try {
            cert.checkValidity(now)
        } catch (e: CertificateException) {
            throw IllegalArgumentException("Certificado fora do prazo de validade")
        }
        val usage = cert.keyUsage
        require(usage != null && (usage[0] || usage[1])) { "O certificado não permite assinatura digital" }
        require(policies(cert).any { it.startsWith(ICP_POLICY_PREFIX) }) {
            "O certificado não é emitido pela ICP-Brasil"
        }
        val cpf = cpfOf(cert) ?: throw IllegalArgumentException("O certificado não contém o CPF do titular")
        require(cpf == expectedCpf) { "O CPF do certificado não é o CPF cadastrado no seu perfil" }
    }

    /** Assinatura CAdES-BES: atributos assinados com signing-certificate-v2 (SHA-256 do certificado). */
    private fun sign(key: PrivateKey, cert: X509Certificate, pdf: ByteArray): ByteArray {
        val certHash = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
        val essCert = ESSCertIDv2(AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256), certHash)
        val signingCertificate = Attribute(
            PKCSObjectIdentifiers.id_aa_signingCertificateV2,
            DERSet(SigningCertificateV2(arrayOf(essCert)))
        )
        val extraAttributes = AttributeTable(ASN1EncodableVector().apply { add(signingCertificate) })

        val signerInfo = JcaSignerInfoGeneratorBuilder(JcaDigestCalculatorProviderBuilder().build())
            .setSignedAttributeGenerator(DefaultSignedAttributeTableGenerator(extraAttributes))
            .build(JcaContentSignerBuilder("SHA256withRSA").build(key), cert)

        val generator = CMSSignedDataGenerator()
        generator.addSignerInfoGenerator(signerInfo)
        generator.addCertificates(JcaCertStore(listOf(cert)))
        // encapsulate = false: assinatura destacada, o PDF continua sendo o arquivo original.
        return generator.generate(CMSProcessableByteArray(pdf), false).encoded
    }

    private fun policies(cert: X509Certificate): List<String> {
        val raw = cert.getExtensionValue(Extension.certificatePolicies.id) ?: return emptyList()
        return CertificatePolicies.getInstance(extensionPayload(raw)).policyInformation.map { it.policyIdentifier.id }
    }

    private fun cpfOf(cert: X509Certificate): String? {
        val raw = cert.getExtensionValue(Extension.subjectAlternativeName.id) ?: return null
        for (name in GeneralNames.getInstance(extensionPayload(raw)).names) {
            if (name.tagNo != GeneralName.otherName) continue
            val seq = ASN1Sequence.getInstance(name.name)
            if (ASN1ObjectIdentifier.getInstance(seq.getObjectAt(0)).id != ICP_CPF_OID) continue
            // O valor é "DDMMAAAA" (data de nascimento) seguido dos 11 dígitos do CPF.
            val value: ASN1Encodable = seq.getObjectAt(1).let { (it as? ASN1TaggedObject)?.baseObject ?: it }
            val text = when (value) {
                is ASN1String -> value.string
                is ASN1OctetString -> String(value.octets, Charsets.UTF_8)
                else -> null
            } ?: continue
            val digits = text.filter(Char::isDigit)
            if (digits.length >= Cpf.LENGTH) return digits.takeLast(Cpf.LENGTH)
        }
        return null
    }

    /** Extensões X.509 vêm como OCTET STRING que envolve a estrutura DER da extensão. */
    private fun extensionPayload(raw: ByteArray): ASN1Primitive =
        ASN1Primitive.fromByteArray(ASN1OctetString.getInstance(raw).octets)

    private fun commonName(cert: X509Certificate): String {
        val cn = X500Name.getInstance(cert.subjectX500Principal.encoded)
            .getRDNs(BCStyle.CN)
            .firstOrNull()
            ?.let { IETFUtils.valueToString(it.first.value) }
        // Certificados ICP-Brasil trazem o CPF no CN após ":" (ex.: "NOME COMPLETO:12345678901").
        return cn?.substringBefore(':')?.trim()?.takeIf { it.isNotEmpty() } ?: cert.subjectX500Principal.name
    }
}
