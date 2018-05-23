package tk.davinctor.recordandlisten

import android.content.Context
import android.media.MediaRecorder
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.functions.Function
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable

/**
 * @author Victor Ponomarenko
 */
class RecordAudioInteractor(context: Context) {

    private val logTag = "RecordAudioIntercator"
    private val appContext = context.applicationContext
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.getDefault())
    private var recordFilePath: String? = null
    private var recorder: MediaRecorder? = null

    fun startRecord(): Observable<RecordsViewState> = Completable
            .fromAction {
                recorder = MediaRecorder()
                recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder!!.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                recordFilePath = "${appContext.externalCacheDir!!.absolutePath!!}/${fileNameFormat.format(Date())}.aac"
                recorder!!.setOutputFile(recordFilePath)
                recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder!!.prepare()
                recorder!!.start()
            }
            .toObservable<RecordsViewState>()
            .startWith(RecordsViewState.StartRecord)
            .onErrorResumeNext(Function<Throwable, Observable<RecordsViewState>> {
                Observable.just(handleFailedRecord(it))
            })

    fun stopRecord(): Observable<RecordsViewState> = Observable
            .fromCallable(Callable<RecordsViewState> {
                recorder?.stop()
                recorder?.release()
                recorder = null
                val mediaFile = File(recordFilePath)
                RecordsViewState.EndRecord(RecordEntity(recordFilePath!!, mediaFile.getMediaDuration(appContext)))
            })
            .onErrorResumeNext(Function<Throwable, Observable<RecordsViewState>> {
                Observable.just(handleFailedRecord(it))
            })

    private fun handleFailedRecord(error: Throwable): RecordsViewState {
        val fileWithBrokenMedia = File(recordFilePath!!)
        fileWithBrokenMedia.deleteOnExit()
        return if (error.message.isNullOrEmpty()) {
            RecordsViewState.RecordFailed(Exception("Record failed", error))
        } else {
            RecordsViewState.RecordFailed(error)
        }
    }

}