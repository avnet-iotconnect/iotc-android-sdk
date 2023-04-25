package com.softweb.iotconnectsdk.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.softweb.iotconnectsdk.R
import com.softweb.iotconnectsdk.activity.FirmwareActivity.sdkClient
import kotlinx.android.synthetic.main.activity_gateway_child_devices.btnCreateDevice
import kotlinx.android.synthetic.main.activity_gateway_child_devices.etDisplayName
import kotlinx.android.synthetic.main.activity_gateway_child_devices.etUniqueId
import kotlinx.android.synthetic.main.activity_gateway_child_devices.spTags
import org.json.JSONObject

class GatewayChildDevicesActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var tagsList: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gateway_child_devices)

        tagsList = intent.extras?.getStringArrayList("tagsList")



        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, tagsList!!
        )
        spTags.adapter = adapter
        spTags.onItemSelectedListener = this

        btnCreateDevice.setOnClickListener {
            val innerObject= JSONObject()
            innerObject.put("dn",etDisplayName.text.toString())
            innerObject.put("id",etUniqueId.text.toString())
            innerObject.put("tg",spTags.selectedItem.toString())

            sdkClient.createChild(innerObject)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }
}