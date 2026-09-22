package moe.rukamori.archivetune.db.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "local_music_alias", primaryKeys = ["sourceId", "kind"], indices = [Index("targetId")])
data class LocalMusicAlias(
    val sourceId: String,
    val targetId: String,
    val kind: String,
)
