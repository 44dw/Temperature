package com.a44dw.temperature;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = Temperature.class, parentColumns = "tempId", childColumns = "tempId", onDelete = CASCADE))
public class Note {
    @PrimaryKey(autoGenerate = true)
    public long noteId;
    public long tempId;
    public String note;
}
