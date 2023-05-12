package com.iotconnectsdk.utils

import android.os.Build
import org.eclipse.paho.client.mqttv3.MqttSecurityException
import java.io.IOException
import java.io.InputStream
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory


internal object  SecurityHelper1 {
    @Throws(MqttSecurityException::class)
    fun getSSLSocketFactory(keyStore: InputStream?, password: String): SSLSocketFactory? {
        return try {
            var ctx: SSLContext? = null
            var sslSockFactory: SSLSocketFactory? = null
            val ks: KeyStore
            ks = KeyStore.getInstance("PKCS12")
            ks.load(keyStore, password.toCharArray())
            val tmf = TrustManagerFactory.getInstance("X509")
            tmf.init(ks)
            val tm = tmf.trustManagers
            ctx = SSLContext.getInstance("TLS")
            ctx.init(null, tm, null)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
               // sslSockFactory = TLSSocketFactory(tm)
            } else {
                sslSockFactory = ctx.socketFactory
            }
            sslSockFactory
        } catch (e: KeyStoreException) {
            throw MqttSecurityException(e)
        } catch (e: CertificateException) {
            throw MqttSecurityException(e)
        } catch (e: IOException) {
            throw MqttSecurityException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw MqttSecurityException(e)
        } catch (e: KeyManagementException) {
            throw MqttSecurityException(e)
        }
    }
}