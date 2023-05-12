package com.iotconnectsdk.utils


import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.security.KeyStore
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory


/**
 * @Author uttam.kachhad
 * @Create 16/03/2022
 * @Description
 */
internal object SecurityHelper2 {
    @Throws(Exception::class)
    fun getSocketFactory2(
        caCrtFile: InputStream?, crtFile: InputStream?, keyFile: InputStream?,
        password: String
    ): SSLSocketFactory? {
        Security.addProvider(BouncyCastleProvider())

        // load CA certificate
        var caCert: X509Certificate? = null
        var bis = BufferedInputStream(caCrtFile)
        val cf = CertificateFactory.getInstance("X.509")
        while (bis.available() > 0) {
            caCert = cf.generateCertificate(bis) as X509Certificate
        }

        // load client certificate
        bis = BufferedInputStream(crtFile)
        var cert: X509Certificate? = null
        while (bis.available() > 0) {
            cert = cf.generateCertificate(bis) as X509Certificate
        }

        // load client private cert
        val pemParser = PEMParser(InputStreamReader(keyFile))
        val `object` = pemParser.readObject()
        val converter = JcaPEMKeyConverter().setProvider("BC")
        val key = converter.getKeyPair(`object` as PEMKeyPair)
        val caKs = KeyStore.getInstance(KeyStore.getDefaultType())
        caKs.load(null, null)
        caKs.setCertificateEntry("cert-certificate", caCert)
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(caKs)
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        ks.load(null, null)
        ks.setCertificateEntry("certificate", cert)
        ks.setKeyEntry(
            "private-cert",
            key.private,
            password.toCharArray(),
            arrayOf<Certificate?>(cert)
        )
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, password.toCharArray())
        val context = SSLContext.getInstance("TLSv1.2")
        context.init(kmf.keyManagers, tmf.trustManagers, null)
        return context.socketFactory
    }

}