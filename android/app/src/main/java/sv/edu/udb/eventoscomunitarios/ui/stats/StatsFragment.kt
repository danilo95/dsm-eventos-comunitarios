package sv.edu.udb.eventoscomunitarios.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import sv.edu.udb.eventoscomunitarios.R
import sv.edu.udb.eventoscomunitarios.data.FirebaseEventRepository
import sv.edu.udb.eventoscomunitarios.databinding.FragmentStatsBinding

class StatsFragment : Fragment() {
    private var binding: FragmentStatsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatsBinding.inflate(inflater, container, false)
        return requireNotNull(binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        FirebaseEventRepository.getStats { result ->
            val currentBinding = binding ?: return@getStats
            result
                .onSuccess { stats ->
                    currentBinding.statsText.text = """
                        Eventos registrados: ${stats.totalEvents}
                        Próximos eventos: ${stats.upcomingEvents}
                        Eventos pasados: ${stats.pastEvents}
                        Participaciones confirmadas: ${stats.confirmedParticipations}
                    """.trimIndent()
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
