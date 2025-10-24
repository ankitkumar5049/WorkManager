package com.practice.workmanager.domain

import retrofit2.Response
import retrofit2.http.GET

data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)

interface PostApiService {
    @GET("posts")
    suspend fun getPosts(): Response<List<Post>>
}