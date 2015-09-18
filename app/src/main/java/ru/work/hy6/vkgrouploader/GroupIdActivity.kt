package ru.work.hy6.vkgrouploader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.a_group_name.bOK
import kotlinx.android.synthetic.a_group_name.etGroup
import kotlinx.android.synthetic.a_group_name.lvGroupId
import java.util.*
import kotlin.properties.Delegates

class GroupIdActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    private var pref: SharedPreferences by Delegates.notNull()
    private var groups: ArrayList<String> by Delegates.notNull()
    private var sAdapter: ArrayAdapter<String> by Delegates.notNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_group_name)
        loadIds()

        lvGroupId.onItemClickListener = this
        sAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, groups)
        lvGroupId.adapter = sAdapter


        bOK.setOnClickListener {
            val id = "${etGroup.text}"
            groups.add(id)
            saveIds()
            val intent = Intent()
            intent.putExtra(A_GROUP_ID, id)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d("onItemClick", "${groups[position]}")
        etGroup.setText(groups[position])
    }

    private fun loadIds() {
        pref = getPreferences(Context.MODE_PRIVATE)
        groups = pref.getStringSet(A_GROUPS, hashSetOf("$DEFAULT_GROUP")).toArrayList()
    }

    private fun saveIds() {
        pref = getPreferences(Context.MODE_PRIVATE)
        val ed = pref.edit()
        ed.putStringSet(A_GROUPS, groups.toSet())
        ed.commit()
    }
}
