package sv.edu.udb.eventoscomunitarios.ui.event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import sv.edu.udb.eventoscomunitarios.R
import sv.edu.udb.eventoscomunitarios.data.FirebaseAuthRepository
import sv.edu.udb.eventoscomunitarios.data.FirebaseEventRepository
import sv.edu.udb.eventoscomunitarios.databinding.FragmentEventListBinding
import sv.edu.udb.eventoscomunitarios.domain.model.CommunityEvent

class EventListFragment : Fragment() {

    private var binding: FragmentEventListBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEventListBinding.inflate(inflater, container, false)
        return requireNotNull(binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.eventsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())

        loadEvents()

        binding?.createEventButton?.setOnClickListener {
            findNavController().navigate(R.id.action_eventListFragment_to_eventFormFragment)
        }

        binding?.historyButton?.setOnClickListener {
            findNavController().navigate(R.id.action_eventListFragment_to_historyFragment)
        }

        binding?.statsButton?.setOnClickListener {
            findNavController().navigate(R.id.action_eventListFragment_to_statsFragment)
        }

        binding?.logoutButton?.setOnClickListener {
            FirebaseAuthRepository.signOut()
            findNavController().navigate(R.id.action_eventListFragment_to_loginFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding != null) {
            loadEvents()
        }
    }

    private fun loadEvents() {
        FirebaseEventRepository.getUpcomingEvents { result ->
            val currentBinding = binding ?: return@getUpcomingEvents

            result
                .onSuccess { events ->
                    updateEventsView(events)
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

    private fun updateEventsView(events: List<CommunityEvent>) {
        val currentBinding = binding ?: return

        currentBinding.eventsCounterText.text = getString(
            R.string.events_counter,
            events.size
        )

        currentBinding.emptyEventsText.visibility =
            if (events.isEmpty()) View.VISIBLE else View.GONE

        currentBinding.eventsRecyclerView.visibility =
            if (events.isEmpty()) View.GONE else View.VISIBLE

        currentBinding.eventsRecyclerView.adapter = EventAdapter(events) { event ->
            findNavController().navigate(
                R.id.action_eventListFragment_to_eventDetailFragment,
                bundleOf("eventId" to event.id)
            )
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}