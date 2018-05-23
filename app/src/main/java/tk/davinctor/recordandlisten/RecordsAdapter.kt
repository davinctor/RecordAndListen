package tk.davinctor.recordandlisten

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * @author Victor Ponomarenko
 */
class RecordsAdapter(context: Context,
                     private val records: List<RecordEntity>,
                     private val onItemClickListener: (Int, Boolean) -> Unit)
    : RecyclerView.Adapter<RecordsAdapter.ViewHolder>() {

    private var mediaInProgress: Pair<MediaState, Int>? = null
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_record, parent, false)
        return ViewHolder(itemView, onItemClickListener)
    }

    override fun getItemCount(): Int = records.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
        if (position == mediaInProgress?.second) {
            holder.bind(mediaInProgress!!.first)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach {
                if (it is MediaState) {
                    holder.bind(it)
                }
                if (it is MediaState.Playing) {
                    mediaInProgress = Pair<MediaState, Int>(it, position)
                }
            }
        }
    }

    sealed class MediaState {
        data class Playing(val progressEmitter: Observable<Int>) : MediaState()
        object Stopped : MediaState()
    }

    class ViewHolder(itemView: View,
                     private val onItemClickListener: (Int, Boolean) -> Unit)
        : RecyclerView.ViewHolder(itemView) {

        private val mediaButton: ImageButton = itemView.findViewById(R.id.media_button)
        private val durationProgressBar: ProgressBar = itemView.findViewById(R.id.media_duration_pb)
        private val durationTextView: TextView = itemView.findViewById(R.id.media_duration_tv)

        private var countdownDisposable: Disposable? = null

        init {
            mediaButton.setOnClickListener {
                onItemClickListener.invoke(adapterPosition, mediaButton.isActivated)
            }
        }

        fun bind(item: RecordEntity) {
            val durationInSeconds = TimeUnit.MILLISECONDS.toSeconds(item.duration)
            durationTextView.text = "%02d:%02d".format(durationInSeconds / 60, durationInSeconds % 60)
        }

        fun bind(mediaState: MediaState) {
            when (mediaState) {
                is MediaState.Playing -> {
                    mediaButton.isActivated = true
                    countdownDisposable = mediaState.progressEmitter
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext {
                                durationProgressBar.progress = it
                            }
                            .subscribe()
                }
                is MediaState.Stopped -> {
                    mediaButton.isActivated = false
                    durationProgressBar.progress = 0
                    countdownDisposable?.dispose()
                }
            }
        }

    }

}