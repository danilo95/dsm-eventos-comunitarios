package sv.edu.udb.eventoscomunitarios.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.util.Calendar
import java.util.Locale
import sv.edu.udb.eventoscomunitarios.R
import sv.edu.udb.eventoscomunitarios.data.FirebaseAuthRepository
import sv.edu.udb.eventoscomunitarios.data.FirebaseEventRepository
import sv.edu.udb.eventoscomunitarios.databinding.FragmentEventFormBinding
import sv.edu.udb.eventoscomunitarios.domain.model.CommunityEvent

class EventFormFragment : Fragment() {
    private var binding: FragmentEventFormBinding? = null
    private var eventIdToEdit: String = ""
    private var eventBeingEdited: CommunityEvent? = null

    private val isEditMode: Boolean
        get() = eventIdToEdit.isNotBlank()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEventFormBinding.inflate(inflater, container, false)
        return requireNotNull(binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        eventIdToEdit = arguments?.getString("eventId").orEmpty()

        if (isEditMode) {
            loadEventForEdit(eventIdToEdit)
        }

        binding?.dateInput?.setOnClickListener {
            showDatePicker()
        }

        binding?.timeInput?.setOnClickListener {
            showTimePicker()
        }

        binding?.saveButton?.setOnClickListener {
            if (validateEventForm()) {
                if (isEditMode) {
                    updateEvent()
                } else {
                    createEvent()
                }
            }
        }
    }

    private fun loadEventForEdit(eventId: String) {
        val currentBinding = binding ?: return
        currentBinding.saveButton.isEnabled = false

        FirebaseEventRepository.getEventById(eventId) { result ->
            val activeBinding = binding ?: return@getEventById
            activeBinding.saveButton.isEnabled = true

            result
                .onSuccess { event ->
                    if (event == null) {
                        Toast.makeText(
                            requireContext(),
                            R.string.error_events_load_failed,
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().popBackStack()
                        return@onSuccess
                    }

                    eventBeingEdited = event

                    activeBinding.titleInput.setText(event.title)
                    activeBinding.descriptionInput.setText(event.description)
                    activeBinding.dateInput.setText(event.date)
                    activeBinding.timeInput.setText(event.time)
                    activeBinding.locationInput.setText(event.location)
                    activeBinding.capacityInput.setText(event.capacity.toString())
                    activeBinding.saveButton.text = getString(R.string.save)
                }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_events_load_failed,
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().popBackStack()
                }
        }
    }

    private fun createEvent() {
        val currentBinding = binding ?: return

        val organizerName = FirebaseAuthRepository.getCurrentUserEmail().ifBlank {
            getString(R.string.default_organizer)
        }

        val event = CommunityEvent(
            id = "",
            title = currentBinding.titleInput.text?.toString()?.trim().orEmpty(),
            description = currentBinding.descriptionInput.text?.toString()?.trim().orEmpty(),
            date = currentBinding.dateInput.text?.toString()?.trim().orEmpty(),
            time = currentBinding.timeInput.text?.toString()?.trim().orEmpty(),
            location = currentBinding.locationInput.text?.toString()?.trim().orEmpty(),
            organizerName = organizerName,
            organizerId = FirebaseAuthRepository.getCurrentUserId().orEmpty(),
            capacity = currentBinding.capacityInput.text?.toString()?.trim()?.toIntOrNull() ?: 0,
            confirmedCount = 0
        )

        currentBinding.saveButton.isEnabled = false

        FirebaseEventRepository.createEvent(event) { result ->
            val activeBinding = binding ?: return@createEvent
            activeBinding.saveButton.isEnabled = true

            result
                .onSuccess {
                    Toast.makeText(
                        requireContext(),
                        R.string.event_created,
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack()
                }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_event_create_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun updateEvent() {
        val currentBinding = binding ?: return
        val originalEvent = eventBeingEdited ?: return

        val updatedEvent = originalEvent.copy(
            title = currentBinding.titleInput.text?.toString()?.trim().orEmpty(),
            description = currentBinding.descriptionInput.text?.toString()?.trim().orEmpty(),
            date = currentBinding.dateInput.text?.toString()?.trim().orEmpty(),
            time = currentBinding.timeInput.text?.toString()?.trim().orEmpty(),
            location = currentBinding.locationInput.text?.toString()?.trim().orEmpty(),
            capacity = currentBinding.capacityInput.text?.toString()?.trim()?.toIntOrNull() ?: 0
        )

        currentBinding.saveButton.isEnabled = false

        FirebaseEventRepository.updateEvent(updatedEvent) { result ->
            val activeBinding = binding ?: return@updateEvent
            activeBinding.saveButton.isEnabled = true

            result
                .onSuccess {
                    Toast.makeText(
                        requireContext(),
                        R.string.event_updated,
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack()
                }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_event_update_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                binding?.dateInput?.setText(
                    String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                )
                binding?.dateInputLayout?.error = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                binding?.timeInput?.setText(
                    String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                )
                binding?.timeInputLayout?.error = null
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun validateEventForm(): Boolean {
        val currentBinding = binding ?: return false

        val title = currentBinding.titleInput.text?.toString()?.trim().orEmpty()
        val description = currentBinding.descriptionInput.text?.toString()?.trim().orEmpty()
        val date = currentBinding.dateInput.text?.toString()?.trim().orEmpty()
        val time = currentBinding.timeInput.text?.toString()?.trim().orEmpty()
        val location = currentBinding.locationInput.text?.toString()?.trim().orEmpty()
        val capacity = currentBinding.capacityInput.text?.toString()?.trim().orEmpty()

        currentBinding.titleInputLayout.error = when {
            title.isBlank() -> getString(R.string.error_required)
            title.length < 3 -> getString(R.string.error_title_length)
            else -> null
        }

        currentBinding.descriptionInputLayout.error = when {
            description.isBlank() -> getString(R.string.error_required)
            description.length < 10 -> getString(R.string.error_description_length)
            else -> null
        }

        currentBinding.dateInputLayout.error = when {
            date.isBlank() -> getString(R.string.error_required)
            !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> getString(R.string.error_date_format)
            else -> null
        }

        currentBinding.timeInputLayout.error = when {
            time.isBlank() -> getString(R.string.error_required)
            !time.matches(Regex("([01]\\d|2[0-3]):[0-5]\\d")) -> getString(R.string.error_time_format)
            else -> null
        }

        currentBinding.locationInputLayout.error = when {
            location.isBlank() -> getString(R.string.error_required)
            else -> null
        }

        val capacityValue = capacity.toIntOrNull()

        currentBinding.capacityInputLayout.error = when {
            capacity.isBlank() -> getString(R.string.error_required)
            capacityValue == null || capacityValue <= 0 -> getString(R.string.error_capacity_invalid)
            else -> null
        }

        return listOf(
            currentBinding.titleInputLayout,
            currentBinding.descriptionInputLayout,
            currentBinding.dateInputLayout,
            currentBinding.timeInputLayout,
            currentBinding.locationInputLayout,
            currentBinding.capacityInputLayout
        ).all { it.error == null }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}