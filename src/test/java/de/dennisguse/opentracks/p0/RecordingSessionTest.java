package de.dennisguse.opentracks.p0;
import static org.junit.Assert.*;
import org.junit.Test;
import de.dennisguse.opentracks.p0.recording.RecordingSession;

public class RecordingSessionTest {
    @Test public void pauseAndResumePreserveWorkoutIdentity() {
        var session=RecordingSession.ready().begin(42).pause();assertEquals(RecordingSession.Phase.PAUSED,session.phase());
        assertEquals(Long.valueOf(42),session.resume().trackId());assertEquals(RecordingSession.Phase.FINISHED,session.resume().finish().phase());
    }
    @Test public void finishedWorkoutCanStartANewIdentity() {
        assertEquals(Long.valueOf(43),RecordingSession.ready().begin(42).finish().begin(43).trackId());
    }
    @Test public void pausedWorkoutCanFinishWithoutResumingSensors() {
        assertEquals(RecordingSession.Phase.FINISHED,RecordingSession.ready().begin(42).pause().finish().phase());
    }
    @Test public void invalidTransitionsAreRejected() {
        for(Runnable action:new Runnable[]{()->RecordingSession.ready().pause(),()->RecordingSession.ready().resume(),()->RecordingSession.ready().finish(),()->RecordingSession.ready().begin(-1),()->RecordingSession.ready().begin(42).begin(43)}) {
            try {action.run();fail();}catch(IllegalStateException expected){}
        }
    }
    @Test public void processRestoreRequiresServiceEvidenceBeforeRecording() {
        assertEquals(RecordingSession.Phase.PAUSED,RecordingSession.recover(42,false).phase());
        assertEquals(RecordingSession.Phase.RECORDING,RecordingSession.recover(42,true).phase());
    }
}
