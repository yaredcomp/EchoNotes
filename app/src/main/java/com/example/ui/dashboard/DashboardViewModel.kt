package com.example.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ActionItem
import com.example.domain.model.Note
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val notes: List<Note> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val context: Context,
    private val repository: NoteRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("echonotes_prefs", Context.MODE_PRIVATE)

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val categories = listOf("All", "Meetings", "Ideas", "Tasks", "Personal")

    init {
        seedInitialStitchNotesIfEmpty()
    }

    private fun seedInitialStitchNotesIfEmpty() {
        val alreadySeeded = prefs.getBoolean("has_seeded_initial_notes", false)
        if (alreadySeeded) {
            return
        }

        viewModelScope.launch {
            if (repository.hasAnyNotes()) {
                prefs.edit().putBoolean("has_seeded_initial_notes", true).apply()
                return@launch
            }

            val note1 = Note(
                id = "stitch_llm_demo",
                title = "What Is An LLM?",
                transcript = "When you ask artificial intelligence, like ChatGPT, a question, like, \"What is the best way to slice bread?\", it provides an answer that seems like a human expert. It works because powerful computers are trained to look for common patterns between words and phrases across trillions of tokens.",
                summary = "When you ask artificial intelligence, like ChatGPT, a question, like, \"What is the best way to slice bread?\", it provides an answer that seems like a human expert. The AI appears to know about knife varieties, ergonomics, and culinary physics, not from real-world senses, but through high-dimensional token probabilities synthesized from trillion-word datasets.",
                keyTakeaways = listOf(
                    "When prompting artificial intelligence like ChatGPT with everyday queries (e.g., \"How to slice bread\"), output reads as authoritative human counsel.",
                    "LLMs do not possess physical perception of knives or heat; knowledge exists strictly as statistical relationships between tokens.",
                    "Contextual disambiguation (e.g., baseball bat vs. flying nocturnal mammal) is resolved mathematically via co-occurring phrases.",
                    "Responses inherit biases and misinformation from web crawl data; disciplined verification is necessary for technical use."
                ),
                actionItems = listOf(
                    ActionItem(id = "task_1", title = "Review recorded note points", isCompleted = true),
                    ActionItem(id = "task_2", title = "Follow up on discussed items", isCompleted = false),
                    ActionItem(id = "task_3", title = "Share synthesized summary with product team", isCompleted = false)
                ),
                durationSeconds = 158,
                category = "Ideas",
                timestamp = System.currentTimeMillis() - 3600000
            )

            val note2 = Note(
                id = "stitch_sprint_demo",
                title = "Sprint Retro & Q4 Roadmap",
                transcript = "Key takeaways from Sarah and Dev lead: The latency for audio chunk transcription dropped from 1.2s to 340ms after moving to the streaming pipeline. We agreed to prioritize the native sync engine before v2 launch.",
                summary = "Key takeaways from Sarah and Dev lead: The latency for audio chunk transcription dropped from 1.2s to 340ms after moving to the streaming pipeline. Team confirmed roadmap milestones.",
                keyTakeaways = listOf(
                    "Latency for audio chunk transcription dropped from 1.2s to 340ms after pipeline optimization.",
                    "Prioritize the native sync engine before v2 launch.",
                    "Conduct multi-device QA testing on Android 14 and edge emulators."
                ),
                actionItems = listOf(
                    ActionItem(id = "task_4", title = "Deploy streaming audio analyzer", isCompleted = true),
                    ActionItem(id = "task_5", title = "Conduct QA testing on edge devices", isCompleted = false),
                    ActionItem(id = "task_6", title = "Finalize v2 launch milestones", isCompleted = false),
                    ActionItem(id = "task_7", title = "Publish sprint retro summary", isCompleted = false)
                ),
                durationSeconds = 878,
                category = "Meetings",
                timestamp = System.currentTimeMillis() - 86400000
            )

            repository.saveNote(note1)
            repository.saveNote(note2)
            prefs.edit().putBoolean("has_seeded_initial_notes", true).apply()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllNotes(),
        _selectedCategory,
        _searchQuery
    ) { notes, category, query ->
        val filteredByCategory = if (category == "All") {
            notes
        } else {
            notes.filter { it.category.equals(category, ignoreCase = true) }
        }

        val filteredByQuery = if (query.isBlank()) {
            filteredByCategory
        } else {
            filteredByCategory.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.transcript.contains(query, ignoreCase = true) ||
                        it.summary.contains(query, ignoreCase = true)
            }
        }

        DashboardUiState(
            notes = filteredByQuery,
            selectedCategory = category,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }
}
