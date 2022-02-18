package firestore

data class SignInWithPasswordPayload(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean
)
