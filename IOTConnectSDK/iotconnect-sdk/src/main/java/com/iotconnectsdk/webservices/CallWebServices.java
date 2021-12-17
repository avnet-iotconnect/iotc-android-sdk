package com.iotconnectsdk.webservices;

import com.iotconnectsdk.utils.IotSDKUrls;
import com.iotconnectsdk.utils.IotSDKWsUtils;
import com.iotconnectsdk.webservices.interfaces.WsResponseInterface;
import com.iotconnectsdk.webservices.requestbean.SyncServiceRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallWebServices {

    public void sync(String url, final SyncServiceRequest syncServiceRequest, final WsResponseInterface wsResponseInterface) {

        Call<ResponseBody> call = IotSDKWsUtils.getAPIService(url).sync(url, syncServiceRequest);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    wsResponseInterface.onSuccessResponse(IotSDKUrls.SYNC_SERVICE, response.body().string());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                try {
                    wsResponseInterface.onFailedResponse(IotSDKUrls.SYNC_SERVICE, 0, t.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getDiscoveryApi(String discoveryUrl, final WsResponseInterface wsResponseInterface) {

        Call<ResponseBody> call = IotSDKWsUtils.getAPIService(discoveryUrl).getDiscoveryApi(discoveryUrl);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    wsResponseInterface.onSuccessResponse(IotSDKUrls.DISCOVERY_SERVICE, response.body().string());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                try {
                    wsResponseInterface.onFailedResponse(IotSDKUrls.DISCOVERY_SERVICE, 0, t.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

   /* private void showDialog(Activity com.softweb.iotconnectsdk.activity) {
        try {
            progressDialog = new ProgressDialog(com.softweb.iotconnectsdk.activity);
            if (com.softweb.iotconnectsdk.activity != null && !com.softweb.iotconnectsdk.activity.isFinishing()) {
                progressDialog.setMessage(IotSDKConstant.PLEASE_WAIT);
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideDialog(Activity com.softweb.iotconnectsdk.activity) {
        if (com.softweb.iotconnectsdk.activity != null && !com.softweb.iotconnectsdk.activity.isFinishing() && progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }*/

}
