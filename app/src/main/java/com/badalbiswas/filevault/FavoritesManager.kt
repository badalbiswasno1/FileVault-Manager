package com.badalbiswas.filevault

import android.content.Context

object FavoritesManager {
    private const val PREF_NAME = "favorites_pref"
    private const val KEY_PATHS = "favorite_paths"

    fun getFavorites(context: Context): MutableSet<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PATHS, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    fun addFavorite(context: Context, path: String) {
        val favs = getFavorites(context)
        favs.add(path)
        save(context, favs)
    }

    fun removeFavorite(context: Context, path: String) {
        val favs = getFavorites(context)
        favs.remove(path)
        save(context, favs)
    }

    fun isFavorite(context: Context, path: String): Boolean {
        return getFavorites(context).contains(path)
    }

    private fun save(context: Context, favs: MutableSet<String>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_PATHS, favs).apply()
    }
}
