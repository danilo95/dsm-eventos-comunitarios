package sv.edu.udb.eventoscomunitarios.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import sv.edu.udb.eventoscomunitarios.domain.model.CommunityEvent
import sv.edu.udb.eventoscomunitarios.domain.model.EventComment
import sv.edu.udb.eventoscomunitarios.domain.model.EventStats
import sv.edu.udb.eventoscomunitarios.domain.model.EventStatus

object FirebaseEventRepository : EventRepository {
    private const val EVENTS_COLLECTION = "events"

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val eventsCollection by lazy { firestore.collection(EVENTS_COLLECTION) }

    override fun getUpcomingEvents(onResult: (Result<List<CommunityEvent>>) -> Unit) {
        eventsCollection
            .whereEqualTo("status", EventStatus.UPCOMING.name)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(
                    Result.success(
                        snapshot.documents
                            .mapNotNull { it.toCommunityEvent() }
                            .sortedBy { it.date }
                    )
                )
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun getPastEvents(onResult: (Result<List<CommunityEvent>>) -> Unit) {
        eventsCollection
            .whereEqualTo("status", EventStatus.PAST.name)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(
                    Result.success(
                        snapshot.documents
                            .mapNotNull { it.toCommunityEvent() }
                            .sortedByDescending { it.date }
                    )
                )
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun getEventById(eventId: String, onResult: (Result<CommunityEvent?>) -> Unit) {
        eventsCollection.document(eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(Result.success(snapshot.toCommunityEvent()))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun getStats(onResult: (Result<EventStats>) -> Unit) {
        eventsCollection.get()
            .addOnSuccessListener { snapshot ->
                val events = snapshot.documents.mapNotNull { it.toCommunityEvent() }

                onResult(
                    Result.success(
                        EventStats(
                            totalEvents = events.size,
                            upcomingEvents = events.count { it.status == EventStatus.UPCOMING },
                            pastEvents = events.count { it.status == EventStatus.PAST },
                            confirmedParticipations = events.sumOf { it.confirmedCount }
                        )
                    )
                )
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun createEvent(event: CommunityEvent, onResult: (Result<Unit>) -> Unit) {
        val document = eventsCollection.document()
        val currentUserId = FirebaseAuthRepository.getCurrentUserId().orEmpty()

        if (currentUserId.isBlank()) {
            onResult(Result.failure(IllegalStateException("Debe iniciar sesión.")))
            return
        }

        val eventToCreate = event.copy(
            id = document.id,
            organizerId = currentUserId,
            confirmedCount = 0,
            confirmedUserIds = emptyList(),
            status = EventStatus.UPCOMING
        )

        document.set(eventToCreate.toCreateMap())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun updateEvent(event: CommunityEvent, onResult: (Result<Unit>) -> Unit) {
        val currentUserId = FirebaseAuthRepository.getCurrentUserId().orEmpty()

        if (currentUserId.isBlank()) {
            onResult(Result.failure(IllegalStateException("Debe iniciar sesión.")))
            return
        }

        val eventDocument = eventsCollection.document(event.id)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(eventDocument)
            val organizerId = snapshot.getString("organizerId").orEmpty()

            if (organizerId != currentUserId) {
                throw EventOwnerRequiredException()
            }

            transaction.update(eventDocument, event.toUpdateMap())
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun deactivateEvent(eventId: String, onResult: (Result<Unit>) -> Unit) {
        val currentUserId = FirebaseAuthRepository.getCurrentUserId().orEmpty()

        if (currentUserId.isBlank()) {
            onResult(Result.failure(IllegalStateException("Debe iniciar sesión.")))
            return
        }

        val eventDocument = eventsCollection.document(eventId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(eventDocument)
            val organizerId = snapshot.getString("organizerId").orEmpty()

            if (organizerId != currentUserId) {
                throw EventOwnerRequiredException()
            }

            transaction.update(
                eventDocument,
                mapOf(
                    "status" to EventStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun confirmAttendance(eventId: String, onResult: (Result<Unit>) -> Unit) {
        val userId = FirebaseAuthRepository.getCurrentUserId().orEmpty()

        if (userId.isBlank()) {
            onResult(Result.failure(IllegalStateException("Debe iniciar sesión.")))
            return
        }

        val eventDocument = eventsCollection.document(eventId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(eventDocument)
            val confirmedUserIds = snapshot.get("confirmedUserIds") as? List<*> ?: emptyList<Any>()

            if (confirmedUserIds.contains(userId)) {
                throw AttendanceAlreadyConfirmedException()
            }

            transaction.update(
                eventDocument,
                mapOf(
                    "confirmedCount" to FieldValue.increment(1),
                    "confirmedUserIds" to FieldValue.arrayUnion(userId)
                )
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun cancelAttendance(eventId: String, onResult: (Result<Unit>) -> Unit) {
        val userId = FirebaseAuthRepository.getCurrentUserId().orEmpty()

        if (userId.isBlank()) {
            onResult(Result.failure(IllegalStateException("Debe iniciar sesión.")))
            return
        }

        val eventDocument = eventsCollection.document(eventId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(eventDocument)
            val confirmedUserIds = snapshot.get("confirmedUserIds") as? List<*> ?: emptyList<Any>()

            if (!confirmedUserIds.contains(userId)) {
                throw AttendanceNotConfirmedException()
            }

            val currentConfirmedCount = snapshot.getLong("confirmedCount") ?: 0L
            val newConfirmedCount = if (currentConfirmedCount > 0L) {
                currentConfirmedCount - 1L
            } else {
                0L
            }

            transaction.update(
                eventDocument,
                mapOf(
                    "confirmedCount" to newConfirmedCount,
                    "confirmedUserIds" to FieldValue.arrayRemove(userId)
                )
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun getComments(eventId: String, onResult: (Result<List<EventComment>>) -> Unit) {
        eventsCollection.document(eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                val comments = snapshot.toEventComments(eventId)
                    .sortedByDescending { it.createdAtMillis }

                onResult(Result.success(comments))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun addComment(eventId: String, commentText: String, onResult: (Result<Unit>) -> Unit) {
        val userEmail = FirebaseAuthRepository.getCurrentUserEmail()
        val userName = userEmail.substringBefore("@").ifBlank { "Usuario" }

        val document = eventsCollection.document(eventId)
        val commentId = document.collection("comments").document().id

        val comment = mapOf(
            "id" to commentId,
            "eventId" to eventId,
            "userId" to FirebaseAuthRepository.getCurrentUserId().orEmpty(),
            "userName" to userName,
            "comment" to commentText,
            "createdAtMillis" to System.currentTimeMillis()
        )

        document.update("comments", FieldValue.arrayUnion(comment))
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    private fun CommunityEvent.toCreateMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "date" to date,
            "time" to time,
            "location" to location,
            "organizerName" to organizerName,
            "organizerId" to organizerId,
            "organizerEmail" to FirebaseAuthRepository.getCurrentUserEmail(),
            "capacity" to capacity,
            "confirmedCount" to confirmedCount,
            "status" to status.name,
            "confirmedUserIds" to confirmedUserIds,
            "comments" to emptyList<Map<String, Any>>(),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }

    private fun CommunityEvent.toUpdateMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "description" to description,
            "date" to date,
            "time" to time,
            "location" to location,
            "capacity" to capacity,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun DocumentSnapshot.toCommunityEvent(): CommunityEvent? {
        if (!exists()) return null

        return CommunityEvent(
            id = getString("id").orEmpty().ifBlank { id },
            title = getString("title").orEmpty(),
            description = getString("description").orEmpty(),
            date = getString("date").orEmpty(),
            time = getString("time").orEmpty(),
            location = getString("location").orEmpty(),
            organizerName = getString("organizerName").orEmpty(),
            organizerId = getString("organizerId").orEmpty(),
            capacity = getLong("capacity")?.toInt() ?: 0,
            confirmedCount = getLong("confirmedCount")?.toInt() ?: 0,
            status = runCatching {
                EventStatus.valueOf(getString("status") ?: EventStatus.UPCOMING.name)
            }.getOrDefault(EventStatus.UPCOMING),
            confirmedUserIds = (get("confirmedUserIds") as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()
        )
    }

    private fun DocumentSnapshot.toEventComment(eventId: String): EventComment? {
        if (!exists()) return null

        return EventComment(
            id = getString("id").orEmpty().ifBlank { id },
            eventId = getString("eventId").orEmpty().ifBlank { eventId },
            userId = getString("userId").orEmpty(),
            userName = getString("userName").orEmpty().ifBlank { "Usuario" },
            comment = getString("comment").orEmpty(),
            createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    }

    private fun DocumentSnapshot.toEventComments(eventId: String): List<EventComment> {
        val commentMaps = get("comments") as? List<*> ?: return emptyList()

        return commentMaps.mapNotNull { value ->
            val comment = value as? Map<*, *> ?: return@mapNotNull null

            EventComment(
                id = comment["id"] as? String ?: "",
                eventId = comment["eventId"] as? String ?: eventId,
                userId = comment["userId"] as? String ?: "",
                userName = (comment["userName"] as? String).orEmpty().ifBlank { "Usuario" },
                comment = comment["comment"] as? String ?: "",
                createdAtMillis = comment["createdAtMillis"] as? Long ?: 0L
            )
        }
    }
}

class AttendanceAlreadyConfirmedException : IllegalStateException(
    "La asistencia ya fue confirmada."
)

class EventOwnerRequiredException : IllegalStateException(
    "Solo el dueño del evento puede realizar esta acción."
)

class AttendanceNotConfirmedException : IllegalStateException(
    "La asistencia no estaba confirmada."
)