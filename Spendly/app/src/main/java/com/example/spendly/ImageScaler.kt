package com.example.spendly

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.example.spendly.R

object ImageScaler {

    fun setResourceSafely(context: Context, imageView: ImageView, resourceId: Int) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resourceId, options)

            var sampleSize = 1
            if (options.outHeight > 1024 || options.outWidth > 1024) {
                val heightRatio = options.outHeight / 1024
                val widthRatio = options.outWidth / 1024
                sampleSize = Math.max(heightRatio, widthRatio)
            }

            options.apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.ic_category_other)
        }
    }
}