/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.library.LibrarySyncTarget
import moe.rukamori.archivetune.library.RefreshLibraryUseCase
import moe.rukamori.archivetune.podcast.ObserveSavedPodcastsUseCase
import moe.rukamori.archivetune.podcast.PodcastLibraryScreenState
import moe.rukamori.archivetune.utils.reportException
import javax.inject.Inject

@HiltViewModel
class PodcastLibraryViewModel
    @Inject
    constructor(
        observeSavedPodcasts: ObserveSavedPodcastsUseCase,
        refreshLibrary: RefreshLibraryUseCase,
    ) : LibraryRefreshViewModel(refreshLibrary) {
        private val _screenState = MutableStateFlow<PodcastLibraryScreenState>(PodcastLibraryScreenState.Loading)
        val screenState = _screenState.asStateFlow()

        init {
            viewModelScope.launch {
                try {
                    observeSavedPodcasts().collect { uiState ->
                        _screenState.value =
                            if (uiState.podcasts.isEmpty()) {
                                PodcastLibraryScreenState.Empty
                            } else {
                                PodcastLibraryScreenState.Success(uiState)
                            }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    reportException(error)
                    _screenState.value = PodcastLibraryScreenState.Error(R.string.error_unknown)
                }
            }
            sync()
        }

        fun sync() {
            refreshLibrary(LibrarySyncTarget.Podcasts)
        }
    }
