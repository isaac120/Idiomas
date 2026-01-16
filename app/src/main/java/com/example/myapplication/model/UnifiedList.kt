package com.example.myapplication.model

import com.example.myapplication.data.VerbList
import com.example.myapplication.data.VocabularyList

/**
 * Unified representation of both vocabulary and verb lists for display in a single RecyclerView.
 */
sealed class UnifiedList {
    abstract val id: Long
    abstract val name: String
    abstract val itemCount: Int
    abstract val createdAt: Long

    data class Vocabulary(val list: VocabularyList) : UnifiedList() {
        override val id = list.id
        override val name = list.name
        override val itemCount = list.wordCount
        override val createdAt = list.createdAt
    }

    data class Verbs(val list: VerbList) : UnifiedList() {
        override val id = list.id
        override val name = list.name
        override val itemCount = list.verbCount
        override val createdAt = list.createdAt
    }
}
