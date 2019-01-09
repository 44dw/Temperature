package com.a44dw.temperature;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class AddSympt extends AppCompatActivity {

    public static final String EXTRA_SYMPT = "symptName";
    static AppDatabase database;
    ArrayList<String> symptoms;
    EditText textField;
    LinearLayout namesHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_sympt);
        //убираем тень и Title...
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
            //вешаем стрелку назад
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }
        //запрашиваем в потоке список симптомов и "раскидываем" их в соответствующее поле
        database = App.getInstance().getDatabase();
        SymptDaoGetNames symptDaoGetNames = new SymptDaoGetNames();
        symptDaoGetNames.execute();
        try {
            symptoms = symptDaoGetNames.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        textField = findViewById(R.id.addSymptField);
        textField.addTextChangedListener(twatcher);
        namesHolder = findViewById(R.id.addSymptHintLayout);
        for (String symptom : symptoms) {
            symptNameToHolder(symptom);
        }
    }

    public void onClickListener(View view) {
        switch (view.getId()) {
            case (R.id.drugSymptName): {
                TextView sympt = view.findViewById(R.id.drugSymptName);
                textField.setText(sympt.getText().toString());
                break;
            }
            case (R.id.addSymptName): {
                TextView field = findViewById(R.id.addSymptField);
                if(field.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "поле \"Симптом\" не должно быть пустым!", Toast.LENGTH_SHORT).show();
                    break;
                }
                String symptName = field.getText().toString().toLowerCase();
                //проверяем, содержится ли элемент в базе, если нет, вставляем его туда
                if(!symptoms.contains(symptName)) {
                    final Symptom symptom = new Symptom();
                    symptom.name = symptName;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            database.symptomDao().insert(symptom);
                        }
                    }).start();
                }
                //отправляем данные в addPoint
                Intent intent = new Intent();
                intent.putExtra(EXTRA_SYMPT, symptName);
                setResult(RESULT_OK, intent);
                finish();
                break;
            }
        }
    }

    //код дублируется в addDrug. Сделать статическим?
    public void symptNameToHolder(String sympt) {
        LinearLayout line = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.add_drug_sympt_line_inflater, namesHolder, false);
        TextView name = line.findViewById(R.id.drugSymptName);
        name.setText(sympt);
        namesHolder.addView(line);
    }

    TextWatcher twatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {
            namesHolder.removeAllViews();
            String regex = textField.getText().toString() + ".*";
            for(String symptom : symptoms) {
                if(Pattern.matches(regex, symptom)) {
                    symptNameToHolder(symptom);
                }
            }
        }
    };

    //При нажатии на кнопку "Назад"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return true;
    }

    static private class SymptDaoGetNames extends AsyncTask<Void, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            return (ArrayList<String>) database.symptomDao().getNames();
        }
    }
}
