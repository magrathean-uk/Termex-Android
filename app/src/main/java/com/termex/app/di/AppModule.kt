package com.termex.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.termex.app.data.local.ServerDao
import com.termex.app.data.local.SnippetDao
import com.termex.app.data.local.TermexDatabase
import com.termex.app.data.local.WorkplaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "termex_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TermexDatabase {
        return Room.databaseBuilder(
            context,
            TermexDatabase::class.java,
            "termex-database"
        ).build()
    }
    
    @Provides
    fun provideServerDao(database: TermexDatabase): ServerDao {
        return database.serverDao()
    }
    
    @Provides
    fun provideSnippetDao(database: TermexDatabase): SnippetDao {
        return database.snippetDao()
    }
    
    @Provides
    fun provideWorkplaceDao(database: TermexDatabase): WorkplaceDao {
        return database.workplaceDao()
    }
    
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
