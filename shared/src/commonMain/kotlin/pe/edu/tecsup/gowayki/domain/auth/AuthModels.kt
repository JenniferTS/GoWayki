package pe.edu.tecsup.gowayki.domain.auth

data class SignedInUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val isAnonymous: Boolean,
    /** True solo en el alta real de una cuenta Google nueva. */
    val isNewUser: Boolean = false,
)

interface AuthRepository {
    fun currentUser(): SignedInUser?
    suspend fun signInAnonymously(): SignedInUser
    suspend fun signInWithGoogle(): SignedInUser
    suspend fun signOut()
}

class DemoAuthRepository : AuthRepository {
    private var user: SignedInUser? = SignedInUser(
        uid = "guest",
        displayName = null,
        email = null,
        isAnonymous = true,
    )

    override fun currentUser(): SignedInUser? = user

    override suspend fun signInAnonymously(): SignedInUser {
        user = SignedInUser("guest", null, null, isAnonymous = true)
        return user!!
    }

    override suspend fun signInWithGoogle(): SignedInUser {
        user = SignedInUser(
            uid = "demo-google",
            displayName = "Sharon",
            email = "sharon@example.com",
            isAnonymous = false,
            isNewUser = true,
        )
        return user!!
    }

    override suspend fun signOut() {
        user = SignedInUser("guest", null, null, isAnonymous = true)
    }
}
