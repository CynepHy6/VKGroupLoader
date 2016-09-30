package ru.work.hy6.vkgrouploader

import android.database.DataSetObserver
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.a_list_files.*
import java.io.File
import java.io.FileFilter

class ListFilesActivity() : AppCompatActivity(), View.OnClickListener {
    private val TAG = "ListFilesActivity"
    private val DIRECTORY = activeDirectory
    private val FILES: Array<out File> by lazy(LazyThreadSafetyMode.NONE) {
        DIRECTORY.listFiles(FileFilter { !it.isDirectory })
    }
    private val NAMES: Array<String> by lazy(LazyThreadSafetyMode.NONE) {
        Array(FILES.size, { i -> "${FILES[i].name} \t ${FILES[i].length() / 1024} Kb" })
    }

    private var isItemСheck = false


    private fun log(s: String) = {
        if (DEBUG) {
            Log.d(TAG, s)
        }
    }
    // TODO: CustomAdapter<Array<Bitmap>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_list_files)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, NAMES)

        adapter.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
            }
        })

        lvListFiles.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
        lvListFiles.adapter = adapter
        tvListFilesTitle.text = DIRECTORY.getAbsolutePath()

        bDelete.setOnClickListener(this)
        bInvert.setOnClickListener(this)

        if (FILES.size > 0) bInvert.visibility = View.VISIBLE
        lvListFiles.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                updateButtonDelete()
            }
        }
        updateButtonDelete()
    }

    private fun updateButtonDelete() {
        isItemСheck = lvListFiles.checkedItemCount > 0
        if (isItemСheck) {
            bDelete.setText(R.string.label_delete_button)
        } else {
            bDelete.setText(R.string.label_back_button)
        }
    }


    override fun onClick(v: View) {
        when (v) {
            bDelete -> {
                actionDelete()
            }
            bInvert -> {
                actionInvert()
            }
        }
    }

    private fun actionInvert() {
        for (i in 0..FILES.size - 1) {
            val check = lvListFiles.isItemChecked(i)
            lvListFiles.setItemChecked(i, !check)
        }
        updateButtonDelete()
    }

    private fun actionDelete() {
        val sba = lvListFiles.checkedItemPositions
        if (isItemСheck) {
            for (i in 0..FILES.size - 1) {
                val key = sba.keyAt(i)
                if (sba.get(key))
                    log("${FILES[key].name}, delete: ${FILES[key].delete()}")

            }
        }
        finish()
    }

}
