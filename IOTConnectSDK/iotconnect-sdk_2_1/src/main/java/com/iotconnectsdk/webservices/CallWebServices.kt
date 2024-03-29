package com.iotconnectsdk.webservices

import android.util.Log
import com.iotconnectsdk.utils.IotSDKUrls
import com.iotconnectsdk.utils.IotSDKWsUtils.getAPIService
import com.iotconnectsdk.webservices.interfaces.WsResponseInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

internal class CallWebServices {

    fun sync(url: String?, wsResponseInterface: WsResponseInterface) {

        val service= getAPIService(url)
        CoroutineScope(Dispatchers.IO).launch {
            val response = service?.sync(url)
            try {
                if (response != null) {
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                wsResponseInterface.onSuccessResponse(IotSDKUrls.SYNC_SERVICE, response.body()!!.string())
                            }
                        } else {

                        }
                    }
                }
            } catch (e: HttpException) {
                Log.e("REQUEST", "Exception ${e.message}")
            } catch (t: Throwable) {
                wsResponseInterface.onFailedResponse(IotSDKUrls.SYNC_SERVICE, 0, t.message)
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun getDiscoveryApi(discoveryUrl: String?, wsResponseInterface: WsResponseInterface) {

        val service= getAPIService(discoveryUrl)
        CoroutineScope(Dispatchers.IO).launch {
            val response = service?.getDiscoveryApi(discoveryUrl)
            try {
                if (response != null) {
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                wsResponseInterface.onSuccessResponse(IotSDKUrls.DISCOVERY_SERVICE, response.body()!!.string())
                            }
                        } else {

                        }
                    }
                }
            } catch (e: HttpException) {
                Log.e("REQUEST", "Exception ${e.message}")
            } catch (t: Throwable) {
                wsResponseInterface.onFailedResponse(IotSDKUrls.DISCOVERY_SERVICE, 0, t.message)
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}