package tk.davinctor.recordandlisten

import android.content.Context
import android.util.Log
import io.reactivex.Flowable
import io.reactivex.Observable
import java.io.File

/**
 * @author Victor Ponomarenko
 */
class LoadRecordsInteractor(context: Context) {

    private val logTag = "LoadRecordsInteractor"
    private val appContext = context.applicationContext

    fun records(): Observable<RecordsViewState> = Observable.fromCallable {
        val dir: File? = appContext.externalCacheDir.absoluteFile
        if (dir == null) {
            emptyArray<File>()
        }
        dir!!.listFiles()
    }
            .switchMap { t ->
                Flowable.fromArray(*t)
                        .map {
                            RecordEntity(it.absolutePath, it.getMediaDuration(appContext))
                        }
                        .toList()
                        .toObservable()
            }
            .map({
                if (it.isEmpty()) {
                    RecordsViewState.EmptyResult
                } else {
                    RecordsViewState.Result(it)
                }
            })
            .doOnNext { Log.d(logTag, "records()") }
            .startWith(RecordsViewState.Loading)
            .onErrorReturn { error -> RecordsViewState.Error(error) }


}