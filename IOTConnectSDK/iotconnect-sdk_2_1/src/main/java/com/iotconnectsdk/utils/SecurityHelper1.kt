package com.iotconnectsdk.utils


import android.os.Build
import androidx.annotation.RequiresApi
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
import org.bouncycastle.util.io.pem.PemReader
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.UnrecoverableKeyException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager


internal object SecurityHelper1 {
    /**
     * Create an SslSocketFactory using PEM encrypted certificate files. Mutual SSL
     * Authentication is NOT supported.
     *
     * @param caCrtFile                  CA certificate of remote server.
     * @param serverHostnameVerification Enable/disable verification of server
     * certificate DNS and hostname.
     * @return
     * @throws CertificateException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    @Throws(
        CertificateException::class,
        IOException::class,
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        KeyManagementException::class
    )
    fun getSSLSocketFactoryLatest123(
        caCrtFile: String,
        serverHostnameVerification: Boolean
    ): SSLSocketFactory? {
        /**
         * Add BouncyCastle as a Security Provider
         */
        Security.addProvider(BouncyCastleProvider())
        val certificateConverter = JcaX509CertificateConverter().setProvider(BouncyCastleProvider())

        /**
         * Load Certificate Authority (CA) certificate
         */
        val caCertHolder = readPEMFile(caCrtFile) as X509CertificateHolder?
        val caCert: X509Certificate = certificateConverter.getCertificate(caCertHolder)

        /**
         * CA certificate is used to authenticate server
         */
        val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        caKeyStore.load(null, null)
        caKeyStore.setCertificateEntry("ca-certificate", caCert)
        /**
         * Create SSL socket factory
         */
        val context = SSLContext.getInstance("TLSv1.2")
        context.init(
            null,
            if (serverHostnameVerification) getTrustManagers(caKeyStore) else getUnsafeTrustManagers(
                caKeyStore
            ), null
        )
        /**
         * Return the newly created socket factory object
         */
        return context.socketFactory
    }

    /**
     * Create an SslSocketFactory using PEM encrypted certificate files. Mutual SSL
     * Authentication is supported.
     *
     * @param caCrtFile                  CA certificate of remote server.
     * @param crtFile                    certificate file of client.
     * @param keyFile                    key file of client.
     * @param password                   password of key file.
     * @param serverHostnameVerification Enable/disable verification of server
     * certificate DNS and hostname.
     * @return
     * @throws CertificateException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     */
    @Throws(
        CertificateException::class,
        IOException::class,
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        KeyManagementException::class,
        UnrecoverableKeyException::class
    )
    fun getSSLSocketFactory(
        caCrtFile: String, crtFile: String,
        keyFile: String, password: String, serverHostnameVerification: Boolean
    ): SSLSocketFactory? {
        /**
         * Add BouncyCastle as a Security Provider
         */
        Security.addProvider(BouncyCastleProvider())
        val certificateConverter = JcaX509CertificateConverter().setProvider(BouncyCastleProvider())

        /**
         * Load Certificate Authority (CA) certificate
         */
       // val caCertHolder = readPEMFile(caCrtFile) as X509CertificateHolder?
       // val caCert: X509Certificate = certificateConverter.getCertificate(caCertHolder)

        /**
         * Load client certificate
         */
        val certHolder = readPEMFile(crtFile) as X509CertificateHolder?
        val cert: X509Certificate = certificateConverter.getCertificate(certHolder)

        /**
         * Load client private key
         */
        val keyObject = readPEMFile(keyFile)
        val keyConverter = JcaPEMKeyConverter().setProvider(BouncyCastleProvider())
        var privateKey: PrivateKey? = null
        privateKey = if (keyObject is PEMEncryptedKeyPair) {
            val provider = JcePEMDecryptorProviderBuilder().build(password.toCharArray())
            val keyPair = keyConverter.getKeyPair(keyObject.decryptKeyPair(provider))
            keyPair.private
        } else if (keyObject is PEMKeyPair) {
            val keyPair = keyConverter.getKeyPair(keyObject as PEMKeyPair?)
            keyPair.private
        } else if (keyObject is PrivateKeyInfo) {
            keyConverter.getPrivateKey(keyObject as PrivateKeyInfo?)
        } else {
            throw IOException(String.format("Unsported type of keyFile %s", keyFile))
        }
        /**
         * CA certificate is used to authenticate server
         */
        val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        caKeyStore.load(null, null)
     //   caKeyStore.setCertificateEntry("ca-certificate", caCert)
        /**
         * Client key and certificates are sent to server so it can authenticate the
         * client. (server send CertificateRequest message in TLS handshake step).
         */
        val clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        clientKeyStore.load(null, null)
        clientKeyStore.setCertificateEntry("certificate", cert)
        clientKeyStore.setKeyEntry("private-key", privateKey, password.toCharArray(), arrayOf(cert))
        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(clientKeyStore, password.toCharArray())
        /**
         * Create SSL socket factory
         */
        val context = SSLContext.getInstance("TLSv1.2")
        context.init(
            keyManagerFactory.keyManagers,
            if (serverHostnameVerification) getTrustManagers(caKeyStore) else getUnsafeTrustManagers(
                caKeyStore
            ), null
        )
        /**
         * Return the newly created socket factory object
         */
        return context.socketFactory
    }

    @Throws(IOException::class)
    private fun readPEMFile(filePath: String): Any? {
        PEMParser(FileReader(filePath)).use { reader -> return reader.readObject() }
    }

    @Throws(NoSuchAlgorithmException::class, KeyStoreException::class)
    private fun getTrustManagers(caKeyStore: KeyStore): Array<TrustManager> {
        val trustManagerFactory = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(caKeyStore)
        return trustManagerFactory.trustManagers
    }

    /**
     * This method checks server and client certificates but overrides server hostname verification.
     * @param caKeyStore
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * '
     */
    @Throws(NoSuchAlgorithmException::class, KeyStoreException::class)
    private fun getUnsafeTrustManagers(caKeyStore: KeyStore): Array<TrustManager>? {
        val standardTrustManager = getTrustManagers(caKeyStore)[0] as X509TrustManager
        return arrayOf<TrustManager>(@RequiresApi(Build.VERSION_CODES.N)
        object : X509ExtendedTrustManager() {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                standardTrustManager.checkClientTrusted(chain, authType)
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                standardTrustManager.checkServerTrusted(chain, authType)
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return standardTrustManager.acceptedIssuers
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(
                chain: Array<X509Certificate?>?,
                authType: String?,
                socket: Socket?
            ) {
                standardTrustManager.checkClientTrusted(chain, authType)
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(
                chain: Array<X509Certificate?>?,
                authType: String?,
                socket: Socket?
            ) {
                standardTrustManager.checkServerTrusted(chain, authType)
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(
                chain: Array<X509Certificate?>?,
                authType: String?,
                engine: SSLEngine?
            ) {
                standardTrustManager.checkClientTrusted(chain, authType)
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(
                chain: Array<X509Certificate?>?,
                authType: String?,
                engine: SSLEngine?
            ) {
                standardTrustManager.checkServerTrusted(chain, authType)
            }
        })
    }
}