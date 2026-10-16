package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.files.FileStorageManager
import com.example.data.repository.NoteRepositoryImpl
import com.example.domain.model.ActionItem
import com.example.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name string resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EchoNotes", appName)
    }

    @Test
    fun `file storage manager saves and reads transcript file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileStorage = FileStorageManager(context)
        val noteId = "test_note_123"
        val sampleTranscript = "Today we agreed to launch the new feature on Friday."

        val path = fileStorage.saveTranscript(noteId, sampleTranscript)
        assertTrue(path.endsWith("${noteId}_transcript.txt"))

        val readBack = fileStorage.readTranscript(noteId)
        assertEquals(sampleTranscript, readBack)
    }

    @Test
    fun `note repository persists and retrieves note reactive flow`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = NoteRepositoryImpl(context)

        val note = Note(
            id = "test_repo_note",
            title = "Sprint Planning Meeting",
            transcript = "Team discussed backlog items and assigned tasks.",
            summary = "Sprint planning completed.",
            keyTakeaways = listOf("Focus on core features", "Deliver by Friday"),
            actionItems = listOf(
                ActionItem(id = "task_1", title = "Write unit tests", isCompleted = false)
            ),
            durationSeconds = 60,
            category = "Meetings"
        )

        repository.saveNote(note)

        val retrieved = repository.getNoteByIdSync("test_repo_note")
        assertNotNull(retrieved)
        assertEquals("Sprint Planning Meeting", retrieved?.title)
        assertEquals(1, retrieved?.actionItems?.size)
        assertEquals("Write unit tests", retrieved?.actionItems?.first()?.title)
    }
}
