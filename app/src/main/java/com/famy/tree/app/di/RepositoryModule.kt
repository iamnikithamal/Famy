package com.famy.tree.app.di

import com.famy.tree.data.repository.FamilyMemberRepositoryImpl
import com.famy.tree.data.repository.FamilyTreeRepositoryImpl
import com.famy.tree.data.repository.LifeEventRepositoryImpl
import com.famy.tree.data.repository.MediaRepositoryImpl
import com.famy.tree.data.repository.RelationshipRepositoryImpl
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.FamilyTreeRepository
import com.famy.tree.domain.repository.LifeEventRepository
import com.famy.tree.domain.repository.MediaRepository
import com.famy.tree.domain.repository.RelationshipRepository
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
    abstract fun bindFamilyTreeRepository(impl: FamilyTreeRepositoryImpl): FamilyTreeRepository

    @Binds
    @Singleton
    abstract fun bindFamilyMemberRepository(impl: FamilyMemberRepositoryImpl): FamilyMemberRepository

    @Binds
    @Singleton
    abstract fun bindRelationshipRepository(impl: RelationshipRepositoryImpl): RelationshipRepository

    @Binds
    @Singleton
    abstract fun bindLifeEventRepository(impl: LifeEventRepositoryImpl): LifeEventRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
