package com.solvex.solvex.data.remote

import com.solvex.solvex.data.entity.MovimientoEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class AuthRequest(val email: String, val contrasena: String)
data class RegisterRequest(val nombre: String, val email: String, val contrasena: String)
data class GoogleRequest(val idToken: String)
data class UserResponse(val id: Int, val nombre: String, val email: String)
data class AuthResponse(val accessToken: String, val tokenType: String, val usuario: UserResponse)
data class ProfileUpdateRequest(val nombre: String)
data class MovimientoRequest(val titulo: String, val monto: Double, val categoria: String, val tipo: String, val fecha: Long)
data class CuentaResponse(
    val userEmail: String,
    val saldoInicial: Double = 0.0,
    val saldoFinal: Double = 0.0,
    val totalIngresos: Double = 0.0,
    val totalGastos: Double = 0.0,
    val banco: String = ""
)
data class CuentaRequest(
    val saldoInicial: Double,
    val saldoFinal: Double,
    val totalIngresos: Double,
    val totalGastos: Double,
    val banco: String
)
data class ExtractoRequest(val nombreArchivo: String, val banco: String, val totalMovimientos: Int, val estado: String = "Procesado")
data class ExtractoResponse(
    val id: String,
    val nombreArchivo: String,
    val banco: String,
    val fechaProcesado: Long,
    val totalMovimientos: Int,
    val estado: String
)

interface SolvexApi {
    @POST("api/auth/register") suspend fun register(@Body request: RegisterRequest): AuthResponse
    @POST("api/auth/login") suspend fun login(@Body request: AuthRequest): AuthResponse
    @POST("api/auth/google") suspend fun googleLogin(@Body request: GoogleRequest): AuthResponse

    @GET("api/usuarios/me") suspend fun getProfile(): UserResponse
    @PATCH("api/usuarios/me") suspend fun updateProfile(@Body request: ProfileUpdateRequest): UserResponse

    @GET("api/movimientos") suspend fun getMovimientos(): List<MovimientoEntity>
    @POST("api/movimientos") suspend fun createMovimiento(@Body request: MovimientoRequest): MovimientoEntity
    @POST("api/movimientos/importar") suspend fun importMovimientos(@Body request: List<MovimientoRequest>): List<MovimientoEntity>
    @PUT("api/movimientos/{id}") suspend fun updateMovimiento(@Path("id") id: Int, @Body request: MovimientoRequest): MovimientoEntity
    @DELETE("api/movimientos/{id}") suspend fun deleteMovimiento(@Path("id") id: Int): Response<Unit>
    @DELETE("api/movimientos") suspend fun deleteAllMovimientos(): Response<Unit>

    @GET("api/cuenta") suspend fun getCuenta(): CuentaResponse
    @PUT("api/cuenta") suspend fun saveCuenta(@Body request: CuentaRequest): CuentaResponse
    @DELETE("api/cuenta") suspend fun deleteCuenta(): Response<Unit>
    @GET("api/extractos") suspend fun getExtractos(): List<ExtractoResponse>
    @POST("api/extractos") suspend fun addExtracto(@Body request: ExtractoRequest): ExtractoResponse
    @DELETE("api/extractos") suspend fun deleteExtractos(): Response<Unit>
}
