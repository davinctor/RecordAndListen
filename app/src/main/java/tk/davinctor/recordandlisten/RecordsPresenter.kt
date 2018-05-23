package tk.davinctor.recordandlisten

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * @author Victor Ponomarenko
 */
class RecordsPresenter(private val loadRecordsInteractor: LoadRecordsInteractor,
                       private val recordAudioInteractor: RecordAudioInteractor,
                       private val playRecordInteractor: PlayRecordInteractor)
    : MviBasePresenter<RecordsView, RecordsViewState>() {

    override fun bindIntents() {
        val loadRecords = intent(RecordsView::loadRecords)
                .switchMap { loadRecordsInteractor.records() }
                .observeOn(AndroidSchedulers.mainThread())

        val startRecordIntent = intent(RecordsView::startRecord)
                .switchMap { recordAudioInteractor.startRecord() }
                .observeOn(AndroidSchedulers.mainThread())
        val stopRecordIntent = intent(RecordsView::stopRecord)
                .switchMap { recordAudioInteractor.stopRecord() }
                .observeOn(AndroidSchedulers.mainThread())

        val startPlayRecordIntent = intent(RecordsView::startPlay)
                .switchMap { playRecordInteractor.startPlay(it) }
                .observeOn(AndroidSchedulers.mainThread())
        val stopPlayRecordIntent = intent(RecordsView::stopPlay)
                .switchMap { playRecordInteractor.stopPlay(it) }
                .observeOn(AndroidSchedulers.mainThread())

        val allIntents = Observable.merge(
                loadRecords,
                Observable.merge(startRecordIntent, stopRecordIntent),
                Observable.merge(startPlayRecordIntent, stopPlayRecordIntent))

        subscribeViewState(allIntents, RecordsView::render)
    }

}