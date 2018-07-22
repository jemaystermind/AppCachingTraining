package me.jemaystermind.appcachingtraining

import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubService {
    @GET("users/{username}/repos")
    fun repos(@Path("username") username: String): Single<List<Repos>>

    @GET("users/{username}/repos")
    fun reposRaw(@Path("username") username: String): Single<ResponseBody>
}