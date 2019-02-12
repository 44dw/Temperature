package com.a44dw.temperature.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.a44dw.temperature.App;
import com.a44dw.temperature.MainActivity;
import com.a44dw.temperature.R;
import com.a44dw.temperature.database.AppDatabase;
import com.a44dw.temperature.dialogs.PickSickDialog;
import com.a44dw.temperature.entities.Note;
import com.a44dw.temperature.entities.PersonDrug;
import com.a44dw.temperature.entities.PersonSymptom;
import com.a44dw.temperature.entities.SickPerson;
import com.a44dw.temperature.entities.Temperature;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PointGenerator extends AppCompatActivity implements PickSickDialog.PickSickDialogListener {

    public static final String EXTRA_DATA = "data";
    final int REQUEST_CODE_ADDSYMPT = 1;
    final int REQUEST_CODE_ADDDRUG = 2;
    final int REQUEST_CODE_ADDNOTE = 3;
    static AppDatabase database;
    ArrayList<String[]> drugArray;
    ArrayList<String> symptArray;
    String note;
    Boolean changedPerson;
    SickPerson nowPerson;
    ImageView noteButton;
    LinearLayout symptLayout;
    LinearLayout drugLayout;
    Button dateButton;
    DatePickerDialog dateDialog;
    TimePickerDialog timeDialog;
    HashMap<String, String> dateTimeMap;
    boolean changedDateTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_generator);
        Intent data = getIntent();
        //убираем тень и Title...
        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
            //вешаем стрелку назад
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }
        //получаем ссылку на БД
        database = App.getInstance().getDatabase();
        drugArray = new ArrayList<>();
        symptArray = new ArrayList<>();
        changedPerson = false;
        symptLayout = findViewById(R.id.symptLayout);
        drugLayout = findViewById(R.id.drugLayout);
        noteButton = findViewById(R.id.noteButton);
        dateButton = findViewById(R.id.dateButton);
        findViewById(R.id.tempField).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) ((EditText)v).setHint(R.string.temp_input_empty);
                else ((EditText)v).setHint(R.string.temp_input);
            }
        });
        dateTimeMap = new HashMap<>();

        //Если активность вызывается в первый раз, создаём и запускаем диалог,
        if(data.hasExtra(MainActivity.EXTRA_FIRST)) {
            PickSickDialog pickSickDialog = new PickSickDialog();
            pickSickDialog.show(getSupportFragmentManager(), "PickSickDialog");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_ADDDRUG) {
            if(resultCode == RESULT_OK) {
                String[] drug = data.getStringArrayExtra(AddDrug.EXTRA_DRUG);
                //добавляем в общий массив
                drugArray.add(drug);
                LinearLayout line = (LinearLayout) getLayoutInflater()
                        .inflate(R.layout.generator_drug_inflater, drugLayout, false);
                TextView text = line.findViewById(R.id.name);
                String newDrug = drug[0] + " " + drug[1] + " " + drug[2];
                text.setText(newDrug);
                line.setTag("drug");
                drugLayout.addView(line);
            }
        }

        if (requestCode == REQUEST_CODE_ADDSYMPT) {
            if(resultCode == RESULT_OK) {
                String symptom = data.getStringExtra(AddSympt.EXTRA_SYMPT);
                symptArray.add(symptom);
                LinearLayout line = (LinearLayout) getLayoutInflater()
                        .inflate(R.layout.generator_sympt_inflater, symptLayout, false);
                TextView text = line.findViewById(R.id.name);
                text.setText(symptom);
                line.setTag("sympt");
                symptLayout.addView(line);
            }
        }

        if(requestCode == REQUEST_CODE_ADDNOTE) {
            if(resultCode == RESULT_OK) {
                note = data.getStringExtra(AddNote.EXTRA_NOTE);
                if(note.length() > 0) {
                    noteButton.setImageResource(R.drawable.ic_writing_on);
                    noteButton.setBackgroundResource(R.drawable.background_has_note);
                }
            }
        }
    }

    public void onClickListener(View view) {
        switch (view.getId()) {
            case (R.id.delete): {
                LinearLayout line = (LinearLayout) view.getParent();
                LinearLayout holder = (LinearLayout) view.getParent().getParent();
                TextView name = line.findViewById(R.id.name);
                String fromName = name.getText().toString();
                switch (line.getTag().toString()) {
                    case "sympt": {
                        for(int i=0; i<symptArray.size(); i++) {
                            if(symptArray.get(i).equals(fromName)) {
                                symptArray.remove(i);
                                break;
                            }
                        }
                        break;
                    }
                    case "drug": {
                        for(int i=0; i<drugArray.size(); i++) {
                            String[] drug = drugArray.get(i);
                            String joinName = drug[0] + " " + drug[1] + " " + drug[2];
                            if(joinName.equals(fromName)) {
                                drugArray.remove(i);
                                break;
                            }
                        }
                        break;
                    }
                }
                holder.removeView(line);
                break;
            }
            case (R.id.noteButton): {
                Intent intent = new Intent(getApplicationContext(), AddNote.class);
                startActivityForResult(intent, REQUEST_CODE_ADDNOTE);
                break;
            }
            case (R.id.symptButton): {
                Intent intent = new Intent(getApplicationContext(), AddSympt.class);
                startActivityForResult(intent, REQUEST_CODE_ADDSYMPT);
                break;
            }
            case (R.id.drugsButton): {
                Intent intent = new Intent(getApplicationContext(), AddDrug.class);
                startActivityForResult(intent, REQUEST_CODE_ADDDRUG);
                break;
            }
            case (R.id.dateButton): {
                int year = Calendar.getInstance().get(Calendar.YEAR);
                int month = Calendar.getInstance().get(Calendar.MONTH);
                int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                dateDialog = new DatePickerDialog(this, dateCallback, year, month, day);
                dateDialog.setTitle(R.string.dialog_date_header);
                dateDialog.show();
                break;
            }
            case (R.id.createPoint): {
                //на случай, если пользоварель отменил диалог
                if((App.getInstance().getPerson() == null)&&(!changedPerson)) {
                    try{
                        //пробуем запросить учётную запись гостя;
                        SickPersonDaoGetByName sickPersonDaoGetByName = new SickPersonDaoGetByName();
                        sickPersonDaoGetByName.execute("Гость");
                        nowPerson = sickPersonDaoGetByName.get();
                        //если такой нет - создаём
                        if(nowPerson == null) {
                            nowPerson = new SickPerson();
                            nowPerson.name = "Гость";
                            SickPersonDaoInsert sickPersonDaoInsert = new SickPersonDaoInsert();
                            sickPersonDaoInsert.execute(nowPerson);
                            nowPerson.sickId = sickPersonDaoInsert.get();
                            changedPerson = true;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                //устонавливаем текущего больного
                if(changedPerson) App.getInstance().setPerson(nowPerson);
                //собираем данные из поля температуры
                EditText tempField = findViewById(R.id.tempField);
                final String regex1 = "\\d{2}\\.\\d";
                final String regex2 = "\\d{2}";
                Pattern lng = Pattern.compile(regex1);
                Pattern shrt = Pattern.compile(regex2);
                String valueFromTempField = tempField.getText().toString();
                //проверка, не пусто ли поле
                if((drugArray.size() == 0)&&
                        (symptArray.size() == 0)&&
                        (note == null)&&
                        (valueFromTempField.isEmpty())) {
                    Toast.makeText(getApplicationContext(),
                            "хотя бы одно поле должно быть заполнено!",
                            Toast.LENGTH_LONG).show();
                    break;
                }
                //если поле температуры не пусто...
                String regexValue = null;
                if(!valueFromTempField.isEmpty()) {
                    //приведение строки к формату
                    Matcher lngMatcher = lng.matcher(valueFromTempField);
                    Matcher shrtMatcher = shrt.matcher(valueFromTempField);
                    if(lngMatcher.find()) regexValue = lngMatcher.group();
                    else if(shrtMatcher.find()) regexValue = shrtMatcher.group();
                    else {
                        Toast.makeText(getApplicationContext(),
                                "неверный формат температуры!",
                                Toast.LENGTH_LONG).show();
                        break;
                    }
                    //проверка допуска по значениям
                    float f = Float.parseFloat(regexValue);
                    if((f < 35)||(f > 42)) {
                        Toast.makeText(getApplicationContext(),
                                "укажите значение в пределах от 35 до 42!",
                                Toast.LENGTH_LONG).show();
                        break;
                    }
                }

                Intent intent = new Intent();
                float nowTemp = 0.0f;
                if (regexValue != null) nowTemp = Float.parseFloat(regexValue);
                //запоминаем дату в виде объекта
                String time;
                String date;
                if(dateTimeMap.size() > 1) {
                    time = dateTimeMap.get("time");
                    date = dateTimeMap.get("date");
                } else {
                    Date nowDatetime = new Date();
                    time = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(nowDatetime);
                    date = new SimpleDateFormat("dd.MM.yy", java.util.Locale.getDefault()).format(nowDatetime);
                }
                //Отправляем данные о температуре в AppDatabase
                Temperature t = new Temperature();
                t.date = date;
                t.time = time;
                t.temperature = nowTemp;
                t.personId = App.getInstance().getPerson().sickId;

                //вставляем t в БД
                TemperatureDaoInsert temperatureDaoInsert = new TemperatureDaoInsert();
                temperatureDaoInsert.execute(t);

                //получаем id вставленной записи
                try {
                    Long temperatureId = temperatureDaoInsert.get();
                    SymptomDrugDaoGetId symptomDrugDaoGetId;
                    //формируем список симптомов и на снове каждого создаём PersonSymptom
                    for (String symptom : symptArray) {
                        final PersonSymptom personSymptom = new PersonSymptom();
                        personSymptom.temperatureId = temperatureId;
                        symptomDrugDaoGetId = new SymptomDrugDaoGetId();
                        symptomDrugDaoGetId.execute(symptom, "symptom");
                        personSymptom.symptomId = symptomDrugDaoGetId.get();
                        new Thread(new Runnable() {
                                @Override
                                public void run() {
                            database.personSymptomDao().insert(personSymptom);
                            }
                        }).start();
                    }
                    //формируем список лекарств и на снове каждого создаём PersonDrug
                    for(String[] drug : drugArray) {
                        final PersonDrug personDrug = new PersonDrug();
                        personDrug.temperatureId = temperatureId;
                        personDrug.amount = Float.parseFloat(drug[1]);
                        personDrug.drugUnit = drug[2];
                        symptomDrugDaoGetId = new SymptomDrugDaoGetId();
                        symptomDrugDaoGetId.execute(drug[0], "drug");
                        personDrug.drugId = symptomDrugDaoGetId.get();
                        new Thread(new Runnable() {
                                @Override
                                public void run() {
                            database.personDrugDao().insert(personDrug);
                            }
                            }).start();
                    }
                    //отправляем данные в Main Activity
                    Bundle bundle = new Bundle();
                    bundle.putFloat("nowTemp", nowTemp);
                    bundle.putLong("temperatureId", temperatureId);
                    bundle.putSerializable("drugArray", drugArray);
                    bundle.putStringArrayList("symptArray", symptArray);
                    bundle.putString("time", time);
                    bundle.putString("date", date);
                    bundle.putBoolean("changedPerson", changedPerson);
                    bundle.putBoolean("changedDateTime", changedDateTime);

                    //формируем Note
                    if(note != null) {
                        final Note newNote = new Note();
                        newNote.tempId = temperatureId;
                        newNote.note = note;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                database.noteDao().insert(newNote);
                            }
                        }).start();
                        bundle.putString("note", note);
                    }

                    intent.putExtra(EXTRA_DATA, bundle);
                    setResult(RESULT_OK, intent);
                    finish();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    DatePickerDialog.OnDateSetListener dateCallback = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

            String day = String.valueOf(dayOfMonth);
            String mnth = String.valueOf(++month);
            String yr = String.valueOf(year);
            yr = yr.substring(2);
            if(day.length() == 1) day = "0" + day;
            if(mnth.length() == 1) mnth = "0" + month;
            String date = day + "." + mnth + "." + yr;

            dateTimeMap.put("date", date);

            timeDialog = new TimePickerDialog(PointGenerator.this, timeCallback,
                    Calendar.getInstance().get(Calendar.HOUR),
                    Calendar.getInstance().get(Calendar.MINUTE),
                    true);

            timeDialog.show();
        }
    };

    TimePickerDialog.OnTimeSetListener timeCallback = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

            String hour = String.valueOf(hourOfDay);
            String min = String.valueOf(minute);
            if(hour.length() == 1) hour = "0" + hour;
            if(min.length() == 1) min = "0" + min;

            dateTimeMap.put("time", hour + ":" + min);

            String dateTime = dateTimeMap.get("date") + ", " +
                    dateTimeMap.get("time");

            changedDateTime = true;

            dateButton.setText(dateTime);
        }
    };

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String name, Boolean newName) {
        try {
            if(newName) {
                nowPerson = new SickPerson();
                nowPerson.name = name;

                SickPersonDaoInsert sickPersonDaoInsert = new SickPersonDaoInsert();
                sickPersonDaoInsert.execute(nowPerson);
                nowPerson.sickId = sickPersonDaoInsert.get();
            } else {
                SickPersonDaoGetByName sickPersonDaoGetByName = new SickPersonDaoGetByName();
                sickPersonDaoGetByName.execute(name);
                nowPerson = sickPersonDaoGetByName.get();
            }
            changedPerson = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    //При нажатии на кнопку "Назад"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    //классы AsyncTask
    static class TemperatureDaoInsert extends AsyncTask<Temperature, Void, Long> {
        @Override
        protected Long doInBackground(Temperature... temperatures) {
            return database.temperatureDao().insert(temperatures[0]);
        }
    }
    static class SymptomDrugDaoGetId extends AsyncTask<String, Void, Long> {
        @Override
        protected Long doInBackground(String... unit) {
            if (unit[1].equals("symptom")) return database.symptomDao().getId(unit[0]);
            return database.drugDao().getId(unit[0]);
        }
    }
    static private class SickPersonDaoGetByName extends AsyncTask<String, Void, SickPerson> {
        @Override
        protected SickPerson doInBackground(String... name) {
            return database.sickPersonDao().getByName(name[0]);
        }
    }
    static private class SickPersonDaoInsert extends AsyncTask<SickPerson, Void, Long> {
        @Override
        protected Long doInBackground(SickPerson... person) {
            return database.sickPersonDao().insert(person[0]);
        }
    }
}