package com.example.scandemo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Canvas

import android.view.animation.Animation
import android.view.animation.LinearInterpolator

/**
 * Created by heng.cai01
 * Date: 2020-05-06
 */
class ScannerView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    // 一次动画时间
    private val ANIMATOR_DURATION: Long = 2000L
    // 系统一秒刷新60次 保证值动画更新和系统刷新一致
    private val ANIMATOR_MAX_VALUE: Int = ANIMATOR_DURATION.toInt() / 1000 * 60

    private var scanHeight: Float = 0f
    private var scanVerticalStart: Float = 0f

    private var tipText: String = "放入卡片二维码，可自动扫描"
    private var tipMarginTop: Float = 0f

    private var mTipPaint: Paint? = null
    private var animatedValue: Int = 0
    private var mBitmap: Bitmap? = null
    private var bitmapHeight: Int = 0
    private var bitmapWidth: Int = 0

    private var widthPixels: Int = resources.displayMetrics.widthPixels
    private var heightPixels: Int = resources.displayMetrics.heightPixels
    private var startLeft: Float = 0f
    private var times: Float = 5F

    private var valueAnimator: ValueAnimator? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScannerView)
        scanHeight = typedArray.getDimension(
            R.styleable.ScannerView_scan_height,
            DensityUtil.dip2px(context, 280F).toFloat()
        )
        tipText = typedArray.getString(R.styleable.ScannerView_tip_text) ?: tipText

        mTipPaint = Paint()
        mTipPaint?.isAntiAlias = true
        tipMarginTop = typedArray.getDimension(
            R.styleable.ScannerView_tip_margin_top,
            DensityUtil.dip2px(context, 44F).toFloat()
        )
        mTipPaint?.textSize = typedArray.getDimension(
            R.styleable.ScannerView_tip_text_size,
            DensityUtil.sp2px(context, 14F).toFloat()
        )
        mTipPaint?.color =
            typedArray.getColor(R.styleable.ScannerView_tip_text_color, Color.parseColor("#FFFFFF"))
        typedArray.recycle()

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        mBitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.scan_indicator, options)

        bitmapWidth = mBitmap?.width ?: 0
        bitmapHeight = mBitmap?.height ?: 0
        startLeft = (widthPixels - bitmapWidth) / 2f

        post { times = scanHeight / ANIMATOR_MAX_VALUE }

        scanVerticalStart = (heightPixels - scanHeight) / 4
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mBitmap != null) {
            // 绘制扫描仪
            canvas.drawBitmap(
                mBitmap!!,
                startLeft,
                scanVerticalStart + animatedValue * times,
                mTipPaint
            )
        }

        mTipPaint?.let {
            val textSize = mTipPaint?.textSize ?: 0f
            var textY = scanVerticalStart + scanHeight + bitmapHeight + tipMarginTop
            var textX = (widthPixels - tipText.length * textSize) / 2
            //绘制提示语
            canvas.drawText(tipText, textX, textY, mTipPaint!!)
        }
    }

    fun onResume() {
        valueAnimator = ValueAnimator.ofInt(ANIMATOR_MAX_VALUE)
        valueAnimator?.interpolator = LinearInterpolator()
        valueAnimator?.duration = ANIMATOR_DURATION
        valueAnimator?.repeatCount = Animation.INFINITE
        valueAnimator?.repeatMode = ValueAnimator.RESTART
        valueAnimator?.addUpdateListener { animation ->
            animatedValue = animation.animatedValue as Int
            postInvalidate()
        }
        valueAnimator?.start()
    }

    fun onCancel() {
        valueAnimator?.cancel()
        valueAnimator = null
    }
}