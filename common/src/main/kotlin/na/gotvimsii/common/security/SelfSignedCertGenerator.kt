package na.gotvimsii.common.security

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.cert.X509Certificate
import java.util.*

object SelfSignedCertGenerator {
    fun generate(keyPair: KeyPair): X509Certificate {
        val now = Date()
        val until = Date(now.time + 3650L * 1000 * 60 * 60 * 24) // 1 year seems good enough;

        val name = X500Name("CN=ECKey")
        val builder = JcaX509v3CertificateBuilder(
            name,
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            until,
            name,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withECDSA")
            .build(keyPair.private)

        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }
}