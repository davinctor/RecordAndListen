package tk.davinctor.recordandlisten

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.hannesdorfmann.mosby3.mvi.MviActivity
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_records.*
import java.util.concurrent.TimeUnit

/**
 * @author Victor Ponomarenko
 */
class RecordsActivity : MviActivity<RecordsView, RecordsPresenter>(), RecordsView {

    val logTag: String = "RecordsActivity"

    private val requestRecordAudioPermission = 12
    private val audioRecordPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private val records = ArrayList<RecordEntity>()
    private val startPlayRecordSubject = PublishSubject.create<RecordEntity>()
    private val stopPlayRecordSubject = PublishSubject.create<RecordEntity>()
    private var permissionToRecordAccepted = false
    private var countdownDisposable: Disposable? = null
    private lateinit var recordFabMotionObservable: Observable<MotionEvent>
    private lateinit var recordsAdapter: RecordsAdapter
    private var lastPlayedRecordPosition = -1
    private var progressEmitterSubject : PublishSubject<Int>? = null
    private var progressDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)
        recordsAdapter = RecordsAdapter(this, records, { position, isActivated ->
            when (isActivated) {
                true -> stopPlayRecordSubject.onNext(records[position])
                false -> startPlayRecordSubject.onNext(records[position])
            }
        })
        records_rv.adapter = recordsAdapter
        records_rv.layoutManager = LinearLayoutManager(this)
        recordFabMotionObservable = RxView.touches(record_fab).share()

        permissionToRecordAccepted = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStart() {
        super.onStart()
        progressEmitterSubject = PublishSubject.create()
    }

    override fun onStop() {
        countdownDisposable?.dispose()
        progressDisposable?.dispose()
        progressEmitterSubject?.onComplete()
        if (lastPlayedRecordPosition >=0) {
            stopPlayRecordSubject.onNext(records[lastPlayedRecordPosition])
        }
        super.onStop()
    }

    override fun loadRecords(): Observable<Boolean> {
        return Observable.just(true)
    }

    override fun startRecord(): Observable<Boolean> = recordFabMotionObservable
            .filter { it.actionMasked == MotionEvent.ACTION_DOWN }
            .doOnNext {
                if (!permissionToRecordAccepted) {
                    ActivityCompat.requestPermissions(this, audioRecordPermissions, requestRecordAudioPermission)
                }
                if (lastPlayedRecordPosition >= 0) {
                    stopPlayRecordSubject.onNext(records[lastPlayedRecordPosition])
                }
            }
            .filter { permissionToRecordAccepted }
            .doOnNext { Log.d(logTag, "Start record") }
            .map { true }

    override fun stopRecord(): Observable<Boolean> = recordFabMotionObservable
            .filter { it.actionMasked == MotionEvent.ACTION_UP && permissionToRecordAccepted }
            .doOnNext { Log.d(logTag, "Stop record") }
            .map { false }

    override fun startPlay(): Observable<RecordEntity> = startPlayRecordSubject
            .doOnNext { Log.d(logTag, "Start play") }

    override fun stopPlay(): Observable<RecordEntity> = stopPlayRecordSubject
            .doOnNext { Log.d(logTag, "Stop play") }

    override fun render(viewState: RecordsViewState) {
        Log.d(logTag, viewState.javaClass.name)
        when (viewState) {

            is RecordsViewState.Result -> renderRecordsList(viewState)
            is RecordsViewState.Error -> renderLoadRecordsError(viewState)

            is RecordsViewState.StartRecord -> renderStartRecord()
            is RecordsViewState.RecordFailed -> renderRecordFailed(viewState)
            is RecordsViewState.EndRecord -> renderEndRecord(viewState)

            is RecordsViewState.PlayStarted -> renderStartPlay(viewState)
            is RecordsViewState.PlayError -> renderPlayError(viewState)
            is RecordsViewState.PlayEnded -> renderStopPlay()
        }
    }

    private fun renderLoadRecordsError(viewState: RecordsViewState.Error) {
        if (viewState.error.message.isNullOrEmpty()) {
            showError("Failed to load records list")
        } else{
            showError(viewState.error.message!!)
        }
    }

    private fun renderPlayError(viewState: RecordsViewState.PlayError) {
        renderStopPlay()
        showError(viewState.error.localizedMessage)
    }

    private fun renderStopPlay() {
        progressDisposable?.dispose()
        if (lastPlayedRecordPosition >= 0) {
            recordsAdapter.notifyItemChanged(lastPlayedRecordPosition, RecordsAdapter.MediaState.Stopped)
        }
        lastPlayedRecordPosition = RecyclerView.NO_POSITION
    }

    private fun renderStartPlay(viewState: RecordsViewState.PlayStarted) {
        val newLastPlayedRecordPosition = records.indexOf(viewState.recordEntity)
        if (newLastPlayedRecordPosition != lastPlayedRecordPosition) {
            renderStopPlay()
        }
        if (newLastPlayedRecordPosition < 0) {
            showError("Failed to find record")
            return
        }
        lastPlayedRecordPosition = newLastPlayedRecordPosition
        recordsAdapter.notifyItemChanged(lastPlayedRecordPosition, RecordsAdapter.MediaState.Playing(progressEmitterSubject!!))
        progressDisposable = Observable.zip(
                Observable.interval(viewState.recordEntity.duration / 100, TimeUnit.MILLISECONDS),
                Observable.range(0, 100),
                BiFunction<Long, Int, Int> { _, rangeValue -> rangeValue })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    progressEmitterSubject!!.onNext(it)
                }
                .subscribe()
    }

    private fun renderRecordsList(viewState: RecordsViewState.Result) {
        var size = records.size
        records.clear()
        recordsAdapter.notifyItemRangeRemoved(0, size)
        records.addAll(viewState.records)
        size = records.size
        recordsAdapter.notifyItemRangeInserted(0, size)
    }

    private fun renderEndRecord(viewState: RecordsViewState.EndRecord) {
        countdownDisposable?.dispose()
        record_time_tv.visibility = View.INVISIBLE
        records.add(viewState.recordEntity)
        val newRecordPosition = records.size - 1
        recordsAdapter.notifyItemInserted(newRecordPosition)
        records_rv.smoothScrollToPosition(newRecordPosition)
    }

    private fun renderRecordFailed(viewState: RecordsViewState.RecordFailed) {
        countdownDisposable?.dispose()
        record_time_tv.visibility = View.INVISIBLE
        showError(viewState.error.localizedMessage)
    }

    @SuppressLint("SetTextI18n")
    private fun renderStartRecord() {
        record_time_tv.visibility = View.VISIBLE
        record_time_tv.text = "00:00"
        countdownDisposable = Observable.interval(1L, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    record_time_tv.text = "%02d:%02d".format(it / 60, it % 60)
                }
                .subscribe()
    }

    private fun showError(errorMessage: String) = Snackbar.make(records_rv, errorMessage, Snackbar.LENGTH_LONG).show()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            requestRecordAudioPermission -> {
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    override fun createPresenter(): RecordsPresenter {
        // as this just a test sample with single screen/presenter/interactor
        // so I decide to leave as this
        return RecordsPresenter(LoadRecordsInteractor(this),
                RecordAudioInteractor(this),
                PlayRecordInteractor())
    }

}
