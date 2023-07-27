package com.iotconnectsdk.utils


import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory


internal object SecurityHelper {
    fun createSocketFactory(
        caFile: String?,
        clientCrtFile: String?,
        clientKeyFile: String?,
        clientKeyPassword: String,
        companySID: String?,
        deviceUniqueID: String?
    ): SSLSocketFactory? {

        // Creates a TLS socket factory with the given
        // CA certificate file, client certificate, client key
        // In this case, we are working without a client key password
        return try {
            Security.addProvider(BouncyCastleProvider())
            val keyManagers = createKeyManagerFactory(
                clientCrtFile,
                clientKeyFile,
                clientKeyPassword,
                companySID,
                deviceUniqueID
            )?.keyManagers
            val trustManagers = createTrustManagerFactory(caFile, companySID, deviceUniqueID)!!
                .trustManagers

            // Create the TLS socket factory for the desired TLS version
            val context = SSLContext.getInstance("TLSv1.2")
            context.init(keyManagers, trustManagers, SecureRandom())
            context.socketFactory
        } catch (e: Exception) {
            e.printStackTrace()
            //            CustomLogger.writeErrorLog(logger, "ERR_IN01", cpId, uniqueId, "SDK cannot create the TLS socket factory");
            null
        }
    }

    fun createKeyManagerFactory(
        clientCertificateFileName: String?,
        clientKeyFileName: String?, clientKeyPassword: String, cpId: String?, uniqueId: String?
    ): KeyManagerFactory? {
        // Creates a key manager factory
        // Load and create the client certificate
        val clientCertificate =
            createX509CertificateFromFile(clientCertificateFileName, cpId, uniqueId)
        // Load the private client key
        val privateKey = createPrivateKeyFromPemFile(clientKeyFileName, cpId, uniqueId)
        // Client key and certificate are sent to server
        val keyStore: KeyStore
        return try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("certificate", clientCertificate)
            keyStore.setKeyEntry(
                "private-key",
                privateKey,
                clientKeyPassword.toCharArray(),
                arrayOf<Certificate?>(clientCertificate)
            )
            val keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, clientKeyPassword.toCharArray())
            keyManagerFactory
        } catch (e: Exception) {
            e.printStackTrace()
            //            CustomLogger.writeErrorLog(logger, "ERR_IN01", cpId, uniqueId, e.getMessage());
            null
        }
    }

    fun createTrustManagerFactory(
        caCertificateFileName: String?, cpId: String?,
        uniqueId: String?
    ): TrustManagerFactory? {
        // Creates a trust manager factory
        // Load CA certificate
        // CA certificate is used to authenticate server
        val keyStore: KeyStore
        return try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry(
                "ca-certificate",
                createX509CertificateFromFile(caCertificateFileName, cpId, uniqueId)
            )
            val trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            trustManagerFactory
        } catch (e: Exception) {
            e.printStackTrace()
            //            CustomLogger.writeErrorLog(logger, "ERR_IN01", cpId, uniqueId, e.getMessage());
            null
        }
    }

    private fun createX509CertificateFromFile(
        certificateFileName: String?,
        cpId: String?,
        uniqueId: String?
    ): X509Certificate? {
        // Load an X509 certificate from the specified certificate file name
        //File file = new java.io.File(certificateFileName);
        val file: File = File(certificateFileName!!)
        if (!file.isFile) {
//            CustomLogger.writeErrorLog(logger, "ERR_IN06", cpId, uniqueId);
            return null
        }
        val certificateFactoryX509: CertificateFactory
        return try {
            certificateFactoryX509 = CertificateFactory.getInstance("X.509")
            val inputStream: InputStream = FileInputStream(file)
            val certificate =
                certificateFactoryX509.generateCertificate(inputStream) as X509Certificate
            inputStream.close()
            certificate
        } catch (e: Exception) {
            e.printStackTrace()
            //            CustomLogger.writeErrorLog(logger, "ERR_IN01", cpId, uniqueId, e.getMessage());
            null
        }
    }

    private fun createPrivateKeyFromPemFile(
        keyFileName: String?,
        cpId: String?,
        uniqueId: String?
    ): PrivateKey? {
        // Loads a privte key from the specified key file name
        //File keyfile = new java.io.File(keyFileName);
        val keyfile: File = File(keyFileName)
        if (!keyfile.isFile) {
//            CustomLogger.writeErrorLog(logger, "ERR_IN06", cpId, uniqueId);
            return null
        }
        val pemReader: PemReader
        return try {
            pemReader = PemReader(FileReader(keyfile))
            val pemObject = pemReader.readPemObject()
            val pemContent = pemObject.content
            pemReader.close()
            val encodedKeySpec =
                PKCS8EncodedKeySpec(pemContent)
            val keyFactory = keyFactoryInstance
            keyFactory.generatePrivate(encodedKeySpec)
        } catch (e: Exception) {
            e.printStackTrace()
            //            CustomLogger.writeErrorLog(logger, "ERR_IN01", cpId, uniqueId, e.toString());
            null
        }
    }

    @get:Throws(NoSuchAlgorithmException::class)
    private val keyFactoryInstance: KeyFactory
        private get() = KeyFactory.getInstance("RSA")
}