package musicplayer.developer.it.musify;

/**
 * Created by anupam on 21-12-2017.
 */

public interface MusicFocusable {

    void onGainedAudioFocus();

    void onLostAudioFocus(boolean canDuck);
}
