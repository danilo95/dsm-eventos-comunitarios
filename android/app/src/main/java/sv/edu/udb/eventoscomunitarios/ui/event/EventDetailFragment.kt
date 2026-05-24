package sv.edu.udb.eventoscomunitarios.ui.event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import sv.edu.udb.eventoscomunitarios.R
import sv.edu.udb.eventoscomunitarios.data.AttendanceAlreadyConfirmedException
import sv.edu.udb.eventoscomunitarios.data.FirebaseAuthRepository
import sv.edu.udb.eventoscomunitarios.data.FirebaseEventRepository
import sv.edu.udb.eventoscomunitarios.databinding.FragmentEventDetailBinding
import sv.edu.udb.eventoscomunitarios.domain.model.EventComment
import sv.edu.udb.eventoscomunitarios.domain.model.EventStatus

class EventDetailFragment : Fragment() {

    private var userHasConfirmedAttendance = false
    private var binding: FragmentEventDetailBinding? = null
    private val commentDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private var currentEventId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return requireNotNull(binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentEventId = arguments?.getString("eventId").orEmpty()

        loadEvent(currentEventId)
        loadComments(currentEventId)

        binding?.rsvpButton?.setOnClickListener {
            if (userHasConfirmedAttendance) {
                cancelAttendance(currentEventId)
            } else {
                confirmAttendance(currentEventId)
            }
        }

        binding?.sendCommentButton?.setOnClickListener {
            sendComment(currentEventId)
        }

        binding?.editEventButton?.setOnClickListener {
            val bundle = Bundle().apply {
                putString("eventId", currentEventId)
            }

            findNavController().navigate(
                R.id.action_eventDetailFragment_to_eventFormFragment,
                bundle
            )
        }

        binding?.deactivateEventButton?.setOnClickListener {
            deactivateEvent(currentEventId)
        }
    }

    private fun loadEvent(eventId: String) {
        FirebaseEventRepository.getEventById(eventId) { result ->
            val currentBinding = binding ?: return@getEventById

            result
                .onSuccess { event ->
                    if (event == null) return@onSuccess

                    currentBinding.titleText.text = event.title
                    currentBinding.descriptionText.text = event.description
                    currentBinding.dateText.text = listOf(event.date, event.time).joinToString(" - ")
                    currentBinding.locationText.text = "Ubicación: ${event.location}"
                    currentBinding.organizerText.text = "Organiza: ${event.organizerName}"

                    val currentUserId = FirebaseAuthRepository.getCurrentUserId().orEmpty()
                    val isOwner = event.organizerId == currentUserId

                    currentBinding.editEventButton.visibility =
                        if (isOwner) View.VISIBLE else View.GONE

                    currentBinding.deactivateEventButton.visibility =
                        if (isOwner) View.VISIBLE else View.GONE

                    val alreadyConfirmed = event.confirmedUserIds.contains(currentUserId)
                    userHasConfirmedAttendance = alreadyConfirmed

                    currentBinding.rsvpButton.isEnabled = event.status == EventStatus.UPCOMING

                    if (alreadyConfirmed) {
                        currentBinding.rsvpButton.text = getString(R.string.cancel_attendance)
                        currentBinding.statusText.text =
                            getString(R.string.rsvp_already_confirmed_message)
                    } else {
                        currentBinding.rsvpButton.text = getString(R.string.confirm_attendance)
                        currentBinding.statusText.text = ""
                    }
                }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_events_load_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun deactivateEvent(eventId: String) {
        val currentBinding = binding ?: return
        currentBinding.deactivateEventButton.isEnabled = false

        FirebaseEventRepository.deactivateEvent(eventId) { result ->
            val activeBinding = binding ?: return@deactivateEvent
            activeBinding.deactivateEventButton.isEnabled = true

            result
                .onSuccess {
                    Toast.makeText(
                        requireContext(),
                        R.string.event_deactivated,
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack()
                }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_event_deactivate_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun confirmAttendance(eventId: String) {
        val currentBinding = binding ?: return
        currentBinding.rsvpButton.isEnabled = false

        FirebaseEventRepository.confirmAttendance(eventId) { result ->
            val activeBinding = binding ?: return@confirmAttendance

            result
                .onSuccess {
                    userHasConfirmedAttendance = true
                    activeBinding.statusText.text = getString(R.string.rsvp_confirmed_message)
                    activeBinding.rsvpButton.text = getString(R.string.cancel_attendance)
                    activeBinding.rsvpButton.isEnabled = true
                }
                .onFailure { error ->
                    if (error is AttendanceAlreadyConfirmedException) {
                        userHasConfirmedAttendance = true
                        activeBinding.statusText.text =
                            getString(R.string.rsvp_already_confirmed_message)
                        activeBinding.rsvpButton.text = getString(R.string.cancel_attendance)
                        activeBinding.rsvpButton.isEnabled = true
                    } else {
                        activeBinding.rsvpButton.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            R.string.error_rsvp_failed,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun cancelAttendance(eventId: String) {
        val currentBinding = binding ?: return
        currentBinding.rsvpButton.isEnabled = false

        FirebaseEventRepository.cancelAttendance(eventId) { result ->
            val activeBinding = binding ?: return@cancelAttendance

            result
                .onSuccess {
                    userHasConfirmedAttendance = false
                    activeBinding.statusText.text = getString(R.string.rsvp_cancelled_message)
                    activeBinding.rsvpButton.text = getString(R.string.confirm_attendance)
                    activeBinding.rsvpButton.isEnabled = true
                }
                .onFailure {
                    activeBinding.rsvpButton.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        R.string.error_rsvp_cancel_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun sendComment(eventId: String) {
        val currentBinding = binding ?: return
        val commentText = currentBinding.commentInput.text?.toString()?.trim().orEmpty()

        currentBinding.commentInputLayout.error = when {
            commentText.isBlank() -> getString(R.string.error_required)
            commentText.length < 3 -> getString(R.string.error_comment_length)
            else -> null
        }

        if (currentBinding.commentInputLayout.error != null) return

        currentBinding.sendCommentButton.isEnabled = false

        FirebaseEventRepository.addComment(eventId, commentText) { result ->
            val activeBinding = binding ?: return@addComment
            activeBinding.sendCommentButton.isEnabled = true

            result
                .onSuccess {
                    activeBinding.commentInput.text?.clear()
                    activeBinding.commentInputLayout.error = null
                    Toast.makeText(
                        requireContext(),
                        R.string.comment_created,
                        Toast.LENGTH_SHORT
                    ).show()
                    loadComments(eventId)
                }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_comment_create_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun loadComments(eventId: String) {
        FirebaseEventRepository.getComments(eventId) { result ->
            val currentBinding = binding ?: return@getComments

            result
                .onSuccess { comments ->
                    currentBinding.commentsText.text = formatComments(comments)
                }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_comments_load_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun formatComments(comments: List<EventComment>): String {
        if (comments.isEmpty()) return getString(R.string.no_comments)

        return comments.joinToString(separator = "\n\n") { comment ->
            val dateText = if (comment.createdAtMillis > 0L) {
                commentDateFormat.format(Date(comment.createdAtMillis))
            } else {
                getString(R.string.comment_time_pending)
            }

            "${comment.userName} - $dateText\n${comment.comment}"
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}