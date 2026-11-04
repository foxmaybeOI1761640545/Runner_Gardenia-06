package de.dennisguse.opentracks.p0.data;

import android.content.Context;
import android.content.SharedPreferences;
import de.dennisguse.opentracks.p0.recording.RecordingSession;

/** Small session-intent journal. Workout/sensor data remains in the original SQLite database. */
public final class SessionStore {
    private final SharedPreferences preferences;
    public SessionStore(Context context) { preferences=context.getSharedPreferences("p0_session",Context.MODE_PRIVATE); }
    public RecordingSession recover() {
        // A stopped process cannot prove it is recording; service status is authoritative after binding.
        return RecordingSession.recover(preferences.getLong("track",-1),false);
    }
    public void save(RecordingSession session) {
        SharedPreferences.Editor editor=preferences.edit();
        if(session.phase()==RecordingSession.Phase.RECORDING||session.phase()==RecordingSession.Phase.PAUSED)
            editor.putLong("track",session.trackId());
        else editor.remove("track");
        if(!editor.commit())throw new IllegalStateException("Could not save session intent");
    }
}
