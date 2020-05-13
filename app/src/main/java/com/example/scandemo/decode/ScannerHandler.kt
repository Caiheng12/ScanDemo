package com.example.scandemo.decode

import android.os.Handler
import android.os.Message
import com.example.scandemo.Constant
import com.example.scandemo.ScannerActivity
import com.example.scandemo.camera.CameraManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result

import java.lang.ref.WeakReference

enum class State {
    PREVIEW,
    SUCCESS,
    DONE
}

class ScannerHandler(
    activity: ScannerActivity,
    decodeFormats: Collection<BarcodeFormat>?,
    characterSet: String,
    cameraManager: CameraManager
) : Handler() {

    private val decodeThread: DecodeThread
    private var state: State? = null
    private val cameraManager: CameraManager
    private val activityWeakReference: WeakReference<ScannerActivity> = WeakReference(activity)

    init {
        decodeThread = DecodeThread(activityWeakReference, decodeFormats!!, characterSet)
        decodeThread.start()
        state = State.SUCCESS
        this.cameraManager = cameraManager
        cameraManager.startPreview()
        restartPreviewAndDecode()
    }

    override fun handleMessage(message: Message?) {
        if (message == null) return
        when (message.what) {
            Constant.MESSAGE_SCANNER_DECODE_SUCCEEDED -> {
                val r = message.obj as Result
                if (activityWeakReference.get() != null) {
                    activityWeakReference.get()!!.handDecode(r)
                }
            }
            Constant.MESSAGE_SCANNER_DECODE_FAIL -> {
                state = State.PREVIEW
                cameraManager.requestPreviewFrame(decodeThread.getHandler(), Constant.MESSAGE_SCANNER_DECODE)
            }
        }
    }

    fun quitSynchronously() {
        state = State.DONE
        cameraManager.stopPreview()
        val quit = Message.obtain(decodeThread.getHandler(), Constant.MESSAGE_SCANNER_QUIT)
        quit.sendToTarget()
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L)
        } catch (e: InterruptedException) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(Constant.MESSAGE_SCANNER_DECODE_SUCCEEDED)
        removeMessages(Constant.MESSAGE_SCANNER_DECODE_FAIL)
    }

    private fun restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), Constant.MESSAGE_SCANNER_DECODE)
        }
    }
}