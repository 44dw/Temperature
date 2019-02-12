package com.a44dw.temperature.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.a44dw.temperature.entities.Drug;

import java.util.List;

@Dao
public interface DrugDao {
    @Query("SELECT * FROM drug")
    List<Drug> getAll();

    @Query("SELECT name FROM drug")
    List<String> getNames();

    @Query("SELECT drugId FROM drug WHERE name = :name")
    long getId(String name);

    @Insert
    long insert(Drug symptom);

    @Update
    void update(Drug symptom);

    @Delete
    void delete(Drug symptom);
}
