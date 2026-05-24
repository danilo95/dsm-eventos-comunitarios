package sv.edu.udb.eventoscomunitarios.ui.event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import sv.edu.udb.eventoscomunitarios.R
import sv.edu.udb.eventoscomunitarios.data.AttendanceAlreadyConfirmedException
import sv.edu.udb.eventoscomunitarios.data.FirebaseAuthRepository
import sv.edu.udb.eventoscomunitarios.data.FirebaseEventRepository
import sv.edu.udb.eventoscomunitarios.databinding.FragmentEventDetailBinding
import sv.edu.udb.eventoscomunitarios.domain.model.EventComment

class EventDetailFragment : Fragment() {
    private var binding: FragmentEventDetailBinding? = null
    private val commentDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return requireNotNull(binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val eventId = arguments?.getString("eventId").orEmpty()
        loadEvent(eventId)
        loadComments(eventId)

        binding?.rsvpButton?.setOnClickListener {
            confirmAttendance(eventId)
        }
        binding?.sendCommentButton?.setOnClickListener {
            sendComment(eventId)
        }
    }

    private fun loadEvent(eventId: String) {
        FirebaseEventRepository.getEventById(eventId) { result ->
            val currentBinding = binding ?: return@getEventById
            result
                .onSuccess { event ->
                    currentBinding.titleText.text = event?.title.orEmpty()
                    currentBinding.descriptionText.text = event?.description.orEmpty()
                    currentBinding.dateText.text = listOfNotNull(event?.date, event?.time).joinToString(" - ")
                    currentBinding.locationText.text = event?.location?.let { "Ubicación: $it" }.orEmpty()
                    currentBinding.organizerText.text = event?.organizerName?.let { "Organiza: $it" }.orEmpty()

                    val alreadyConfirmed = event?.confirmedUserIds?.contains(
                        FirebaseAuthRepository.getCurrentUserId()
                    ) == true
                    currentBinding.rsvpButton.isEnabled = !alreadyConfirmed
                    if (alreadyConfirmed) {
                        currentBinding.statusText.text = getString(R.string.rsvp_already_confirmed_message)
                    }
                }
                .onFailure {
                    Toast.makeText(requireContext(), R.string.error_events_load_failed, Toast.LENGTH_LONG).show()
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
                    activeBinding.statusText.text = getString(R.string.rsvp_confirmed_message)
                    activeBinding.rsvpButton.isEnabled = false
                }
                .onFailure { error ->
                    if (error is AttendanceAlreadyConfirmedException) {
                        activeBinding.statusText.text = getString(R.string.rsvp_already_confirmed_message)
                        activeBinding.rsvpButton.isEnabled = false
                    } else {
                        activeBinding.rsvpButton.isEnabled = true
                        Toast.makeText(requireContext(), R.string.error_rsvp_failed, Toast.LENGTH_LONG).show()
                    }
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
                    Toast.makeText(requireContext(), R.string.comment_created, Toast.LENGTH_SHORT).show()
                    loadComments(eventId)
                }
                .onFailure {
                    Toast.makeText(requireContext(), R.string.error_comment_create_failed, Toast.LENGTH_LONG).show()
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
                    Toast.makeText(requireContext(), R.string.error_comments_load_failed, Toast.LENGTH_LONG).show()
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
