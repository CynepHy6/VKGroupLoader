package ru.work.hy6.vkgrouploader

import android.graphics.Matrix
import android.graphics.RectF
import android.hardware.Camera
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.Toast
import kotlinx.android.synthetic.main.a_cam.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

public class CamActivity : AppCompatActivity(), View.OnClickListener {
    val DIR2SAVE = STORAGE
    var count = 0
    var safeToCapture = false

    var mCamera: Camera? = null

    val previewHolder: SurfaceHolder by lazy { svSurface.holder }
    val surfaceCallback: HolderCallback by lazy { HolderCallback() }
    val CAMERA_ID = 0

    val FULL_SCREEN = true
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_cam)
        previewHolder.addCallback(surfaceCallback)
        svSurface.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            svSurface -> {
                if (safeToCapture) {
                    captureImage()
                }
            }
        }
    }

    private fun captureImage() {
        if (mCamera == null) return
        safeToCapture = false
        mCamera?.takePicture(null, null, object : Camera.PictureCallback {
            override fun onPictureTaken(data: ByteArray?, camera: Camera?) {
                SaveInBackground().execute(data)
                camera?.startPreview()
            }

        })
        count++
        val msg = resources.getString(R.string.capture_image) + " $count"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    }

    inner class SaveInBackground : AsyncTask<ByteArray, String, String>() {
        override fun doInBackground(vararg params: ByteArray?): String? {
            val photoFile = File(DIR2SAVE, getPhotoName())
            try {
                val fos = FileOutputStream(photoFile)
                fos.write(params[0])
                fos.close()
            } catch(e: Exception) {
                e.printStackTrace()
            }
            safeToCapture = true
            return null
        }
    }

    private fun getPhotoName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        return "${sdf.format(Date())}.jpg"
    }

    override fun onResume() {
        super.onResume()
        if (mCamera == null) {
            mCamera = Camera.open();
//            mCamera = Camera.open(CAMERA_ID)
        }
        setPreviewSize(FULL_SCREEN)
    }

    override fun onPause() {
        super.onPause()
        if (mCamera != null) {
            mCamera?.release()
            mCamera = null
        }
    }

    inner class HolderCallback : SurfaceHolder.Callback {

        override fun surfaceCreated(holder: SurfaceHolder?) {
            try {
                mCamera?.setPreviewDisplay(holder)
                mCamera?.startPreview()
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            //            val params = mCamera?.parameters
            //            params?.setPreviewSize()
            mCamera?.stopPreview()
            setCameraDisplayOrientation(CAMERA_ID)
            try {
                mCamera?.setPreviewDisplay(holder)
                mCamera?.startPreview()
            } catch(e: Exception) {
                e.printStackTrace() //Nhfvgfvgfv88 Nhfvgfvgfv88
            }
            safeToCapture = true
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            // nothing
        }

    }

    private fun setPreviewSize(fullScreen: Boolean) {
        //        размеры экрана
        val display = windowManager.defaultDisplay
        val widthIsMax = display.width > display.height
        //        размеры превью камеры
        val size = mCamera?.parameters?.previewSize
        val rectDisplay = RectF()
        val rectPreview = RectF()
        //        recfF экрана соответствует размеру экрана
        rectDisplay.set(0f, 0f, display.width.toFloat(), display.height.toFloat())
        if (widthIsMax) {
            rectPreview.set(0f, 0f, size?.width!!.toFloat(), size?.height!!.toFloat())
        } else {
            rectPreview.set(0f, 0f, size?.height!!.toFloat(), size?.width!!.toFloat())
        }
        val matrix = Matrix()
        if (!fullScreen) {
            matrix.setRectToRect(rectPreview, rectDisplay, Matrix.ScaleToFit.START)
        } else {
            matrix.setRectToRect(rectDisplay, rectPreview, Matrix.ScaleToFit.START)
            matrix.invert(matrix)
        }
        matrix.mapRect(rectPreview)
        svSurface.layoutParams.height = rectPreview.bottom.toInt()
        svSurface.layoutParams.width = rectPreview.right.toInt()
    }

    private fun setCameraDisplayOrientation(cameraId: Int) {
        //        определяем насколько повернут экран от нормального положения
        val rotation = windowManager.defaultDisplay.rotation
        var degres = 0
        when (rotation) {
            Surface.ROTATION_0 -> {
                degres = 0
            }
            Surface.ROTATION_90 -> {
                degres = 90
            }
            Surface.ROTATION_180 -> {
                degres = 180
            }
            Surface.ROTATION_270 -> {
                degres = 270
            }
        }
        var result = 0
        //        получаем инфо по камере
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = 360 - degres + info.orientation
        } else {
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = 360 - degres - info.orientation
                result += 360
            }
        }
        result %= 360
        mCamera?.setDisplayOrientation(result)
    }

}
