package moe.rukamori.archivetune.localmedia

import javax.inject.Inject

class ScanLocalMusicUseCase @Inject constructor(
    private val scanner: LocalSongScanner,
) {
    suspend operator fun invoke(config: LocalSongScanConfig): LocalSongScanSummary =
        scanner.scanDevice(
            config.copy(
                minimumDurationSeconds = config.sanitizedMinimumDurationSeconds,
                includedFolders = config.sanitizedIncludedFolders,
                excludedFolders = config.sanitizedExcludedFolders,
            ),
        )
}
