package com.tradepilot.feature.mentor.di

import com.tradepilot.feature.mentor.MentorViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureMentorModule = module {
    viewModel { MentorViewModel(get(), get()) }
}
