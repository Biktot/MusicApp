package moe.rukamori.archivetune.db

import java.text.Normalizer
import java.util.Locale

object LocalMusicIdentity {
    private val whitespace = Regex("\\s+")

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .trim()
            .replace(whitespace, " ")
            .lowercase(Locale.ROOT)
}
