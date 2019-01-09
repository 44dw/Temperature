package com.a44dw.temperature;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface TemperatureDao {
    @Query("SELECT * FROM temperature")
    List<Temperature> getAll();

    @Query("SELECT DISTINCT date FROM temperature WHERE personId = :id")
    List<String> getUnicDatesWithPerson(long id);

    @Query("SELECT * FROM temperature WHERE personId = :id AND date = :date")
    List<Temperature> getAllWithPersonAndDate(long id, String date);

    @Query("SELECT * FROM temperature WHERE personId = :id AND date in (SELECT MAX(date) FROM temperature)")
    List<Temperature> getAllWithPersonAndLastDate(long id);

    @Query("SELECT Note.note, Note.tempId FROM Note, Temperature WHERE Note.tempId = Temperature.tempId AND Temperature.date = :date AND Temperature.personId = :id")
    List<PersonDateNote> getNotesWithPersonAndDate(long id, String date);

    @Query("DELETE FROM temperature WHERE tempId = :id")
    void deleteById(long id);

    @Insert
    long insert(Temperature temperature);

    @Update
    void update(Temperature temperature);

    @Delete
    void delete(Temperature temperature);
}