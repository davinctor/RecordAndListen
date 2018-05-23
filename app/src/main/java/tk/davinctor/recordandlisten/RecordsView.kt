package tk.davinctor.recordandlisten

import com.hannesdorfmann.mosby3.mvp.MvpView
import io.reactivex.Observable

/**
 * @author Victor Ponomarenko
 */
interface RecordsView : MvpView {

    fun loadRecords() : Observable<Boolean>

    fun startRecord(): Observable<Boolean>

    fun stopRecord() : Observable<Boolean>

    fun startPlay() : Observable<RecordEntity>

    fun stopPlay(): Observable<RecordEntity>

    fun render(viewState: RecordsViewState)

}