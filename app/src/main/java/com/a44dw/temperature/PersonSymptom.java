package com.a44dw.temperature;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = {
        @ForeignKey(entity = Temperature.class, parentColumns = "tempId", childColumns = "temperatureId", onDelete = CASCADE),
        @ForeignKey(entity = Symptom.class, parentColumns = "symptId", childColumns = "symptomId")
})
public class PersonSymptom {
    @PrimaryKey(autoGenerate = true)
    public long personSymptId;
    public long temperatureId;
    public long symptomId;
}
