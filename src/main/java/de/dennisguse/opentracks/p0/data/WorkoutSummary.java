package de.dennisguse.opentracks.p0.data;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.p0.statistics.HeartRateSummary;

/** Read-only projection over the existing normalized recording database. */
public record WorkoutSummary(Track track, HeartRateSummary heartRate) {}
