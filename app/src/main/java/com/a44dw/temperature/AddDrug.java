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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class AddDrug extends AppCompatActivity {

    public static final String EXTRA_DRUG = "drug";
    static AppDatabase database;
    ArrayList<String> drugs;
    EditText textField;
    LinearLayout namesHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_drug);
         ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
             bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }
        database = App.getInstance().getDatabase();
        //запрашиваем в потоке список лекарств и "раскидываем" их в соответствующее поле
        DrugDaoGetNames drugDaoGetNames = new DrugDaoGetNames();
        drugDaoGetNames.execute();
        try {
            drugs = drugDaoGetNames.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        textField = findViewById(R.id.drugName);
        textField.addTextChangedListener(twatcher);
        namesHolder = findViewById(R.id.addDrugHintLayout);
        for (String drug : drugs) {
            drugNameToHolder(drug);
        }
    }

    public void onClickListener(View view) {
        EditText amount = findViewById(R.id.drugAmount);
        switch (view.getId()) {
            //при выборе лекарства из списка
            case (R.id.drugSymptName): {
                TextView drug = view.findViewById(R.id.drugSymptName);
                textField.setText(drug.getText().toString());
                amount.requestFocus();
                break;
            }
            case (R.id.addDrugName): {
                EditText drugNameField = findViewById(R.id.drugName);
                if(drugNameField.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "поле \"Лекарство\" не должно быть пустым!", Toast.LENGTH_SHORT).show();
                    break;
                }
                if(amount.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "поле \"Кол-во\" не должно быть пустым!", Toast.LENGTH_SHORT).show();
                    break;
                }
                String drugName = drugNameField.getText().toString().toLowerCase();
                Spinner drugUnits = findViewById(R.id.drugUnits);
                String[] drugArray = {drugName,
                        amount.getText().toString(),
                        drugUnits.getSelectedItem().toString()};

                if (!drugs.contains(drugName)) {
                    final Drug drug = new Drug();
                    drug.name = drugName;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            database.drugDao().insert(drug);
                        }
                    }).start();
                }

                Intent intent = new Intent();
                intent.putExtra(EXTRA_DRUG, drugArray);
                setResult(RESULT_OK, intent);
                finish();
                break;
            }
        }
    }

    public void drugNameToHolder(String drug) {
        LinearLayout line = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.add_drug_sympt_line_inflater, namesHolder, false);
        TextView name = line.findViewById(R.id.drugSymptName);
        name.setText(drug);
        namesHolder.addView(line);
    }

    //При нажатии на кнопку "Назад"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return true;
    }

    //наблюдение за вводом пользователя в поле лекарств
    TextWatcher twatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            namesHolder.removeAllViews();
            String regex = textField.getText().toString() + ".*";
            for (String drug : drugs) {
                if (Pattern.matches(regex, drug)) {
                    drugNameToHolder(drug);
                }
            }
        }
    };

    static private class DrugDaoGetNames extends AsyncTask<Void, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            return (ArrayList<String>) database.drugDao().getNames();
        }
    }
}