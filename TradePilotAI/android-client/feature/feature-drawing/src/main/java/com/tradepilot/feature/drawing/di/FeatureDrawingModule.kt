package com.tradepilot.feature.drawing.di

import com.tradepilot.feature.drawing.AnnotationEngine
import org.koin.dsl.module

val featureDrawingModule = module {
    single { AnnotationEngine() }
}
