package sv.edu.udb.eventoscomunitarios.data

import sv.edu.udb.eventoscomunitarios.domain.model.CommunityEvent
import sv.edu.udb.eventoscomunitarios.domain.model.EventStats
import sv.edu.udb.eventoscomunitarios.domain.model.EventStatus

object InMemoryEventRepository : EventRepository {
    private val events = listOf(
        CommunityEvent(
            id = "evt-001",
            title = "Jornada de limpieza comunitaria",
            description = "Actividad para recuperar espacios verdes del barrio.",
            date = "2026-05-30",
            time = "08:00",
            location = "Parque central",
            organizerName = "Comité local",
            capacity = 50,
            confirmedCount = 18
        ),
        CommunityEvent(
            id = "evt-002",
            title = "Taller de reciclaje",
            description = "Charla y práctica sobre separación de residuos.",
            date = "2026-06-02",
            time = "15:00",
            location = "Casa comunal",
            organizerName = "Voluntarios ambientales",
            capacity = 35,
            confirmedCount = 12
        ),
        CommunityEvent(
            id = "evt-003",
            title = "Feria de salud",
            description = "Evento pasado usado como ejemplo para historial.",
            date = "2026-05-10",
            time = "09:00",
            location = "Cancha municipal",
            organizerName = "Unidad de salud",
            capacity = 100,
            confirmedCount = 76,
            status = EventStatus.PAST
        )
    )

    override fun getUpcomingEvents(onResult: (Result<List<CommunityEvent>>) -> Unit) {
        onResult(Result.success(events.filter { it.status == EventStatus.UPCOMING }))
    }

    override fun getPastEvents(onResult: (Result<List<CommunityEvent>>) -> Unit) {
        onResult(Result.success(events.filter { it.status == EventStatus.PAST }))
    }

    override fun getEventById(eventId: String, onResult: (Result<CommunityEvent?>) -> Unit) {
        onResult(Result.success(events.firstOrNull { it.id == eventId }))
    }

    override fun getStats(onResult: (Result<EventStats>) -> Unit) {
        onResult(Result.success(EventStats(
            totalEvents = events.size,
            upcomingEvents = events.count { it.status == EventStatus.UPCOMING },
            pastEvents = events.count { it.status == EventStatus.PAST },
            confirmedParticipations = events.sumOf { it.confirmedCount }
        )))
    }

    override fun createEvent(event: CommunityEvent, onResult: (Result<Unit>) -> Unit) {
        onResult(Result.failure(UnsupportedOperationException("Repositorio de solo lectura.")))
    }

    override fun confirmAttendance(eventId: String, onResult: (Result<Unit>) -> Unit) {
        onResult(Result.failure(UnsupportedOperationException("Repositorio de solo lectura.")))
    }
}
