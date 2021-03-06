package tk.davinctor.recordandlisten

import android.content.Context
import android.util.Log
import io.reactivex.Observable
import java.io.File

/**
 * @author Victor Ponomarenko
 */
class LoadRecordsInteractor(context: Context) {

    private val logTag = "LoadRecordsInteractor"
    private val appContext = context.applicationContext

    fun records(): Observable<RecordsViewState> = Observable
            .fromCallable {
                val dir: File? = appContext.externalCacheDir.absoluteFile
                if (dir == null) {
                    emptyArray<File>()
                }
                dir!!.listFiles()
            }
            .map { t ->
                val records = ArrayList<RecordEntity>()
                t.forEach {
                    try {
                        records.add(RecordEntity(it.absolutePath, it.getMediaDuration(appContext)))
                    } catch (error: Throwable) {
                        Log.e(logTag, "Failed to get media duration: " + it.absolutePath)
                    }
                }
                records
            }
            .map({
                if (it.isEmpty()) {
                    RecordsViewState.EmptyResult
                } else {
                    RecordsViewState.Result(it)
                }
            })
            .startWith(RecordsViewState.Loading)
            .onErrorReturn { error -> RecordsViewState.Error(error) }

}