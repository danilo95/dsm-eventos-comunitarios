package sv.edu.udb.eventoscomunitarios.domain.model

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val role: UserRole = UserRole.ATTENDEE
)

enum class UserRole {
    ORGANIZER,
    ATTENDEE
}
