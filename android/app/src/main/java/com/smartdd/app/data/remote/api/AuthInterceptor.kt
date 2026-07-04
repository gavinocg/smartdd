package com.smartdd.app.data.remote.api

import com.smartdd.app.data.local.preferences.TokenManager
import com.smartdd.app.data.repository.AuthRepository
import com.smartdd.app.domain.model.Result
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: dagger.Lazy<AuthRepository>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        val token = tokenManager.getAccessToken()
        val authenticatedRequest = if (token != null) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else request

        val response = chain.proceed(authenticatedRequest)

        if (response.code == 401 && token != null) {
            response.close()
            val newToken = runBlocking {
                when (val result = authRepository.get().refreshToken()) {
                    is Result.Success -> result.data
                    is Result.Error -> null
                }
            }
            if (newToken != null) {
                return chain.proceed(
                    request.newBuilder().header("Authorization", "Bearer $newToken").build()
                )
            }
        }
        return response
    }
}
