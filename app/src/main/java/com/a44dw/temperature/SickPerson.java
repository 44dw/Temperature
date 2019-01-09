package com.a44dw.temperature;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.io.Serializable;

@Entity
public class SickPerson implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public long sickId;
    public String name;
}
