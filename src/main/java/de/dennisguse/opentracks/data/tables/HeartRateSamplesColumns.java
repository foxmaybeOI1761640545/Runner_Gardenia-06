package de.dennisguse.opentracks.data.tables;

/** Raw notification history in the existing recording database; TrackPoint BPM remains compatible. */
public interface HeartRateSamplesColumns {
    String TABLE_NAME = "heart_rate_samples";
    String CREATE_TABLE = "CREATE TABLE heart_rate_samples (_id INTEGER PRIMARY KEY AUTOINCREMENT, trackid INTEGER NOT NULL, time INTEGER NOT NULL, sample_key TEXT NOT NULL, bpm FLOAT NOT NULL CHECK(bpm > 0 AND bpm <= 65535), metadata TEXT NOT NULL, UNIQUE(trackid, sample_key), FOREIGN KEY (trackid) REFERENCES tracks(_id) ON UPDATE CASCADE ON DELETE CASCADE)";
    String CREATE_INDEX = "CREATE INDEX heart_rate_samples_trackid_time_index ON heart_rate_samples(trackid, time, _id)";
}
