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
import com.a44dw.temperature.interfaces.PersonSettable;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PointGenerator extends AppCompatActivity implements PickSickDialog.PickSickDialogListener, View.OnClickListener, PersonSettable {

    public static final String EXTRA_DATA = "data";
    private final int REQUEST_CODE_ADDSYMPT = 1;
    private final int REQUEST_CODE_ADDDRUG = 2;
    private final int REQUEST_CODE_ADDNOTE = 3;
    private AppDatabase database;
    private ArrayList<String[]> drugArray;
    private ArrayList<String> symptArray;
    private String note;
    private SickPerson nowPerson;
    private ImageView noteButton;
    private LinearLayout symptLayout;
    private LinearLayout drugLayout;
    private Button dateButton;
    private HashMap<String, String> dateTimeMap;
    private Boolean changedPerson = false;
    private boolean changedDateTime = false;

    private DatePickerDialog dateDialog;
    private TimePickerDialog timeDialog;
    DatePickerDialog.OnDateSetListener dateCallback;
    TimePickerDialog.OnTimeSetListener timeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_generator);
        Intent data = getIntent();
        //получаем ссылку на БД
        database = App.getInstance().getDatabase();

        initUI();

        drugArray = new ArrayList<>();
        symptArray = new ArrayList<>();
        dateTimeMap = new HashMap<>();

        initDateTimeCallbacks();

        //Если активность вызывается в первый раз, создаём и запускаем диалог,
        if(data.hasExtra(MainActivity.EXTRA_FIRST)) {
            PickSickDialog pickSickDialog = new PickSickDialog();
            pickSickDialog.show(getSupportFragmentManager(), "PickSickDialog");
        }
    }

    private void initDateTimeCallbacks() {

        dateCallback = new DatePickerDialog.OnDateSetListener() {
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

        timeCallback = new TimePickerDialog.OnTimeSetListener() {
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
    }

    private void initUI() {
        //убираем тень и Title...
        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
            //вешаем стрелку назад
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }
        symptLayout = findViewById(R.id.symptLayout);
        drugLayout = findViewById(R.id.drugLayout);
        noteButton = findViewById(R.id.noteButton);
        dateButton = findViewById(R.id.dateButton);

        findViewById(R.id.noteButton).setOnClickListener(this);
        findViewById(R.id.symptButton).setOnClickListener(this);
        findViewById(R.id.drugsButton).setOnClickListener(this);
        findViewById(R.id.dateButton).setOnClickListener(this);
        findViewById(R.id.createPoint).setOnClickListener(this);

        findViewById(R.id.tempField).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) ((EditText)v).setHint(R.string.temp_input_empty);
                else ((EditText)v).setHint(R.string.temp_input);
            }
        });
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.delete): {
                removeDrugOrSympt(view);
                break;
            }
            case (R.id.noteButton): {
                startAddNoteActivity();
                break;
            }
            case (R.id.symptButton): {
                startAddSymptActivity();
                break;
            }
            case (R.id.drugsButton): {
                startAddDrugActivity();
                break;
            }
            case (R.id.dateButton): {
                startDateTimePicker();
                break;
            }
            case (R.id.createPoint): {

                //проверяем, установлен ли юзер
                checkSickPerson();

                break;
            }
        }
    }

    private void checkSickPerson() {
        //на случай, если пользоварель отменил диалог
        if ((App.getInstance().getPerson() == null) && (!changedPerson)) {
            LateSickPersonDaoGetByName sickPersonDaoGetByName = new LateSickPersonDaoGetByName(this);
            sickPersonDaoGetByName.execute("Гость");
        } else {
            //собираем данные из полей
            collectDataFromFields();
        }
    }

    public void setPerson(SickPerson person) {
        nowPerson = person;
        changedPerson = true;
        App.getInstance().setPerson(person);
    }

    private void collectDataFromFields() {
        //собираем данные из поля температуры
        EditText tempField = findViewById(R.id.tempField);
        final String regex1 = "\\d{2}\\.\\d";
        final String regex2 = "\\d{2}";
        Pattern lng = Pattern.compile(regex1);
        Pattern shrt = Pattern.compile(regex2);
        String valueFromTempField = tempField.getText().toString();

        //проверяем, что все поля не пустые
        if(!checkIfAllFieldsNotEmpty(drugArray, symptArray, note, valueFromTempField)) {
            showEmptyFieldsToast();
            return;
        }

        //если поле температуры не пусто...
        String regexValue = null;
        if (!valueFromTempField.isEmpty()) {
            //приведение строки к формату
            Matcher lngMatcher = lng.matcher(valueFromTempField);
            Matcher shrtMatcher = shrt.matcher(valueFromTempField);
            if (lngMatcher.find()) regexValue = lngMatcher.group();
            else if (shrtMatcher.find()) regexValue = shrtMatcher.group();
            else {
                showWrongTempFormatToast(false);
                return;
            }
            //проверка допуска по значениям
            float f = Float.parseFloat(regexValue);
            if ((f < 35) || (f > 42)) {
                showWrongTempFormatToast(true);
                return;
            }
        }

        float nowTemp = 0.0f;
        if (regexValue != null) nowTemp = Float.parseFloat(regexValue);

        //запоминаем дату в виде объекта
        String time;
        String date;
        if (dateTimeMap.size() > 1) {
            time = dateTimeMap.get("time");
            date = dateTimeMap.get("date");
        } else {
            Date nowDatetime = new Date();
            time = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(nowDatetime);
            date = new SimpleDateFormat("dd.MM.yy", java.util.Locale.getDefault()).format(nowDatetime);
        }

        new InsertDataInDB(this, date, time, nowTemp, App.getInstance().getPerson().getSickId(),
                symptArray, drugArray, note).execute();
    }

    private void showWrongTempFormatToast(boolean modificator) {
        if(modificator) {
            Toast.makeText(getApplicationContext(),
                    "укажите значение в пределах от 35 до 42!",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(),
                    "неверный формат температуры!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showEmptyFieldsToast() {
        Toast.makeText(getApplicationContext(),
                "хотя бы одно поле должно быть заполнено!",
                Toast.LENGTH_LONG).show();
    }

    private boolean checkIfAllFieldsNotEmpty(ArrayList<String[]> drugArray,
                                             ArrayList<String> symptArray,
                                             String note,
                                             String valueFromTempField) {
        return (drugArray.size() != 0) ||
                (symptArray.size() != 0) ||
                (note != null) ||
                (!valueFromTempField.isEmpty());
    }

    private void processResultToMain(Bundle bundle) {
        bundle.putBoolean("changedPerson", changedPerson);
        bundle.putBoolean("changedDateTime", changedDateTime);

        //отправляем данные в Main Activity
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DATA, bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void startDateTimePicker() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int month = Calendar.getInstance().get(Calendar.MONTH);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        dateDialog = new DatePickerDialog(this, dateCallback, year, month, day);
        dateDialog.setTitle(R.string.dialog_date_header);
        dateDialog.show();
    }

    private void startAddDrugActivity() {
        Intent intent = new Intent(getApplicationContext(), AddDrug.class);
        startActivityForResult(intent, REQUEST_CODE_ADDDRUG);
    }

    private void startAddSymptActivity() {
        Intent intent = new Intent(getApplicationContext(), AddSympt.class);
        startActivityForResult(intent, REQUEST_CODE_ADDSYMPT);
    }

    private void startAddNoteActivity() {
        Intent intent = new Intent(getApplicationContext(), AddNote.class);
        startActivityForResult(intent, REQUEST_CODE_ADDNOTE);
    }

    private void removeDrugOrSympt(View view) {
        LinearLayout line = (LinearLayout) view.getParent();
        LinearLayout holder = (LinearLayout) view.getParent().getParent();
        TextView name = line.findViewById(R.id.name);
        String fromName = name.getText().toString();
        switch (line.getTag().toString()) {
            case "sympt": {
                for (int i = 0; i < symptArray.size(); i++) {
                    if (symptArray.get(i).equals(fromName)) {
                        symptArray.remove(i);
                        break;
                    }
                }
                break;
            }
            case "drug": {
                for (int i = 0; i < drugArray.size(); i++) {
                    String[] drug = drugArray.get(i);
                    String joinName = drug[0] + " " + drug[1] + " " + drug[2];
                    if (joinName.equals(fromName)) {
                        drugArray.remove(i);
                        break;
                    }
                }
                break;
            }
        }
        holder.removeView(line);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, final SickPerson person) {
        if(!person.isExist()) {
            new MainActivity.InsertPersonInDB(this).execute(person);
        } else setPerson(person);
    }

    //При нажатии на кнопку "Назад"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return true;
    }

    static private class LateSickPersonDaoGetByName extends AsyncTask<String, Void, SickPerson> {

        WeakReference<PointGenerator> wrActivity;

        public LateSickPersonDaoGetByName(PointGenerator activity) {
            this.wrActivity = new WeakReference<>(activity);
        }

        @Override
        protected SickPerson doInBackground(String... name) {

            SickPerson person = wrActivity.get().database.sickPersonDao().getByName(name[0]);
            //если такой нет - создаём
            if (person == null) {
                person = new SickPerson(name[0]);
                person.setSickId(wrActivity.get().database.sickPersonDao().insert(person));
            }

            return person;
        }

        @Override
        protected void onPostExecute(SickPerson person) {
            PointGenerator generatorActivity = wrActivity.get();

            generatorActivity.setPerson(person);

            //собираем данные из полей
            generatorActivity.collectDataFromFields();
        }
    }

    private static class InsertDataInDB extends AsyncTask<Void, Void, Bundle> {

        WeakReference<PointGenerator> wrActivity;

        String date;
        String time;
        float nowTemp;
        long sickId;
        ArrayList<String> symptArray;
        ArrayList<String[]> drugArray;
        String note;

        public InsertDataInDB(PointGenerator activity, String date, String time, float nowTemp, long sickId, ArrayList<String> symptArray, ArrayList<String[]> drugArray, String note) {
            this.wrActivity = new WeakReference<>(activity);

            this.date = date;
            this.time = time;
            this.nowTemp = nowTemp;
            this.sickId = sickId;
            this.symptArray = symptArray;
            this.drugArray = drugArray;
            this.note = note;
        }

        @Override
        protected Bundle doInBackground(Void... voids) {

            //Отправляем данные о температуре в AppDatabase
            Temperature t = new Temperature();
            t.date = date;
            t.time = time;
            t.temperature = nowTemp;
            t.personId = App.getInstance().getPerson().getSickId();

            //вставляем t в БД
            long temperatureId = wrActivity.get().database.temperatureDao().insert(t);

            //формируем список симптомов и на снове каждого создаём PersonSymptom
            for (String symptom : symptArray) {
                PersonSymptom personSymptom = new PersonSymptom();
                personSymptom.temperatureId = temperatureId;
                personSymptom.symptomId = wrActivity.get().database.symptomDao().getId(symptom);
                wrActivity.get().database.personSymptomDao().insert(personSymptom);
            }
            //формируем список лекарств и на снове каждого создаём PersonDrug
            for (String[] drug : drugArray) {
                final PersonDrug personDrug = new PersonDrug();
                personDrug.temperatureId = temperatureId;
                personDrug.amount = Float.parseFloat(drug[1]);
                personDrug.drugUnit = drug[2];
                personDrug.drugId = wrActivity.get().database.drugDao().getId(drug[0]);
                wrActivity.get().database.personDrugDao().insert(personDrug);
            }

            //формируем Note
            if (note != null) {
                Note newNote = new Note();
                newNote.tempId = temperatureId;
                newNote.note = note;
                wrActivity.get().database.noteDao().insert(newNote);
            }

            return createBundle(temperatureId);
        }

        private Bundle createBundle(long temperatureId) {
            Bundle bundle = new Bundle();
            bundle.putFloat("nowTemp", nowTemp);
            bundle.putLong("temperatureId", temperatureId);
            bundle.putSerializable("drugArray", drugArray);
            bundle.putStringArrayList("symptArray", symptArray);
            bundle.putString("time", time);
            bundle.putString("date", date);
            if(note != null) bundle.putString("note", note);

            return bundle;
        }

        @Override
        protected void onPostExecute(Bundle bundle) {
            wrActivity.get().processResultToMain(bundle);
        }
    }
}