package sv.edu.udb.eventoscomunitarios.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import sv.edu.udb.eventoscomunitarios.domain.model.UserProfile
import sv.edu.udb.eventoscomunitarios.domain.model.UserRole

object FirebaseAuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun loginOrCreateUser(
        email: String,
        password: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("No se pudo obtener el usuario.")))
                } else {
                    loadOrCreateProfile(user.uid, user.email.orEmpty(), onResult)
                }
            }
            .addOnFailureListener { error ->
                val errorCode = (error as? FirebaseAuthException)?.errorCode
                if (errorCode == "ERROR_USER_NOT_FOUND" || errorCode == "ERROR_INVALID_CREDENTIAL") {
                    createUser(email, password, onResult)
                } else {
                    onResult(Result.failure(error))
                }
            }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getCurrentUserEmail(): String = auth.currentUser?.email.orEmpty()

    fun signOut() {
        auth.signOut()
    }

    private fun createUser(
        email: String,
        password: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("No se pudo crear el usuario.")))
                } else {
                    loadOrCreateProfile(user.uid, user.email.orEmpty(), onResult)
                }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    private fun loadOrCreateProfile(
        uid: String,
        email: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        val userDocument = firestore.collection("users").document(uid)
        userDocument.get()
            .addOnSuccessListener { snapshot ->
                val profile = if (snapshot.exists()) {
                    UserProfile(
                        id = snapshot.id,
                        fullName = snapshot.getString("fullName").orEmpty(),
                        email = snapshot.getString("email").orEmpty(),
                        role = runCatching {
                            UserRole.valueOf(snapshot.getString("role") ?: UserRole.ATTENDEE.name)
                        }.getOrDefault(UserRole.ATTENDEE)
                    )
                } else {
                    createDefaultProfile(uid, email)
                }

                userDocument.set(profile.toMap())
                    .addOnSuccessListener { onResult(Result.success(profile)) }
                    .addOnFailureListener { onResult(Result.success(profile)) }
            }
            .addOnFailureListener {
                onResult(Result.success(createDefaultProfile(uid, email)))
            }
    }

    private fun createDefaultProfile(uid: String, email: String): UserProfile {
        return UserProfile(
            id = uid,
            fullName = email.substringBefore("@").ifBlank { "Usuario" },
            email = email,
            role = UserRole.ATTENDEE
        )
    }

    private fun UserProfile.toMap(): Map<String, Any> {
        return mapOf(
            "fullName" to fullName,
            "email" to email,
            "role" to role.name
        )
    }
}
