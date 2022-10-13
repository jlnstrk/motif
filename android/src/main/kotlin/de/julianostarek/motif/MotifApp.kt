package de.julianostarek.motif

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class MotifApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        androidStartKoin(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .build()
    }
}