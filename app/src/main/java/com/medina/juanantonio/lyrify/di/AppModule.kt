package com.medina.juanantonio.lyrify.di

import android.content.Context
import com.medina.juanantonio.lyrify.data.managers.DataStoreManager
import com.medina.juanantonio.lyrify.data.managers.IDataStoreManager
import com.medina.juanantonio.lyrify.data.managers.ISpotifyManager
import com.medina.juanantonio.lyrify.data.managers.SpotifyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppModule {

    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context
    ): IDataStoreManager {
        return DataStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideSpotifyManager(
        @ApplicationContext context: Context
    ): ISpotifyManager {
        return SpotifyManager(context)
    }
}