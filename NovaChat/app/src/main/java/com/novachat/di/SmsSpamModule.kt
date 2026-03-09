package com.novachat.di

import com.novachat.core.sms.AllowlistChecker
import com.novachat.core.sms.ScamDetector
import com.novachat.core.sms.hebrew.CampaignDetector
import com.novachat.core.sms.hebrew.HebrewSpamEngine
import com.novachat.core.sms.hebrew.HebrewSpamMlLayer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SmsSpamModule {

    @Binds
    @Singleton
    abstract fun bindAllowlistChecker(scamDetector: ScamDetector): AllowlistChecker

    companion object {
        @Provides
        @Singleton
        fun provideHebrewSpamMlLayer(): HebrewSpamMlLayer = HebrewSpamMlLayer()

        @Provides
        @Singleton
        fun provideCampaignDetector(): CampaignDetector = CampaignDetector(bufferSize = 50)

        @Provides
        @Singleton
        fun provideHebrewSpamEngine(
            mlLayer: HebrewSpamMlLayer,
            campaignDetector: CampaignDetector,
            allowlistChecker: AllowlistChecker
        ): HebrewSpamEngine = HebrewSpamEngine(mlLayer, campaignDetector, allowlistChecker)
    }
}
