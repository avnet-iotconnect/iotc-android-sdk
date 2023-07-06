package com.iotconnectsdk.utils

import android.annotation.SuppressLint
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
import java.io.FileReader
import java.security.KeyPair
import java.security.KeyStore
import java.security.Security
import java.security.cert.Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory


internal object DateTimeUtils {

    val currentDate: String
        get() {
            @SuppressLint("SimpleDateFormat") val df =
                SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'")
            df.timeZone = TimeZone.getTimeZone("gmt")
            return df.format(Date())
        }

     fun getCurrentTime(): Long {
        return System.currentTimeMillis() / 1000
    }

    fun getSocketFactory( caCrtFile: String?,
                          crtFile: String?,
                          keyFile: String?,
                          password: String,): SSLSocketFactory?{

        try {
            /**
             * Add BouncyCastle as a Security Provider
             */
            Security.addProvider(BouncyCastleProvider())
            val certificateConverter = JcaX509CertificateConverter().setProvider(BouncyCastleProvider())

            /**
             * Load Certificate Authority (CA) certificate
             */
            var reader = PEMParser(FileReader(caCrtFile))
            val caCertHolder = reader.readObject() as X509CertificateHolder
            reader.close()
            val caCert = certificateConverter.getCertificate(caCertHolder)
            /**
             * Load client certificate
             */
            reader = PEMParser(FileReader(crtFile))
            val certHolder = reader.readObject() as X509CertificateHolder
            reader.close()
            val cert = certificateConverter.getCertificate(certHolder)
            /**
             * Load client private key
             */
            reader = PEMParser(FileReader(keyFile))
            val keyObject = reader.readObject()
            reader.close()
            val provider = JcePEMDecryptorProviderBuilder().build(password.toCharArray())
            val keyConverter = JcaPEMKeyConverter().setProvider(BouncyCastleProvider())
            val key: KeyPair
            key = if (keyObject is PEMEncryptedKeyPair) {
                keyConverter.getKeyPair((keyObject as PEMEncryptedKeyPair).decryptKeyPair(provider))
            } else {
                keyConverter.getKeyPair(keyObject as PEMKeyPair)
            }
            /**
             * CA certificate is used to authenticate server
             */
            val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            caKeyStore.load(null, null)
            caKeyStore.setCertificateEntry("ca-certificate", caCert)
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(caKeyStore)
            /**
             * Client key and certificates are sent to server so it can authenticate the client
             */
            val clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            clientKeyStore.load(null, null)
            clientKeyStore.setCertificateEntry("certificate", cert)
            clientKeyStore.setKeyEntry(
                "private-key",
                key.private,
                password.toCharArray(),
                arrayOf<Certificate>(cert)
            )
            val keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
            )
            keyManagerFactory.init(clientKeyStore, password.toCharArray())
            /**
             * Create SSL socket factory
             */
            val context = SSLContext.getInstance("TLSv1.2")
            context.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
            /**
             * Return the newly created socket factory object
             */
            return context.socketFactory
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

}