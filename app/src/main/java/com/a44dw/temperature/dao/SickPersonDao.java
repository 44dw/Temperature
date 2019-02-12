package com.a44dw.temperature.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.a44dw.temperature.pojo.PersonDrugHistory;
import com.a44dw.temperature.pojo.PersonSymptHistory;
import com.a44dw.temperature.entities.SickPerson;

import java.util.List;

@Dao
public interface SickPersonDao {
    @Query("SELECT * FROM sickperson")
    List<SickPerson> getAll();

    @Query("SELECT name FROM sickperson")
    List<String> getNames();

    @Query("SELECT * FROM sickperson WHERE name = :name")
    SickPerson getByName(String name);

    @Query("SELECT pd.temperatureId, t.time, d.name, pd.amount, pd.drugUnit " +
            "FROM Drug AS d, PersonDrug AS pd, Temperature AS t " +
            "WHERE pd.drugId = d.drugId AND pd.temperatureId = t.tempId AND t.personId = :id AND t.date = :date")
    List<PersonDrugHistory> getPersonDrugHistory(long id, String date);

    @Query("SELECT ps.temperatureId, t.time, s.name " +
            "FROM Symptom AS s, PersonSymptom AS ps, Temperature AS t " +
            "WHERE ps.symptomId = s.symptId AND ps.temperatureId = t.tempId AND t.personId = :id  AND t.date = :date")
    List<PersonSymptHistory> getPersonSymptHistory(long id, String date);

    @Query("DELETE FROM SickPerson WHERE name = :name")
    void deleteByName(String name);

    @Insert
    long insert(SickPerson person);

    @Update
    void update(SickPerson person);

    @Delete
    void delete(SickPerson person);
}