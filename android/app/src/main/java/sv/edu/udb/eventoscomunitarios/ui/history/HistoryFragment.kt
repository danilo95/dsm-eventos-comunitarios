package sv.edu.udb.eventoscomunitarios.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import sv.edu.udb.eventoscomunitarios.R
import sv.edu.udb.eventoscomunitarios.data.FirebaseEventRepository
import sv.edu.udb.eventoscomunitarios.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {
    private var binding: FragmentHistoryBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return requireNotNull(binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        FirebaseEventRepository.getPastEvents { result ->
            val currentBinding = binding ?: return@getPastEvents
            result
                .onSuccess { history ->
                    currentBinding.historyText.text = if (history.isEmpty()) {
                        getString(R.string.empty_history)
                    } else {
                        history.joinToString(separator = "\n\n") {
                            "${it.title}\n${it.date} - ${it.location}"
                        }
                    }
                }
                .onFailure {
                    Toast.makeText(requireContext(), R.string.error_events_load_failed, Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
