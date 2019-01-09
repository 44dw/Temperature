package com.a44dw.temperature;

import android.content.Intent;
import android.os.AsyncTask;
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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class FocusPoint extends AppCompatActivity
                        implements DelDialogFragment.DelDialogListener {

    public static final String EXTRA_DRUGS = "changeDrugs";
    public static final String EXTRA_SYMPTS = "changeSympts";
    public static final String EXTRA_NOTE = "changeNote";
    public static final String EXTRA_TEMPID = "id";
    public static final String EXTRA_DEL = "del";
    final int REQUEST_CODE_ADDSYMPT = 1;
    final int REQUEST_CODE_ADDDRUG = 2;
    LinearLayout focusDrug;
    LinearLayout focusSympt;
    EditText focusNote;
    static AppDatabase database;
    static long tempId;
    String oldNote;
    ArrayList<String[]> oldDrugArray;
    ArrayList<String> oldSymptArray;
    ArrayList<String[]> addedDrugArray;
    ArrayList<String> addedSymptArray;
    ArrayList<String[]> deletedDrugArray;
    ArrayList<String> deletedSymptArray;
    boolean drugArrayChange;
    boolean symptArrayChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_point);
        //убираем тень и Title...
        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
            //вешаем стрелку назад
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }

        Intent data = getIntent();
        ArrayList dataList = (ArrayList) data.getSerializableExtra(MainActivity.EXTRA_ALL);
        tempId = data.getLongExtra(MainActivity.EXTRA_ID, 0);
        oldDrugArray = (ArrayList<String[]>) dataList.get(3);
        oldSymptArray = (ArrayList<String>) dataList.get(4);
        addedDrugArray = new ArrayList<>();
        addedSymptArray = new ArrayList<>();
        deletedDrugArray = new ArrayList<>();
        deletedSymptArray = new ArrayList<>();
        oldNote = (String) dataList.get(5);
        drugArrayChange = false;
        symptArrayChange = false;

        database = App.getInstance().getDatabase();

        TextView focusTemp = findViewById(R.id.focus_temperature);
        TextView focusTime = findViewById(R.id.focus_time);
        TextView focusDate = findViewById(R.id.focus_date);
        focusDrug = findViewById(R.id.focus_drugs_name);
        focusSympt = findViewById(R.id.focus_symptoms_name);
        focusNote = findViewById(R.id.focus_note_edit);

        //заполняем поля времени, температуры и заметок
        focusTime.setText((String) dataList.get(0));
        focusDate.setText((String) dataList.get(1));
        String temp = String.valueOf(dataList.get(2));
        if(!temp.equals("0.0")) focusTemp.setText(temp);
        focusNote.setText(oldNote);

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

    public void onClickListener(View view) {
        switch (view.getId()) {
            case(R.id.delete): {
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
                break;
            }
            case(R.id.focus_sympt_add): {
                Intent intent = new Intent(getApplicationContext(), AddSympt.class);
                startActivityForResult(intent, REQUEST_CODE_ADDSYMPT);
                break;
            }
            case(R.id.focus_drug_add): {
                Intent intent = new Intent(getApplicationContext(), AddDrug.class);
                startActivityForResult(intent, REQUEST_CODE_ADDDRUG);
                break;
            }
            case(R.id.focus_del): {
                //Создаём диалог и прикладываем к нему ссылку на текст, к-й нужно показать
                DialogFragment dialog = new DelDialogFragment();
                Bundle args = new Bundle();
                args.putInt("textToShow", R.string.deldialog_shure_del_line);
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), "DelDialogFragment");
            }
        }
    }

    //Если пользователь кликнул в диалоге на "ОК"...
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //удаляем записи из БД
        new Thread(new Runnable() {
            @Override
            public void run() {
                database.temperatureDao().deleteById(tempId);
            }
        }).start();

        Intent intent = new Intent();
        intent.putExtra(EXTRA_TEMPID, tempId);
        intent.putExtra(EXTRA_DEL, "del");
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
        }
        if (requestCode == REQUEST_CODE_ADDSYMPT) {
            if(resultCode == RESULT_OK) {
                //Добавляем лекарство в новый массив симптомов
                String symptom = data.getStringExtra(AddSympt.EXTRA_SYMPT);
                addedSymptArray.add(symptom);
                //Добавляем симптом в UI
                LinearLayout line = (LinearLayout) getLayoutInflater()
                        .inflate(R.layout.generator_sympt_inflater, focusSympt, false);
                TextView text = line.findViewById(R.id.name);
                text.setText(symptom);
                focusSympt.addView(line);
                if (!symptArrayChange) symptArrayChange = true;
            }
        }
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
            try {
                //если были изменения в массиве лекарств...
                if((addedDrugArray.size() > 0)||(deletedDrugArray.size() > 0)) {
                    //перебираем удалённые лекарства
                    for(String[] dDrugArr : deletedDrugArray) {
                        //проверяем, присутствовало ли лекарство в массиве
                        int index = oldDrugArray.indexOf(dDrugArr);
                        //Map<Long, String[]> args = new HashMap<>();
                        //args.put(tempId, dDrugArr)
                        ArrayList args = new ArrayList();
                        args.add(tempId);
                        args.add(dDrugArr);
                        //отправляем данные в БД
                        PersonDrugDelete personDrugDelete = new PersonDrugDelete();
                        personDrugDelete.execute(args);
                        //удаляем из массива
                        oldDrugArray.remove(index);
                    }
                    //перебираем добавленные лекарства
                    for(String[] aDrugArr : addedDrugArray) {
                        //Добавляем каждое новое лекарство в БД
                        PersonDrugGetId personDrugGetId = new PersonDrugGetId();
                        personDrugGetId.execute(aDrugArr[0]);
                        final PersonDrug personDrug = new PersonDrug();
                        personDrug.temperatureId = tempId;
                        personDrug.drugId = personDrugGetId.get();
                        personDrug.amount = Float.parseFloat(aDrugArr[1]);
                        personDrug.drugUnit = aDrugArr[2];
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                database.personDrugDao().insert(personDrug);
                            }
                        }).start();
                        oldDrugArray.add(aDrugArr);
                    }
                    intent.putExtra(EXTRA_DRUGS, oldDrugArray);
                }
                if((addedSymptArray.size() > 0)||(deletedSymptArray.size() > 0)) {
                    //перебираем удалённые симптомы
                    for(String dSympt : deletedSymptArray) {
                        //проверяем, присутствовал ли симптиом в массиве
                        int index = oldSymptArray.indexOf(dSympt);
                        //if(index == -1) continue;
                        //отправляем данные в БД
                        PersonSymptDelete personSymptDelete = new PersonSymptDelete();
                        personSymptDelete.execute(dSympt);
                        //удаляем из массива
                        oldSymptArray.remove(index);
                    }
                    //перебираем добавленные симптомы
                    for(String aSympt : addedSymptArray) {
                        //Добавляем каждый новый симптом в БД
                        PersonSymptGetId personSymptGetId = new PersonSymptGetId();
                        personSymptGetId.execute(aSympt);

                        final PersonSymptom personSymptom = new PersonSymptom();
                        personSymptom.temperatureId = tempId;
                        personSymptom.symptomId = personSymptGetId.get();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                database.personSymptomDao().insert(personSymptom);
                            }
                        }).start();

                        oldSymptArray.add(aSympt);
                    }
                    intent.putExtra(EXTRA_SYMPTS, oldSymptArray);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            String newNote = focusNote.getText().toString();
            //если заметка обновлялась...
            if(!oldNote.equals(newNote)) {
                //проверяем, создана ли заметка в БД. Создаём или обновляем
                if(oldNote.length() == 0) {
                    final Note note = new Note();
                    note.tempId = tempId;
                    note.note = newNote;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            database.noteDao().insert(note);
                        }
                    }).start();
                } else {
                    NoteDaoGetById noteDaoGetById = new NoteDaoGetById();
                    noteDaoGetById.execute(tempId);
                    try {
                        final Note note = noteDaoGetById.get();
                        note.note = newNote;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                database.noteDao().update(note);
                            }
                        }).start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                //обновляем её в БД
                intent.putExtra(EXTRA_NOTE, focusNote.getText().toString());
            }
            intent.putExtra(EXTRA_TEMPID, tempId);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        super.onBackPressed();
        return true;
    }

    static private class PersonDrugDelete extends AsyncTask<ArrayList, Void, Void> {

        protected Void doInBackground(ArrayList... args) {
            PersonDrug pd = database.personDrugDao().getByTempId((Long) args[0].get(0),
                                                                ((String[])args[0].get(1))[0],
                                                                ((String[])args[0].get(1))[1],
                                                                ((String[])args[0].get(1))[2]);
            database.personDrugDao().delete(pd);
            return null;
        }
    }

    //классы по работе с БД
    static private class PersonDrugGetId extends AsyncTask<String, Void, Long> {

        @Override
        protected Long doInBackground(String... name) {
            return database.drugDao().getId(name[0]);
        }
    }

    static private class PersonSymptDelete extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... name) {
            PersonSymptom ps = database.personSymptomDao().getByTempId(tempId, name[0]);
            database.personSymptomDao().delete(ps);
            return null;
        }
    }

    static private class PersonSymptGetId extends AsyncTask<String, Void, Long> {

        @Override
        protected Long doInBackground(String... name) {
            return database.symptomDao().getId(name[0]);
        }
    }

    static private class NoteDaoGetById extends AsyncTask<Long, Void, Note> {
        @Override
        protected Note doInBackground(Long... id) {
            return database.noteDao().getById(id[0]);
        }
    }
}
