package ru.work.hy6.vkgrouploader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
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
import kotlinx.android.synthetic.main.*
import org.json.JSONObject
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates


val DEFAULT_GROUP = if (DEBUG) 98059938 else 18267412
val CHANGE_GROUP_REQUEST_CODE = 1
val CAPTURE_IMAGE_REQUEST_CODE = 2
val A_GROUP_ID = "vk_group"
val A_LAST_POST_ID = "last_post_id"
val A_GROUPS = "groups_set"


var activeDirectory: File by Delegates.notNull()

public class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = "MainActivity"

    private val STORAGE: File by lazy(LazyThreadSafetyMode.NONE) {
        val d = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "VKGroUploader")
        if (!d.exists())
            d.mkdir()
        d
    }

    private val STORAGE_SENDOUT: File by lazy(LazyThreadSafetyMode.NONE) {
        val d = File(STORAGE.absoluteFile, "Sendout")
        if (!d.exists())
            d.mkdir()
        d
    }
    private var group_id: Int by Delegates.notNull()
    private var message_text: String by Delegates.notNull()
    private var last_post_id: Int by Delegates.notNull()
    private var isGroupIdUpdated: Boolean = true

    private var pref: SharedPreferences by Delegates.notNull()

    private fun mylog(s: String) = {
        if (DEBUG) {
            Log.d(TAG, s)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        tvStorageListFiles.setOnClickListener(this)
        bSend.setOnClickListener(this)
        bPhoto.setOnClickListener(this)
        bDeleteMessage.setOnClickListener(this)
        bLogin.setOnClickListener(this)
        bGroupId.setOnClickListener(this)
        bDelete.setOnClickListener(this)
        bDelete.setOnLongClickListener {
            activeDirectory = STORAGE_SENDOUT
            actionListFiles()
            false
        }

        labelGroupName.isClickable = true
        labelGroupName.setOnClickListener(this)

        if (isJustStarted) actionPhoto()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        updateButtonLogin()
        updateListFiles()
        updateGroupName()
    }

    override fun onPause() {
        super.onPause()
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
            bGroupId -> {
                actionGroupId()
            }
        }
    }

    private fun updateListFiles() {
        labelListFiles.text = "${STORAGE.absolutePath}:"
        val sb = StringBuilder()
        for ((i, file) in getStorageFiles().withIndex()) {
            sb.append("${i + 1} \t${file.name} \t${Math.round(file.length() / 1024f)} Kb\n")
        }
        tvStorageListFiles.text = sb.toString()
    }

    private fun updateGroupName() {
        if (!isGroupIdUpdated) return

        val request = VKApi.groups().getById(VKParameters.from(VKApiConst.GROUP_ID, group_id))
        request.executeWithListener(object : VKRequest.VKRequestListener() {
            override fun onComplete(response: VKResponse?) {
                super.onComplete(response)
                if (response != null) {
                    if (DEBUG) Log.d(TAG, "request COMPLETE")
                    parseResponseGroupName(response)
                }
            }

            override fun onError(error: VKError?) {
                if (DEBUG) Log.d(TAG, "updateGroupName request ERROR")
            }
        })
        isGroupIdUpdated = false
    }

    private fun parseResponseGroupName(response: VKResponse) {
        val json = response.json.getJSONArray("response")
        val name = (json[0] as JSONObject).get("name")
        if (DEBUG) Log.d(TAG, "$json")
        if (DEBUG) Log.d(TAG, "name: $name")
        tvGroupName.text = "$name"
    }

    private fun updateButtonLogin() {
        if (!VKSdk.wakeUpSession(this)) {
            bLogin.setText(R.string.label_vk_login)
        } else {
            bLogin.setText(R.string.label_vk_logout)
        }
    }

    private fun loadSettings() {
        pref = getPreferences(Context.MODE_PRIVATE)
        group_id = pref.getInt(A_GROUP_ID, DEFAULT_GROUP)
        bGroupId.text = "${group_id}"
        message_text = getDefaultMessage()
        etMessage.setText(message_text)
        last_post_id = pref.getInt(A_LAST_POST_ID, 0)
    }

    private fun saveSettings() {
        pref = getPreferences(Context.MODE_PRIVATE)
        val ed = pref.edit()
        group_id = if ("${bGroupId.text}" == "") DEFAULT_GROUP else "${bGroupId.text}".toInt()
        ed.putInt(A_GROUP_ID, group_id)
        ed.putInt(A_LAST_POST_ID, last_post_id)
        ed.commit()
    }

    private fun getDefaultMessage(): String {
        val today = resources.getText(R.string.today)
        val about = resources.getText(R.string.about)
        val file = getStorageFiles().firstOrNull()
        if (file != null) {
            val date = Math.round((file.lastModified() / 600000).toDouble()) * 600000
            return SimpleDateFormat("$today dd MMMM $about HH.mm ").format(date)
        } else {
            return ""
        }
    }

    private fun actionLogin() {
        if (!VKSdk.wakeUpSession(this)) {
            mylog("touch Login")
            VKSdk.login(this, VKScope.GROUPS, VKScope.WALL, VKScope.PHOTOS)
        } else {
            mylog("touch Logout")
            VKSdk.logout()
        }
        updateButtonLogin()
    }

    private fun actionDeleteMessage() {
        mylog("touch Comment")
        etMessage.setText("")
    }

    private fun actionSend() {
        mylog("touch Send")
        saveSettings()

        if (isOnline() && VKSdk.isLoggedIn()) {
            if (getStorageFiles().size() == 0 && message_text == "" ) {
                toast(R.string.message_send_nothing)
            } else if (getStorageFiles().size() == 0 && message_text != "") {
                updateLastPost(last_post_id)
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

    private fun updateLastPost(id: Int) {
        //TODO this
        mylog("update last post ${id}")
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

    private fun sendPhotos() {
        val files = getStorageFiles()
        val requests = Array(files.size(), { it -> VKRequest("") })
        for ((i, file) in files.withIndex()) {
            val bitmap = getBitmap(file.absoluteFile)
            val request = VKApi.uploadWallPhotoRequest(VKUploadImage(bitmap, VKImageParameters.jpgImage(0.9f)), 0, group_id);
            requests[i] = request
        }
        val batch = VKBatchRequest(*requests)
        batch.executeWithListener(object : VKBatchRequest.VKBatchRequestListener() {
            override fun onComplete(responses: Array<out VKResponse>?) {
                if (responses == null) {
                    mylog("sendPhotos: responses is NULL")
                    return
                }
                val attachments = VKAttachments()
                for (response in responses) {
                    val photoModel = (response.parsedModel as VKPhotoArray).get(0)
                    attachments.add(photoModel)
                }
                makePost(attachments, message_text)
                mylog("sendPhotos: SUCCESS")
                cleanStorageDir()
                updateListFiles()
                updateMessage()
            }

            override fun onError(error: VKError?) {
                mylog("sendPhotos: error")
            }
        })

    }

    private fun updateMessage() {
        message_text = getDefaultMessage()
        etMessage.setText(message_text)
    }

    private fun makePost(attach: VKAttachments, msg: String) {
        val post = VKApi.wall().post(VKParameters.from(VKApiConst.OWNER_ID, "-$group_id", VKApiConst.ATTACHMENTS, attach, VKApiConst.MESSAGE, msg));
        post.setModelClass(VKWallPostResult::class.java)
        post.executeWithListener(object : VKRequest.VKRequestListener() {
            override fun onComplete(response: VKResponse?) {
                mylog("Post SUCCESSFULLY send")
                toast(R.string.message_success_sendout)
            }

            override fun onError(error: VKError?) {
                mylog("makePost ERROR")
            }
        })
    }

    private fun cleanStorageDir() {
        val files = getStorageFiles()
        for (file in files) {
            file.renameTo(File(STORAGE_SENDOUT.absoluteFile, file.name))
        }
    }

    private fun actionListFiles() {
        mylog("touch List Files")
        val intent = Intent(this, ListFilesActivity::class.java)
        startActivity(intent)
    }

    fun getBitmap(file: File): Bitmap = BitmapFactory.decodeFile(file.absolutePath)

    fun getStorageFiles(): Array<out File> = STORAGE.listFiles(FileFilter { !it.isDirectory })

    private fun getNewFileForImage(): File {
        val fileName = "VKGU_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}.jpg"
        return File(STORAGE.absolutePath, fileName)
    }

    private fun toast(res: Int) {
        Toast.makeText(this, res, Toast.LENGTH_LONG).show()
        mylog("${resources.getText(res)}")
    }

    private fun actionGroupId() {
        mylog("touch actionGroupName")
        val intent = Intent(this, GroupIdActivity::class.java)
        startActivityForResult(intent, CHANGE_GROUP_REQUEST_CODE)
    }

    private fun actionPhoto() {
        mylog("creating photo")
        val file = getNewFileForImage()
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file))
        startActivityForResult(intent, CAPTURE_IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mylog("onActivityResult")
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CHANGE_GROUP_REQUEST_CODE && data != null) {
                val group = data.getStringExtra(A_GROUP_ID)
                bGroupId.text = group
                saveSettings()
                isGroupIdUpdated = true
                updateGroupName()

            }
            if (requestCode == CAPTURE_IMAGE_REQUEST_CODE) {
                actionPhoto()
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {

        }
    }


}