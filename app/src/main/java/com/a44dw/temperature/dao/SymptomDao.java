package com.a44dw.temperature.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.a44dw.temperature.entities.Symptom;

import java.util.List;

@Dao
public interface SymptomDao {
    @Query("SELECT * FROM symptom")
    List<Symptom> getAll();

    @Query("SELECT name FROM symptom")
    List<String> getNames();

    @Query("SELECT symptId FROM symptom WHERE name = :name")
    long getId(String name);

    @Insert
    long insert(Symptom symptom);

    @Update
    void update(Symptom symptom);

    @Delete
    void delete(Symptom symptom);
}
