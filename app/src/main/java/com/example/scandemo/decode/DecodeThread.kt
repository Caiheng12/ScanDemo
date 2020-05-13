package com.example.scandemo.decode

import android.os.Handler
import android.os.Looper
import com.example.scandemo.ScannerActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import java.lang.ref.WeakReference
import java.util.EnumMap
import java.util.concurrent.CountDownLatch

class DecodeThread(
    activityWeakReference: WeakReference<ScannerActivity>,
    decodeFormats: Collection<BarcodeFormat>,
    characterSet: String
) : Thread() {

    private val activityWeakReference: WeakReference<ScannerActivity> = activityWeakReference
    private val hints: MutableMap<DecodeHintType, Any>
    private var handler: Handler? = null
    private val handlerInitLatch: CountDownLatch = CountDownLatch(1)

    init {
        hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
        hints[DecodeHintType.CHARACTER_SET] = characterSet
        hints[DecodeHintType.TRY_HARDER] = true
    }

    fun getHandler(): Handler {
        try {
            handlerInitLatch.await()
        } catch (ie: InterruptedException) {
        }
        return handler!!
    }

    override fun run() {
        Looper.prepare()
        handler = DecodeHandler(activityWeakReference, hints)
        handlerInitLatch.countDown()
        Looper.loop()
    }
}