package com.example.spendly

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.example.spendly.R

object ImageScaler {

    fun setResourceSafely(context: Context, imageView: ImageView, resourceId: Int) {
        try {
            // Get resource dimensions without loading the full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resourceId, options)

            // Calculate appropriate sample size
            var sampleSize = 1
            if (options.outHeight > 1024 || options.outWidth > 1024) {
                val heightRatio = options.outHeight / 1024
                val widthRatio = options.outWidth / 1024
                sampleSize = Math.max(heightRatio, widthRatio)
            }

            // Now decode with scaling
            options.apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // Fallback to a default image if there's a problem
            imageView.setImageResource(R.drawable.ic_category_other)
        }
    }
}