package com.practice.workmanager.domain

import android.content.Context

class PostRepository(context: Context) {
    private val api = RetrofitInstance.provideApi(context)

    suspend fun getPosts(): List<Post> {
        val response = api.getPosts()
        return response.body() ?: emptyList()
    }
}
