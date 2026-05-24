package sv.edu.udb.eventoscomunitarios.ui.event

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import sv.edu.udb.eventoscomunitarios.databinding.ItemEventBinding
import sv.edu.udb.eventoscomunitarios.domain.model.CommunityEvent

class EventAdapter(
    private val events: List<CommunityEvent>,
    private val onEventClick: (CommunityEvent) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return EventViewHolder(ItemEventBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: CommunityEvent) {
            binding.titleText.text = event.title
            binding.dateText.text = "${event.date} - ${event.time}"
            binding.locationText.text = event.location
            binding.participantsText.text = "${event.confirmedCount}/${event.capacity} confirmados"
            binding.root.setOnClickListener { onEventClick(event) }
        }
    }
}
