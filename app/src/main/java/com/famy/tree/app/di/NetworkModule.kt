package com.famy.tree.app.di

import com.famy.tree.data.remote.LocationServiceImpl
import com.famy.tree.domain.service.LocationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindLocationService(impl: LocationServiceImpl): LocationService
}
