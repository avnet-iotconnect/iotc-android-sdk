package com.softweb.iotconnectsdk.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.softweb.iotconnectsdk.R
import com.softweb.iotconnectsdk.model.AttributesModel
import kotlinx.android.synthetic.main.activity_gateway_child_devices.spTags
import java.util.ArrayList

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
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }
}