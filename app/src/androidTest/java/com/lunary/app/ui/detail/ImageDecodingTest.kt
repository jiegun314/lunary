package com.lunary.app.ui.detail

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageDecodingTest {
    @Test
    fun rotate90OrientationProducesPortraitBitmap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "orientation-${System.nanoTime()}.jpg")
        try {
            val source = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
            file.outputStream().use { output ->
                source.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            source.recycle()

            ExifInterface(file.path).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_90.toString()
                )
                saveAttributes()
            }

            val decoded = decodeOrientedBitmap(file)
            assertNotNull(decoded)
            assertEquals(300, decoded!!.width)
            assertEquals(400, decoded.height)
            decoded.recycle()

            val thumbnail = decodeOrientedBitmap(file, inSampleSize = 2)
            assertNotNull(thumbnail)
            assertEquals(150, thumbnail!!.width)
            assertEquals(200, thumbnail.height)
            thumbnail.recycle()

            val bounded = decodeOrientedBoundedImage(file, maxDimension = 100)
            assertNotNull(bounded)
            assertTrue(bounded!!.width <= 100 && bounded.height <= 100)
            bounded.recycle()
        } finally {
            file.delete()
        }
    }
}
