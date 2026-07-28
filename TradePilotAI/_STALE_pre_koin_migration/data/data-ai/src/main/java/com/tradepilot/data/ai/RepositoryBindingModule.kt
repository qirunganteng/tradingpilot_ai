package com.tradepilot.data.ai

import com.tradepilot.data.ai.repository.AIRepositoryImpl
import com.tradepilot.domain.repository.AIRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingModule {

    @Binds
    @Singleton
    abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository
}
