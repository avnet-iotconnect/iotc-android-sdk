package com.softweb.iotconnectsdk.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.iotconnectsdk.interfaces.DeviceCallback
import com.softweb.iotconnectsdk.R
import com.softweb.iotconnectsdk.activity.FirmwareActivity.sdkClient
import kotlinx.android.synthetic.main.activity_gateway_child_devices.btnCreateDevice
import kotlinx.android.synthetic.main.activity_gateway_child_devices.btnDeleteDevice
import kotlinx.android.synthetic.main.activity_gateway_child_devices.etDisplayName
import kotlinx.android.synthetic.main.activity_gateway_child_devices.etUniqueId
import kotlinx.android.synthetic.main.activity_gateway_child_devices.spTags
import kotlinx.android.synthetic.main.activity_gateway_child_devices.toolbar
import org.json.JSONObject

class GatewayChildDevicesActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener,
    DeviceCallback {

    private var tagsList: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gateway_child_devices)

        tagsList = intent.extras?.getStringArrayList("tagsList")

        // calling the action bar

        // showing the back button in action bar
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, tagsList!!
        )
        spTags.adapter = adapter
        spTags.onItemSelectedListener = this

        btnCreateDevice.setOnClickListener {
            if (checkValidation()) {
                val innerObject = JSONObject()
                innerObject.put("dn", etDisplayName.text.toString())
                innerObject.put("id", etUniqueId.text.toString())
                innerObject.put("tg", spTags.selectedItem.toString())
                sdkClient.createChild(innerObject)
            }
        }

        btnDeleteDevice.setOnClickListener {
            if (etUniqueId.text.toString().isEmpty()) {
                Toast.makeText(
                    this@GatewayChildDevicesActivity,
                    getString(R.string.alert_enter_unique_id),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val innerObject = JSONObject()
                innerObject.put("id", etUniqueId.text.toString())
                sdkClient.deleteChild(innerObject)
            }

        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onReceiveMsg(message: String?) {
        if (!message.isNullOrBlank()) {
            Toast.makeText(this@GatewayChildDevicesActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    /*
     * ## Function to check prerequisite configuration to run this sample code
     * cpId               : It need to get from the IoTConnect platform "Settings->Key Vault".
     * uniqueId           : Its device ID which register on IotConnect platform and also its status has Active and Acquired
     */
    private fun checkValidation(): Boolean {
        if (etUniqueId.getText().toString().isEmpty()) {
            Toast.makeText(
                this@GatewayChildDevicesActivity,
                getString(R.string.alert_enter_unique_id),
                Toast.LENGTH_SHORT
            ).show()
            return false
        } else if (etDisplayName.text.toString().isEmpty()) {
            Toast.makeText(
                this@GatewayChildDevicesActivity,
                getString(R.string.alert_enter_display_name),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    // this event will enable the back
    // function to the button on press
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}