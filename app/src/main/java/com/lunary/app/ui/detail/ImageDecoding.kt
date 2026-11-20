package com.lunary.app.ui.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Decodes an image and applies its EXIF orientation for display.
 *
 * The stored file is intentionally left untouched: phone cameras commonly save
 * landscape pixels plus an orientation flag, and rewriting the bytes would
 * destroy that metadata.
 */
fun decodeOrientedBitmap(file: File, inSampleSize: Int = 1): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
    ) ?: return null
    return bitmap.applyExifOrientation(file)
}

/**
 * Decodes a bounded image for the full-screen viewer, applying EXIF
 * orientation after sampling so portrait photos stay portrait.
 */
fun decodeOrientedBoundedImage(file: File, maxDimension: Int = 2048): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (
        bounds.outWidth / (sampleSize * 2) >= maxDimension ||
        bounds.outHeight / (sampleSize * 2) >= maxDimension
    ) {
        sampleSize *= 2
    }
    return decodeOrientedBitmap(file, sampleSize)
}

private fun Bitmap.applyExifOrientation(file: File): Bitmap {
    val orientation = runCatching {
        ExifInterface(file.path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(90f)
            matrix.postScale(1f, -1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        else -> return this
    }

    val transformed = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (transformed !== this) recycle()
    return transformed
}
