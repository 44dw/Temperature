package com.a44dw.temperature;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Drug {

    @PrimaryKey(autoGenerate = true)
    public long drugId;
    public String name;
}
