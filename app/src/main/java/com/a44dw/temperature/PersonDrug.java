package com.a44dw.temperature;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = {
        @ForeignKey(entity = Temperature.class, parentColumns = "tempId", childColumns = "temperatureId", onDelete = CASCADE),
        @ForeignKey(entity = Drug.class, parentColumns = "drugId", childColumns = "drugId")
})
public class PersonDrug {
    @PrimaryKey(autoGenerate = true)
    public long personDrugId;
    public long temperatureId;
    public long drugId;
    public float amount;
    public String drugUnit;
}