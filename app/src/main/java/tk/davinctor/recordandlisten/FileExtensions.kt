package tk.davinctor.recordandlisten

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

/**
 * @author Victor Ponomarenko
 */
fun File.getMediaDuration(context: Context): Long {
    val uri = Uri.parse(absolutePath)
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(context.applicationContext, uri)
    val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    return Integer.parseInt(durationStr).toLong()
}