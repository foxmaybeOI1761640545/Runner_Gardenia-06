package de.dennisguse.opentracks.sensors;

import de.dennisguse.opentracks.data.models.HeartRate;

/** In-memory preview only; the recording guard controls database writes. */
public record HeartRatePreview(HeartRate value, HeartRateStatus status) {}
