package de.dennisguse.opentracks.p0.recording;

/** UI/session intent; the mature recording service owns actual sensor/data lifetime. */
public record RecordingSession(Phase phase, Long trackId) {
    public enum Phase { READY, RECORDING, PAUSED, FINISHED }
    public static RecordingSession ready() { return new RecordingSession(Phase.READY,null); }
    public RecordingSession begin(long id) {
        if(id<=0||phase==Phase.RECORDING||phase==Phase.PAUSED)throw new IllegalStateException("Recording already active or invalid ID");
        return new RecordingSession(Phase.RECORDING,id);
    }
    public RecordingSession pause() { require(Phase.RECORDING);return new RecordingSession(Phase.PAUSED,trackId); }
    public RecordingSession resume() { require(Phase.PAUSED);return new RecordingSession(Phase.RECORDING,trackId); }
    public RecordingSession finish() {
        if(phase!=Phase.RECORDING&&phase!=Phase.PAUSED)throw new IllegalStateException("No active workout");
        return new RecordingSession(Phase.FINISHED,trackId);
    }
    public static RecordingSession recover(long id,boolean serviceRecording) {
        if(id<=0)return ready();
        return new RecordingSession(serviceRecording?Phase.RECORDING:Phase.PAUSED,id);
    }
    private void require(Phase expected) { if(phase!=expected)throw new IllegalStateException("Invalid recording transition"); }
}
