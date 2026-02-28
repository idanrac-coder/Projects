package com.novachat.di

import com.novachat.data.repository.BlockRepositoryImpl
import com.novachat.data.repository.ContactRepositoryImpl
import com.novachat.data.repository.ConversationRepositoryImpl
import com.novachat.data.repository.ThemeRepositoryImpl
import com.novachat.domain.repository.BlockRepository
import com.novachat.domain.repository.ContactRepository
import com.novachat.domain.repository.ConversationRepository
import com.novachat.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindBlockRepository(impl: BlockRepositoryImpl): BlockRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
}
