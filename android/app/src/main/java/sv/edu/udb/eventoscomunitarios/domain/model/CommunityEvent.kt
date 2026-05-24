package sv.edu.udb.eventoscomunitarios.domain.model

data class CommunityEvent(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val location: String,
    val organizerName: String,
    val capacity: Int,
    val confirmedCount: Int,
    val status: EventStatus = EventStatus.UPCOMING,
    val confirmedUserIds: List<String> = emptyList()
)

enum class EventStatus {
    UPCOMING,
    PAST,
    CANCELLED
}
