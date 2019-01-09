package com.a44dw.temperature;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Symptom {

    @PrimaryKey(autoGenerate = true)
    public long symptId;
    public String name;
}
