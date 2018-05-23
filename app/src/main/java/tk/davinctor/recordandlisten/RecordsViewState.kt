package tk.davinctor.recordandlisten

/**
 * @author Victor Ponomarenko
 */
sealed class RecordsViewState {

    object Loading : RecordsViewState()

    object EmptyResult : RecordsViewState()

    data class Result(val records: List<RecordEntity>) : RecordsViewState()

    data class Error(val error: Throwable) : RecordsViewState()

    object StartRecord : RecordsViewState()

    data class RecordFailed(val error: Throwable) : RecordsViewState()

    data class EndRecord(val recordEntity: RecordEntity) : RecordsViewState()

    data class PlayStarted(val recordEntity: RecordEntity) : RecordsViewState()

    data class PlayError(val recordEntity: RecordEntity,
                         val error: Throwable) : RecordsViewState()

    data class PlayEnded(val recordEntity: RecordEntity) : RecordsViewState()

}
