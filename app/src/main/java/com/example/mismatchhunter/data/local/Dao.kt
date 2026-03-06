package com.example.mismatchhunter.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY dateEpochDay DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<SessionEntity?>
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun observeEpisodes(sessionId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes ORDER BY createdAt DESC")
    fun observeAllEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :episodeId")
    fun observeEpisode(episodeId: Long): Flow<EpisodeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: EpisodeEntity): Long

    @Update
    suspend fun updateEpisode(episode: EpisodeEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
