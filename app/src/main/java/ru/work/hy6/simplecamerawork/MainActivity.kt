package ru.work.hy6.simplecamerawork

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import com.vk.sdk.VKScope
import com.vk.sdk.VKSdk
import com.vk.sdk.api.*
import com.vk.sdk.api.model.VKAttachments
import com.vk.sdk.api.model.VKPhotoArray
import com.vk.sdk.api.model.VKWallPostResult
import com.vk.sdk.api.photo.VKImageParameters
import com.vk.sdk.api.photo.VKUploadImage
import kotlinx.android.synthetic.a_group_name.bOK
import kotlinx.android.synthetic.a_group_name.etGroup
import kotlinx.android.synthetic.main.*
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.properties.Delegates

val DEBUG = true

val DEFAULT_GROUP = if (DEBUG) 98059938 else 18267412
val DEFAULT_MESSAGE = ""
val CHANGE_GROUP_REQUEST_CODE = 1
val A_GROUP_ID = "vk_group"
val A_MESSAGE = "message"

val STORAGE: File by Delegates.lazy {
    val d = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "VKGroUploader")
    if (!d.exists())
        d.mkdir()
    d
}

val STORAGE_SENDOUT: File by Delegates.lazy {
    val d = File(STORAGE.getAbsoluteFile(), "Sendout")
    if (!d.exists())
        d.mkdir()
    d
}
var activeDirectory: File by Delegates.notNull()

fun log(s: String) = {
    if (DEBUG) Log.d("simpleLog", s)
}

public class MainActivity : Activity(), View.OnClickListener {
    private var group_id: Int by Delegates.notNull()
    private var message_text: String by Delegates.notNull()

    private var sPref: SharedPreferences by Delegates.notNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super<Activity>.onCreate(savedInstanceState)
        VKSdk.initialize(this)
        setContentView(R.layout.main)

        tvStorageListFiles.setOnClickListener(this)
        bSend.setOnClickListener(this)
        bPhoto.setOnClickListener(this)
        bDeleteMessage.setOnClickListener(this)
        bLogin.setOnClickListener(this)
        bGroup.setOnClickListener(this)
        bDelete.setOnClickListener(this)
        bDelete.setOnLongClickListener {
            activeDirectory = STORAGE_SENDOUT
            actionListFiles()
            false
        }

        labelGroupName.setClickable(true)
        labelGroupName.setOnClickListener(this)

