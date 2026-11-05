# PreferenceInflater uses the class name and (Context, AttributeSet) constructor from XML.
-keep,allowoptimization class de.dennisguse.opentracks.p0.ble.P0HeartRatePreference {
    public <init>(android.content.Context, android.util.AttributeSet);
}
# AndroidViewModel factories and Fragment recreation use these constructors reflectively.
-keepclassmembers,allowoptimization class de.dennisguse.opentracks.p0.** extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
-keepclassmembers,allowoptimization class de.dennisguse.opentracks.p0.app.P0SensorsFragment {
    public <init>();
}
