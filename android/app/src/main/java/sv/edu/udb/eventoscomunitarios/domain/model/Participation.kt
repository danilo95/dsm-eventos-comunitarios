package sv.edu.udb.eventoscomunitarios.domain.model

data class Participation(
    val id: String,
    val eventId: String,
    val userId: String,
    val status: ParticipationStatus
)

enum class ParticipationStatus {
    CONFIRMED,
    WAITLIST,
    CANCELLED
}
