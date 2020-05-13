package com.example.scandemo.decode

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.example.scandemo.Constant
import com.example.scandemo.ScannerActivity
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.Result
import java.lang.ref.WeakReference
import java.util.Arrays

class DecodeHandler(activityWeakReference: WeakReference<ScannerActivity>, hints: MutableMap<DecodeHintType, Any>) : Handler() {

    private val TAG = DecodeHandler::class.java.simpleName

    private val activityWeakReference: WeakReference<ScannerActivity>
    private val multiFormatReader: MultiFormatReader = MultiFormatReader()
    private var running = true
    private var mRotatedData: ByteArray? = null

    init {
        multiFormatReader.setHints(hints)
        this.activityWeakReference = activityWeakReference
    }

    override fun handleMessage(message: Message?) {
        if (message == null || !running) {
            return
        }
        if (message.what == Constant.MESSAGE_SCANNER_DECODE) {
            decode(message.obj as ByteArray, message.arg1, message.arg2)
        } else if (message.what == Constant.MESSAGE_SCANNER_QUIT) {
            running = false
            Looper.myLooper()!!.quit()
        }
    }

    private fun decode(data: ByteArray, width: Int, height: Int) {
        var width = width
        var height = height
        //这里需要对扫码的数据进行宽高的调换，原来扫码的是横屏数据，需要转化成竖屏。
        if (null == mRotatedData) {
            mRotatedData = ByteArray(width * height)
        } else {
            if (mRotatedData!!.size < width * height) {
                mRotatedData = ByteArray(width * height)
            }
        }
        Arrays.fill(mRotatedData, 0.toByte())
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x + y * width >= data.size) {
                    break
                }
                mRotatedData!![x * height + height - y - 1] = data[x + y * width]
            }
        }
        val tmp = width
        width = height
        height = tmp
        val start = System.currentTimeMillis()
        var rawResult: Result? = null
        if (activityWeakReference.get() == null) {
            return
        }
        val handler = activityWeakReference.get()!!.getHandler()
        val source = activityWeakReference.get()!!.getCameraManager()!!.buildLuminanceSource(mRotatedData, width, height)
        if (source != null) {
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap)
            } catch (re: ReaderException) {
                // continue
                Log.e(TAG, "decode: re:" + re.message)
            } finally {
                multiFormatReader.reset()
            }
        }

        if (rawResult != null) {
            // Don't log the barcode contents for security.
            val end = System.currentTimeMillis()
            Log.d(TAG, "Found barcode in " + (end - start) + " ms")
            if (handler != null) {
                val message = Message.obtain(handler, Constant.MESSAGE_SCANNER_DECODE_SUCCEEDED, rawResult)
                val bundle = Bundle()
                message.data = bundle
                message.sendToTarget()
            }
        } else {
            if (handler != null) {
                val message = Message.obtain(handler, Constant.MESSAGE_SCANNER_DECODE_FAIL)
                message.sendToTarget()
            }
        }
    }
}