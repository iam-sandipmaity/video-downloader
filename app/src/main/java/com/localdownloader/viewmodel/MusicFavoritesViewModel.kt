package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.data.MusicFavoritesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicFavoritesViewModel @Inject constructor(
    private val musicFavoritesStore: MusicFavoritesStore,
) : ViewModel() {
    val favoriteTaskIds: StateFlow<Set<String>> = musicFavoritesStore.favoriteTaskIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptySet())

    fun toggleFavorite(taskId: String) {
        viewModelScope.launch {
            musicFavoritesStore.toggleFavorite(taskId)
        }
    }
}
