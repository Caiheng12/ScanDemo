package com.example.scandemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.Toast
import com.example.scandemo.camera.CameraManager
import com.example.scandemo.decode.ScannerHandler
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_scanner.*

import java.io.IOException
import java.util.EnumSet

/**
 * Created by heng.cai01
 * Date: 2020-05-05
 */
class ScannerActivity : Activity(), SurfaceHolder.Callback {

    private var isSurfaceCreated: Boolean = false
    private var handler: ScannerHandler? = null
    private var decodeFormats: Collection<BarcodeFormat>? = null
    private var cameraManager: CameraManager? = null
    private var beepManager: BeepManager? = null

    companion object {
        const val TAG = "ScannerActivity"
        fun launch(context: Context) {
            context.startActivity(Intent(context, ScannerActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_scanner)
        initData()
    }

    private fun initData() {
        isSurfaceCreated = false
        decodeFormats = EnumSet.of(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128)
        beepManager = BeepManager(this)
        rl_scan_cancel.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        cameraManager = CameraManager(this)

        if (isSurfaceCreated) initCamera(surface_scan.holder)
        else surface_scan.holder.addCallback(this)

        beepManager?.updatePrefs()
        scan_view?.onResume()
    }

    override fun onPause() {
        super.onPause()
        handler?.quitSynchronously()
        handler = null
        cameraManager?.closeDriver()
        beepManager?.close()
        scan_view?.onCancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.clearFramingRect()
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        isSurfaceCreated = false
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        if (!isSurfaceCreated) {
            isSurfaceCreated = true
            initCamera(p0)
        }
    }

    private fun initCamera(surfaceHolder: SurfaceHolder?) {
        checkNotNull(surfaceHolder) { "No SurfaceHolder provided" }
        if (cameraManager == null || cameraManager!!.isOpen) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?")
            return
        }
        try {
            cameraManager?.openDriver(surfaceHolder)
            if (handler == null) {
                handler = ScannerHandler(this, decodeFormats, "utf-8", cameraManager!!)
            }
        } catch (ioe: IOException) {
            Log.w(TAG, ioe)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Unexpected error initializing camera", e)
        }
    }

    fun getCameraManager(): CameraManager? {
        return cameraManager
    }

    fun getHandler(): Handler? {
        return handler
    }

    //在这里处理扫码结果
    fun handDecode(result: Result) {
        beepManager?.playBeepSoundAndVibrate()
        Toast.makeText(this, getString(R.string.can_not_resolve_result) + result.text, Toast.LENGTH_SHORT).show()
        finish()
    }
}