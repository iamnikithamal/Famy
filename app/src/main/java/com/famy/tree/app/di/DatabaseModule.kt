package com.famy.tree.app.di

import android.content.Context
import com.famy.tree.data.local.FamyDatabase
import com.famy.tree.data.local.dao.FamilyMemberDao
import com.famy.tree.data.local.dao.FamilyTreeDao
import com.famy.tree.data.local.dao.LifeEventDao
import com.famy.tree.data.local.dao.MediaDao
import com.famy.tree.data.local.dao.RelationshipDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FamyDatabase {
        return FamyDatabase.getInstance(context)
    }

    @Provides
    fun provideFamilyTreeDao(database: FamyDatabase): FamilyTreeDao {
        return database.familyTreeDao()
    }

    @Provides
    fun provideFamilyMemberDao(database: FamyDatabase): FamilyMemberDao {
        return database.familyMemberDao()
    }

    @Provides
    fun provideRelationshipDao(database: FamyDatabase): RelationshipDao {
        return database.relationshipDao()
    }

    @Provides
    fun provideLifeEventDao(database: FamyDatabase): LifeEventDao {
        return database.lifeEventDao()
    }

    @Provides
    fun provideMediaDao(database: FamyDatabase): MediaDao {
        return database.mediaDao()
    }
}
