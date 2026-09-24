package com.example.ocr.engine

import android.content.Context

object OcrEngineFactory {
    private var instance: OcrEngine? = null
    private var routerInstance: OcrEngineRouter? = null

    fun getEngine(context: Context): OcrEngine {
        return instance ?: synchronized(this) {
            instance ?: TesseractOcrEngine(context.applicationContext).also {
                instance = it
            }
        }
    }

    fun getRouter(context: Context): OcrEngineRouter {
        return routerInstance ?: synchronized(this) {
            routerInstance ?: OcrEngineRouter(context.applicationContext).also {
                routerInstance = it
            }
        }
    }
}
