package com.famy.tree.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.famy.tree.data.local.converter.Converters
import com.famy.tree.data.local.dao.FamilyMemberDao
import com.famy.tree.data.local.dao.FamilyTreeDao
import com.famy.tree.data.local.dao.LifeEventDao
import com.famy.tree.data.local.dao.MediaDao
import com.famy.tree.data.local.dao.RelationshipDao
import com.famy.tree.data.local.entity.FamilyMemberEntity
import com.famy.tree.data.local.entity.FamilyTreeEntity
import com.famy.tree.data.local.entity.LifeEventEntity
import com.famy.tree.data.local.entity.MediaEntity
import com.famy.tree.data.local.entity.RelationshipEntity

@Database(
    entities = [
        FamilyTreeEntity::class,
        FamilyMemberEntity::class,
        RelationshipEntity::class,
        LifeEventEntity::class,
        MediaEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FamyDatabase : RoomDatabase() {

    abstract fun familyTreeDao(): FamilyTreeDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun lifeEventDao(): LifeEventDao
    abstract fun mediaDao(): MediaDao

    companion object {
        const val DATABASE_NAME = "famy_database"

        @Volatile
        private var INSTANCE: FamyDatabase? = null

        fun getInstance(context: Context): FamyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): FamyDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FamyDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
