package ru.work.hy6.simplecamerawork

import android.app.Activity
import android.database.DataSetObserver
import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.a_list_files.*
import kotlinx.android.synthetic.a_list_files.lvListFiles
import kotlinx.android.synthetic.a_list_files.tvListFilesTitle
import java.io.File
import java.io.FileFilter
import kotlin.properties.Delegates

public class ListFilesActivity() : Activity(), View.OnClickListener {

    private val DIRECTORY = activeDirectory
    private val FILES: Array<out File> by Delegates.lazy {
        DIRECTORY.listFiles(FileFilter { !it.isDirectory() })
    }
    private val NAMES: Array<String> by Delegates.lazy {
        Array(FILES.size(), { i -> "${FILES[i].name} \t ${FILES[i].length() / 1024} Kb" })
    }

    private var isItemСheck = false


    // TODO: CustomAdapter<Array<Bitmap>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super<Activity>.onCreate(savedInstanceState)
        setContentView(R.layout.a_list_files)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, NAMES)

        adapter.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
            }
        })

        lvListFiles.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE)
        lvListFiles.setAdapter(adapter)
        tvListFilesTitle.setText(DIRECTORY.getAbsolutePath())

        bDelete.setOnClickListener(this)
        bInvert.setOnClickListener(this)

        if (FILES.size() > 0) bInvert.setVisibility(View.VISIBLE)
        lvListFiles.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                updateButtonDelete()
            }
        })
        updateButtonDelete()
    }

    private fun updateButtonDelete() {
        isItemСheck = lvListFiles.getCheckedItemCount() > 0
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
            bInvert ->{
                actionInvert()
            }
        }
    }

    private fun actionInvert() {
        for (i in 0..FILES.size()-1) {
            val check = lvListFiles.isItemChecked(i)
            lvListFiles.setItemChecked(i, !check)
        }
        updateButtonDelete()
    }

    private fun actionDelete() {
        val sba = lvListFiles.getCheckedItemPositions()
        if (isItemСheck) {
            for (i in 0..FILES.size() - 1) {
                val key = sba.keyAt(i)
                if (sba.get(key))
                    MainActivity().log("${FILES[key].name}, delete: ${FILES[key].delete()}")

            }
        }
        finish()
    }

}
