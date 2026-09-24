package com.danstudios.reelnotes

import android.app.Application
import com.danstudios.reelnotes.data.local.AppDatabase
import com.danstudios.reelnotes.data.local.PreferencesManager
import com.danstudios.reelnotes.data.repository.ReelNoteRepository
import com.danstudios.reelnotes.data.util.SampleDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReelNotesApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ReelNoteRepository
        private set

    lateinit var preferences: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = ReelNoteRepository(database.reelNoteDao())
        preferences = PreferencesManager(this)

        if (preferences.isFirstRun) {
            preferences.isFirstRun = false
            CoroutineScope(Dispatchers.IO).launch {
                repository.insertAll(SampleDataProvider.getSampleNotes())
            }
        }
    }
}
