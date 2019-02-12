package com.a44dw.temperature.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.a44dw.temperature.pojo.ConcreteTemperatureDrug;
import com.a44dw.temperature.entities.PersonDrug;

import java.util.List;

@Dao
public interface PersonDrugDao {
    @Query("SELECT * FROM persondrug")
    List<PersonDrug> getAll();

    @Query("SELECT * FROM PersonDrug WHERE temperatureId=:id AND drugId=(SELECT drugId FROM Drug WHERE name=:name) AND amount=:amount AND drugUnit=:unit")
    PersonDrug getByTempId(long id, String name, String amount, String unit);

    @Query("SELECT d.name AS drug_name, pd.amount, pd.drugUnit FROM Drug as d, PersonDrug as pd, Temperature as t WHERE pd.drugId = d.drugId AND pd.temperatureId = t.tempId AND t.tempId = :id")
    List<ConcreteTemperatureDrug> getConcreteTemperatureDrug(long id);

    @Insert
    long insert(PersonDrug personDrug);

    @Update
    void update(PersonDrug personDrug);

    @Delete
    void delete(PersonDrug personDrug);
}
