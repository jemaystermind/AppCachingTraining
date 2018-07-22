package me.jemaystermind.appcachingtraining

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubRepoRepository {
    @GET("/users/{username}/repos")
    fun repos(@Path("username") username: String): Single<List<Repos>>
}