        if (!DEBUG) actionPhoto()
    }

    override fun onResume() {
        super<Activity>.onResume()
        loadSettings()
        updateLabelLoginButton()
        updateListFiles()
        updateGroupName()
    }

    override fun onPause() {
        super<Activity>.onPause()
        saveSettings()
    }

    override fun onClick(v: View) {
        when (v) {
            tvStorageListFiles, bDelete -> {
                activeDirectory = STORAGE
                actionListFiles()
            }
            bDeleteMessage -> {
                actionDeleteMessage()
            }
            bLogin -> {
                actionLogin()
            }
            bPhoto -> {
                actionPhoto()
            }
            bSend -> {
                actionSend()
            }
            bGroup -> {
                actionGroup()
            }
        }
    }

    private fun updateListFiles() {
        labelListFiles.setText("${STORAGE.getAbsolutePath()}:")
        val sb = StringBuilder()
        for ((i, file) in getStorageFiles().withIndex()) {
            sb.append("${i + 1} \t${file.name} \t${Math.round(file.length() / 1024f)} Kb\n")
        }
        tvStorageListFiles.setText(sb.toString())
    }

    private fun updateLabelLoginButton() {
        if (!VKSdk.wakeUpSession(this)) {
            bLogin.setText(R.string.label_vk_login)
        } else {
            bLogin.setText(R.string.label_vk_logout)
        }
    }

    private fun loadSettings() {
        sPref = getPreferences(Context.MODE_PRIVATE)
        group_id = sPref.getInt(A_GROUP_ID, DEFAULT_GROUP)
        bGroup.setText("${group_id}")
        message_text = sPref.getString(A_MESSAGE, DEFAULT_MESSAGE)
        etMessage.setText(message_text)
    }

    private fun saveSettings() {
        sPref = getPreferences(Context.MODE_PRIVATE)
        val ed = sPref.edit()
        group_id = if ("${bGroup.getText()}" == "") DEFAULT_GROUP else "${bGroup.getText()}".toInt()
        message_text = if ("${etMessage.getText()}" == "") DEFAULT_MESSAGE else "${etMessage.getText()}"
        ed.putInt(A_GROUP_ID, group_id)
        ed.putString(A_MESSAGE, message_text)
        ed.commit()
    }

    private fun actionLogin() {
        if (!VKSdk.wakeUpSession(this)) {
            log("touch Login")
            VKSdk.login(this, VKScope.GROUPS, VKScope.WALL, VKScope.PHOTOS)
        } else {
            log("touch Logout")
            VKSdk.logout()
        }
        updateLabelLoginButton()
    }

    private fun actionDeleteMessage() {
        log("touch Comment")
        etMessage.setText("")
    }

    private fun actionSend() {
        log("touch Send")
        saveSettings()

        if (isOnline() && VKSdk.isLoggedIn()) {
            if (getStorageFiles().size() == 0 && message_text == "" ) {
                toast(R.string.message_send_nothing)
            } else {
                toast(R.string.message_run_send)
                sendPhotos()
            }
        } else if (!VKSdk.isLoggedIn()) {
            toast(R.string.message_logoff)
        } else if (!isOnline()) {
            toast(R.string.message_internet_off)
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.getActiveNetworkInfo()
        return netInfo != null && netInfo.isConnectedOrConnecting()
    }

    private fun sendPhotos() {
        val files = getStorageFiles()
        val requests = Array(files.size(), { it -> VKRequest("") })
        for ((i, file) in files.withIndex()) {
            val bitmap = getBitmap(file.getAbsoluteFile())
            val request = VKApi.uploadWallPhotoRequest(VKUploadImage(bitmap, VKImageParameters.jpgImage(0.9f)), 0, group_id);
            requests[i] = request
        }
        val batch = VKBatchRequest(*requests)
        batch.executeWithListener(object : VKBatchRequest.VKBatchRequestListener() {
            override fun onComplete(responses: Array<out VKResponse>?) {
                if (responses == null) {
                    log("sendPhotos: responses is NULL")
                    return
                }
                val attachments = VKAttachments()
                for (response in responses) {
                    val photoModel = (response.parsedModel as VKPhotoArray).get(0)
                    attachments.add(photoModel)
                }
                makePost(attachments, message_text)
                log("sendPhotos: SUCCESS")
                cleanStorageDir()
                updateListFiles()
            }

            override fun onError(error: VKError?) {
                log("sendPhotos: error")
            }
        })

    }

    private fun cleanStorageDir() {
        val files = getStorageFiles()
        for (file in files) {
            file.renameTo(File(STORAGE_SENDOUT.getAbsoluteFile(), file.name))
        }

    }

    private fun actionListFiles() {
        log("touch List Files")
        val intent = Intent(this, javaClass<ListFilesActivity>())
        startActivity(intent)
    }

    private fun actionPhoto() {
        log("creating photo")
        val file = getNewFileForImage()
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file))
        startActivity(intent)
    }

    private fun makePost(attach: VKAttachments, msg: String) {
        val post = VKApi.wall().post(VKParameters.from(VKApiConst.OWNER_ID, "-${group_id}", VKApiConst.ATTACHMENTS, attach, VKApiConst.MESSAGE, msg));
        post.setModelClass(javaClass<VKWallPostResult>())
        post.executeWithListener(object : VKRequest.VKRequestListener() {
            override fun onComplete(response: VKResponse?) {
                log("Post SUCCESSFULLY send")
                toast(R.string.message_success_sendout)
            }

            override fun onError(error: VKError?) {
                log("makePost ERROR")
            }
        })
    }

    fun getBitmap(file: File): Bitmap = BitmapFactory.decodeFile(file.getAbsolutePath())

    fun getStorageFiles(): Array<out File> = STORAGE.listFiles(FileFilter { !it.isDirectory() })

    private fun getNewFileForImage(): File {
        val fileName = "SCW_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}.jpg"
        return File(STORAGE.getAbsolutePath(), fileName)
    }

    private fun toast(res: Int) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
        log("${getResources().getText(res)}")
    }

    private fun actionGroup() {
        log("touch actionGroup")
        val intent = Intent(this, javaClass<GroupIdActivity>())
        startActivityForResult(intent, CHANGE_GROUP_REQUEST_CODE)
    }

    private fun updateGroupName() {
        log("update group_name clicked")
        val request = VKApi.groups().getById(VKParameters.from(VKApiConst.GROUP_ID, group_id))
        request.executeWithListener(object : VKRequest.VKRequestListener() {
            override fun onComplete(response: VKResponse?) {
                super.onComplete(response)
                log("group_name request COMPLETE")
                parseResponse(response)
            }

            override fun onError(error: VKError?) {
                log("group_name request ERROR")
            }
        })
    }

    private fun parseResponse(response: VKResponse?) {
        if (response == null) return
        log("--- parse response ---")

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super<Activity>.onActivityResult(requestCode, resultCode, data)
        log("onActivityResult")
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CHANGE_GROUP_REQUEST_CODE && data != null) {
                val group = data.getStringExtra(A_GROUP_ID)
                bGroup.setText(group)
                saveSettings()
            }
        }
    }

}

class GroupIdActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_group_name)
        bOK.setOnClickListener {
            val intent = Intent()
            intent.putExtra(A_GROUP_ID, "${etGroup.getText()}")
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}