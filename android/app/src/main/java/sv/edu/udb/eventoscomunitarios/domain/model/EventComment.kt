package sv.edu.udb.eventoscomunitarios.domain.model

data class EventComment(
    val id: String,
    val eventId: String,
    val userId: String,
    val userName: String,
    val comment: String,
    val createdAtMillis: Long = 0L
)
