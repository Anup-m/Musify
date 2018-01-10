package musicplayer.developer.it.musify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by anupam on 26-12-2017.
 */

public class NotificationReciever extends BroadcastReceiver {
    SongInfo songInfo;
    @Override
    public void onReceive(Context context, Intent intent) {

        switch (intent.getAction()) {
            case NotificationGenerator.NOTIFY_PAUSE:
                if(SongsListActivity.mediaPlayer.isPlaying()) {
                    SongsListActivity.mediaPlayer.pause();
                    PlayerActivity.isPlaying = false;
                    SongsListActivity.playedLength = SongsListActivity.mediaPlayer.getCurrentPosition();
                }
                else {
                    SongsListActivity.mediaPlayer.seekTo(SongsListActivity.playedLength);
                    SongsListActivity.mediaPlayer.start();
                    PlayerActivity.isPlaying = true;
                }

                SongsListActivity.checkIfUiNeedsToUpdate = true;
                PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                Utility.recallNotificationBuilder(context);
                break;

            case NotificationGenerator.NOTIFY_NEXT:
                SongsListActivity.playNext();
                songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
                SongsListActivity.playSong(songInfo,SongsListActivity.currSongPosition,context);
                PlayerActivity.isPlaying = true;
                PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                SongsListActivity.checkIfUiNeedsToUpdate = true;
                //Utility.recallNotificationBuilder(context);
                break;

            case NotificationGenerator.NOTIFY_PREVIOUS:
                SongsListActivity.playPrevious();
                songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
                SongsListActivity.playSong(songInfo,SongsListActivity.currSongPosition,context);
                PlayerActivity.isPlaying = true;
                PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                SongsListActivity.checkIfUiNeedsToUpdate = true;
                //Utility.recallNotificationBuilder(context);
                break;
            case NotificationGenerator.NOTIFY_DUCK_UP:
                NotificationGenerator.isSimpleView = true;
                Utility.recallNotificationBuilder(context);
                break;
            case NotificationGenerator.NOTIFY_DUCK_DOWN:
                NotificationGenerator.isSimpleView = false;
                Utility.recallNotificationBuilder(context);
                break;
        }
    }
}
