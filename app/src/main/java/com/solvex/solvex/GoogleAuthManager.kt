package com.solvex.solvex

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

data class GoogleUser(
    val email: String,
    val name: String?,
    val photoUrl: String?,
    val idToken: String
)

sealed class AuthResult {
    data class Success(val user: GoogleUser) : AuthResult()
    object Cancelled : AuthResult()
    data class Error(val message: String) : AuthResult()
}

// administrador de autenticacion con google usando credential manager
class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    // id de cliente web configurado en firebase y google cloud console
    private val webClientId = "170158622353-af3ls0k64c7a1v9nvqkjeiolokj797rl.apps.googleusercontent.com"

    suspend fun signIn(): AuthResult {
        // opcion para mostrar todas las cuentas de google del dispositivo sin filtrar
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        // opcion nativa de compatibilidad
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                AuthResult.Success(
                    GoogleUser(
                        email = google.id,
                        name = google.displayName,
                        photoUrl = google.profilePictureUri?.toString(),
                        idToken = google.idToken
                    )
                )
            } else {
                AuthResult.Error("Tipo de credencial no soportado")
            }
        } catch (e: GetCredentialCancellationException) {
            // el usuario cerro la ventana de cuentas de google
            AuthResult.Cancelled
        } catch (e: NoCredentialException) {
            AuthResult.Error("No hay cuentas de Google registradas en este dispositivo")
        } catch (e: GoogleIdTokenParsingException) {
            AuthResult.Error("Token de Google invalido")
        } catch (e: GetCredentialException) {
            AuthResult.Error(e.message ?: "Error al iniciar sesion con Google")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Error de autenticacion con Google")
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // ignorar errores al limpiar credenciales
        }
    }
}
