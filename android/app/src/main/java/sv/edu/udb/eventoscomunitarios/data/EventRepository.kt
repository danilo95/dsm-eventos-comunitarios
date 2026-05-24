package sv.edu.udb.eventoscomunitarios.data

import sv.edu.udb.eventoscomunitarios.domain.model.CommunityEvent
import sv.edu.udb.eventoscomunitarios.domain.model.EventStats

interface EventRepository {
    fun getUpcomingEvents(onResult: (Result<List<CommunityEvent>>) -> Unit)
    fun getPastEvents(onResult: (Result<List<CommunityEvent>>) -> Unit)
    fun getEventById(eventId: String, onResult: (Result<CommunityEvent?>) -> Unit)
    fun getStats(onResult: (Result<EventStats>) -> Unit)
    fun createEvent(event: CommunityEvent, onResult: (Result<Unit>) -> Unit)
    fun confirmAttendance(eventId: String, onResult: (Result<Unit>) -> Unit)
}
