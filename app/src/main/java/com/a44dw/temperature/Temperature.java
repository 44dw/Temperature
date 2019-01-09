package com.a44dw.temperature;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = SickPerson.class,
        parentColumns = "sickId",
        childColumns = "personId",
        onDelete = CASCADE))

public class Temperature {
    @PrimaryKey(autoGenerate = true)
    public long tempId;
    public long personId;
    public String date;
    public String time;
    public float temperature;
}
