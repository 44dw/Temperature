package com.a44dw.temperature.activities;

import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import com.a44dw.temperature.App;
import com.a44dw.temperature.dialogs.DelDialog;
import com.a44dw.temperature.database.AppDatabase;
import com.a44dw.temperature.MainActivity;
import com.a44dw.temperature.entities.Note;
import com.a44dw.temperature.entities.PersonDrug;
import com.a44dw.temperature.entities.PersonSymptom;
import com.a44dw.temperature.R;
import com.a44dw.temperature.pojo.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FocusPoint extends AppCompatActivity
                        implements DelDialog.DelDialogListener {

    public static final String EXTRA_DRUGS = "changeDrugs";
    public static final String EXTRA_SYMPTS = "changeSympts";
    public static final String EXTRA_NOTE = "changeNote";
    public static final String EXTRA_TEMPID = "id";
    public static final String EXTRA_DEL = "del";
    public static final int REQUEST_CODE_ADDSYMPT = 1;
    public static final int REQUEST_CODE_ADDDRUG = 2;
    private LinearLayout focusDrug;
    private LinearLayout focusSympt;
    private EditText focusNote;
    private AppDatabase database;
    private long tempId;
    private String oldNote;
    private ArrayList<String[]> oldDrugArray = new ArrayList<>();
    private ArrayList<String> oldSymptArray = new ArrayList<>();
    private ArrayList<String[]> addedDrugArray = new ArrayList<>();
    private ArrayList<String> addedSymptArray = new ArrayList<>();
    private ArrayList<String[]> deletedDrugArray = new ArrayList<>();
    private ArrayList<String> deletedSymptArray = new ArrayList<>();
    private boolean drugArrayChange = false;
    private boolean symptArrayChange = false;
    private ExecutorService exec = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_point);

        prepareActionBar();

        Intent data = getIntent();
        HashMap<String, Object> dataMap = (HashMap<String, Object>) data.getSerializableExtra(MainActivity.EXTRA_ALL);
        tempId = data.getLongExtra(MainActivity.EXTRA_ID, 0);
        oldDrugArray = (ArrayList<String[]>) dataMap.get(Point.KEY_DARR);
        oldSymptArray = (ArrayList<String>) dataMap.get(Point.KEY_SARR);
        oldNote = (String) dataMap.get(Point.KEY_NOTE);

        database = App.getInstance().getDatabase();

        initUI(dataMap);
    }

    private void initUI(HashMap<String, Object> dataMap) {
        TextView focusTemp = findViewById(R.id.focus_temperature);
        TextView focusTime = findViewById(R.id.focus_time);
        TextView focusDate = findViewById(R.id.focus_date);
        focusDrug = findViewById(R.id.focus_drugs_name);
        focusSympt = findViewById(R.id.focus_symptoms_name);
        focusNote = findViewById(R.id.focus_note_edit);

        //заполняем поля времени, температуры и заметок
        focusTime.setText((String) dataMap.get(Point.KEY_TIME));
        focusDate.setText((String) dataMap.get(Point.KEY_DATE));
        String temp = String.valueOf(dataMap.get(Point.KEY_TEMP));
        if(!temp.equals("0.0")) focusTemp.setText(temp);
        focusNote.setText(oldNote);

        fillDrugField();
        fillSymptField();
    }

    private void fillSymptField() {
        StringBuilder builder = new StringBuilder();

        //заполняем поле симптомов из массива
        for (int i = 0; i < oldSymptArray.size(); i++) {
            builder.append(oldSymptArray.get(i));
            LinearLayout symptLine = (LinearLayout) getLayoutInflater().inflate(R.layout.generator_sympt_inflater, focusSympt, false);
            //прикрепляем тэг с номером позиции в массиве
            symptLine.setTag(i);
            TextView text = symptLine.findViewById(R.id.name);
            text.setText(builder.toString());
            focusSympt.addView(symptLine);
            builder.setLength(0);
        }
    }

    private void fillDrugField() {
        StringBuilder builder = new StringBuilder();

        //заполняем поле лекарств из массива
        for (int i = 0; i < oldDrugArray.size(); i++) {
            String drugs = oldDrugArray.get(i)[0] + " " + oldDrugArray.get(i)[1] + " " + oldDrugArray.get(i)[2];
            builder.append(drugs);
            LinearLayout drugLine = (LinearLayout) getLayoutInflater().inflate(R.layout.generator_drug_inflater, focusDrug, false);
            //прикрепляем тэг с номером позиции в массиве
            drugLine.setTag(i);
            TextView text = drugLine.findViewById(R.id.name);
            text.setText(builder.toString());
            focusDrug.addView(drugLine);
            builder.setLength(0);
        }
    }

    private void prepareActionBar() {
        //убираем тень и Title...
        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
            //вешаем стрелку назад
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }
    }

    public void onClickListener(View view) {
        switch (view.getId()) {
            case(R.id.delete): {
                deleteDrugSymptLine(view);
                break;
            }
            case(R.id.focus_sympt_add): {
                openAddSymptActivity();
                break;
            }
            case(R.id.focus_drug_add): {
                openAddDrugActivity();
                break;
            }
            case(R.id.focus_del): {
                showPointDeleteDialog();
            }
        }
    }

    private void showPointDeleteDialog() {
        //Создаём диалог и прикладываем к нему ссылку на текст, к-й нужно показать
        DialogFragment dialog = new DelDialog();
        Bundle args = new Bundle();
        args.putInt(DelDialog.TEXT_TO_SHOW, R.string.deldialog_shure_del_line);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "DelDialog");
    }

    private void openAddDrugActivity() {
        Intent intent = new Intent(getApplicationContext(), AddDrug.class);
        startActivityForResult(intent, REQUEST_CODE_ADDDRUG);
    }

    private void openAddSymptActivity() {
        Intent intent = new Intent(getApplicationContext(), AddSympt.class);
        startActivityForResult(intent, REQUEST_CODE_ADDSYMPT);
    }

    private void deleteDrugSymptLine(View view) {
        LinearLayout line = (LinearLayout) view.getParent();
        if(line.getTag() != null) {
            //получаем тэг с номером позиции в массиве
            int i = (int)line.getTag();
            if(line.getId() == R.id.drugLine) {
                //добавляем запись в массив на удаление
                deletedDrugArray.add(oldDrugArray.get(i));
                if (!drugArrayChange) drugArrayChange = true;
            }
            if(line.getId() == R.id.symptLine) {
                //удаляем запись из массива
                deletedSymptArray.add(oldSymptArray.get(i));
                if (!symptArrayChange) symptArrayChange = true;
            }
        } else {
            //удаляем из массива добавленных
            TextView tv = line.findViewById(R.id.name);
            String value = tv.getText().toString();
            if(line.getId() == R.id.drugLine){
                for(int i=0; i<addedDrugArray.size(); i++) {
                    String concat = addedDrugArray.get(i)[0] + " " +
                            addedDrugArray.get(i)[1] + " " +
                            addedDrugArray.get(i)[2];
                    if(concat.equals(value)) {
                        addedDrugArray.remove(i);
                        break;
                    }
                }
            }
            if(line.getId() == R.id.symptLine) {
                for(int i=0; i<addedSymptArray.size(); i++) {
                    if(addedSymptArray.get(i).equals(value)) {
                        addedSymptArray.remove(i);
                        break;
                    }
                }
            }
        }
        //получаем родителя элемента и удаляем элемент из дерева
        LinearLayout holder = (LinearLayout) view.getParent().getParent();
        holder.removeView(line);
    }

    //Если пользователь кликнул в диалоге на "ОК"...
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //удаляем записи из БД
        exec.execute(new Runnable() {
            @Override
            public void run() {
                database.temperatureDao().deleteById(tempId);
            }
        });

        Intent intent = new Intent();
        intent.putExtra(EXTRA_TEMPID, tempId);
        intent.putExtra(EXTRA_DEL, EXTRA_DEL);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_ADDDRUG) {
            if(resultCode == RESULT_OK) {
                //Добавляем лекарство в новый массив лекарств
                String[] drug = data.getStringArrayExtra(AddDrug.EXTRA_DRUG);

                addDrug(drug);
            }
        }
        if (requestCode == REQUEST_CODE_ADDSYMPT) {
            if(resultCode == RESULT_OK) {
                //Добавляем лекарство в новый массив симптомов
                String symptom = data.getStringExtra(AddSympt.EXTRA_SYMPT);

                addSympt(symptom);
            }
        }
    }

    private void addSympt(String symptom) {
        addedSymptArray.add(symptom);
        //Добавляем симптом в UI
        LinearLayout line = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.generator_sympt_inflater, focusSympt, false);
        TextView text = line.findViewById(R.id.name);
        text.setText(symptom);
        focusSympt.addView(line);
        if (!symptArrayChange) symptArrayChange = true;
    }

    private void addDrug(String[] drug) {
        addedDrugArray.add(drug);
        //Добавляем лекарство в UI
        LinearLayout line = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.generator_drug_inflater, focusDrug, false);
        TextView text = line.findViewById(R.id.name);
        String dname = drug[0] + " " + drug[1] + " " + drug[2];
        text.setText(dname);
        focusDrug.addView(line);
        if (!drugArrayChange) drugArrayChange = true;
    }

    //создаёт меню в "шапке"
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_focus_ok, menu);
        return true;
    }

    //При нажатии на кнопку "Назад"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_ok) {
            Intent intent = new Intent();
            //если были изменения в массиве лекарств...
            if((addedDrugArray.size() > 0)||(deletedDrugArray.size() > 0)) {
                //перебираем удалённые лекарства
                for(String[] dDrugArr : deletedDrugArray) {
                    deleteDrugFromArrayAndDB(dDrugArr);
                }
                //перебираем добавленные лекарства
                for(String[] aDrugArr : addedDrugArray) {
                    addDrugToArrayAndDB(aDrugArr);
                }
                intent.putExtra(EXTRA_DRUGS, oldDrugArray);
            }
            if((addedSymptArray.size() > 0)||(deletedSymptArray.size() > 0)) {
                //перебираем удалённые симптомы
                for(String dSympt : deletedSymptArray) {
                    deleteSymptFromArrayAndDB(dSympt);
                }
                //перебираем добавленные симптомы
                for(String aSympt : addedSymptArray) {
                    addSymptToArrayAndDB(aSympt);
                }
                intent.putExtra(EXTRA_SYMPTS, oldSymptArray);
            }
            String newNote = focusNote.getText().toString();
            //если заметка обновлялась...
            if(!oldNote.equals(newNote)) {
                //обновляем её в БД
                updateNote(newNote);
                intent.putExtra(EXTRA_NOTE, newNote);
            }
            intent.putExtra(EXTRA_TEMPID, tempId);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        super.onBackPressed();
        return true;
    }

    private void updateNote(final String newNote) {

        //проверяем, создана ли заметка в БД. Создаём или обновляем
        if(oldNote.length() == 0) {
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    final Note note = new Note();
                    note.tempId = tempId;
                    note.note = newNote;
                    database.noteDao().insert(note);
                }
            });
        } else {
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    Note note = database.noteDao().getById(tempId);
                    note.note = newNote;
                    database.noteDao().update(note);
                }
            });
        }
    }

    private void addSymptToArrayAndDB(final String aSympt) {
        //добавляем в лист
        oldSymptArray.add(aSympt);

        exec.execute(new Runnable() {
            @Override
            public void run() {
                PersonSymptom personSymptom = new PersonSymptom();
                personSymptom.temperatureId = tempId;
                personSymptom.symptomId = database.symptomDao().getId(aSympt);
                database.personSymptomDao().insert(personSymptom);
            }
        });
    }

    private void deleteSymptFromArrayAndDB(final String dSympt) {
        //удаляем из массива
        oldSymptArray.remove(dSympt);
        //отправляем данные в БД
        exec.execute(new Runnable() {
            @Override
            public void run() {
                PersonSymptom ps = database.personSymptomDao().getByTempId(tempId, dSympt);
                database.personSymptomDao().delete(ps);
            }
        });
    }

    private void addDrugToArrayAndDB(final String[] aDrugArr) {
        //добавляем в лист
        oldDrugArray.add(aDrugArr);

        exec.execute(new Runnable() {
            @Override
            public void run() {

            }
        });

        exec.execute(new Runnable() {
            @Override
            public void run() {
                //Добавляем каждое новое лекарство в БД
                PersonDrug personDrug = new PersonDrug();
                personDrug.temperatureId = tempId;
                personDrug.drugId = database.drugDao().getId(aDrugArr[0]);
                personDrug.amount = Float.parseFloat(aDrugArr[1]);
                personDrug.drugUnit = aDrugArr[2];
                database.personDrugDao().insert(personDrug);
            }
        });
    }

    private void deleteDrugFromArrayAndDB(final String[] dDrugArr) {
        //удаляем по индексу
        oldDrugArray.remove(dDrugArr);

        exec.execute(new Runnable() {
            @Override
            public void run() {
                PersonDrug pd = database.personDrugDao().getByTempId(tempId,
                        dDrugArr[0],
                        dDrugArr[1],
                        dDrugArr[2]);
                database.personDrugDao().delete(pd);
            }
        });
    }
}
