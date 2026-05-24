package sv.edu.udb.eventoscomunitarios.domain.model

data class EventStats(
    val totalEvents: Int,
    val upcomingEvents: Int,
    val pastEvents: Int,
    val confirmedParticipations: Int
)
