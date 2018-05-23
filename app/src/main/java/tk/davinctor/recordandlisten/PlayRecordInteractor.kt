package tk.davinctor.recordandlisten

import android.media.MediaPlayer
import android.util.Log
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.Callable


/**
 * @author Victor Ponomarenko
 */
class PlayRecordInteractor {

    private val stopSubject = PublishSubject.create<RecordsViewState>()
    private var mediaPlayer: MediaPlayer? = null

    fun startPlay(recordEntity: RecordEntity): Observable<RecordsViewState> = Observable.fromCallable {
        releasePlayer()
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setDataSource(recordEntity.uri)
        mediaPlayer!!.prepare()
        mediaPlayer!!.start()
        mediaPlayer!!.setOnCompletionListener {
            releasePlayer()
            stopSubject.onNext(RecordsViewState.PlayEnded(recordEntity))
        }
        true
    }
            .switchMap { stopSubject }
            .startWith(RecordsViewState.PlayStarted(recordEntity))
            .onErrorResumeNext(Function { error -> Observable.just(RecordsViewState.PlayError(recordEntity, error)) })

    fun stopPlay(recordEntity: RecordEntity): Observable<RecordsViewState> = Observable.fromCallable(Callable<RecordsViewState> {
        releasePlayer()
        RecordsViewState.PlayEnded(recordEntity)
    })
            .onErrorResumeNext(Function { error -> Observable.just(RecordsViewState.PlayError(recordEntity, error)) })

    private fun releasePlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer?.setOnCompletionListener(null)
        mediaPlayer = null
    }

}