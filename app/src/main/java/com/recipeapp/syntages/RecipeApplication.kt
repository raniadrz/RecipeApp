package com.recipeapp.syntages

import android.app.Application
import com.recipeapp.syntages.data.AppDatabase
import com.recipeapp.syntages.data.RecipeRepository

class AppContainer(val app: Application) {
    val database: AppDatabase by lazy { AppDatabase.create(app) }
    val repository: RecipeRepository by lazy { RecipeRepository(database) }
}

class RecipeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
