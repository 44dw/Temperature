package com.a44dw.temperature.activities;

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

import com.a44dw.temperature.App;
import com.a44dw.temperature.database.AppDatabase;
import com.a44dw.temperature.entities.Drug;
import com.a44dw.temperature.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class AddDrug extends AppCompatActivity {

    public static final String EXTRA_DRUG = "drug";
    static AppDatabase database;
    ArrayList<String> drugs = new ArrayList<>();
    EditText drugNameField;
    LinearLayout namesHolder;
    EditText amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_drug);

        prepareActionBar();
        initUI();

        database = App.getInstance().getDatabase();

        //запрашиваем список лекарств и "раскидываем" их в соответствующее поле
        new DrugDaoGetNames(this).execute();
    }

    private void initUI() {
        drugNameField = findViewById(R.id.drugName);
        drugNameField.addTextChangedListener(twatcher);
        namesHolder = findViewById(R.id.addDrugHintLayout);
        amount = findViewById(R.id.drugAmount);
    }

    private void prepareActionBar() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }
    }

    public void onClickListener(View view) {
        switch (view.getId()) {
            //при выборе лекарства из списка
            case (R.id.drugSymptName): {
                TextView drug = view.findViewById(R.id.drugSymptName);
                drugNameField.setText(drug.getText().toString());
                amount.requestFocus();
                break;
            }
            case (R.id.addDrugName): {

                if(!checkFieldsIsNotEmpty()) break;

                String drugName = drugNameField.getText().toString().toLowerCase();
                Spinner drugUnits = findViewById(R.id.drugUnits);
                String[] drugArray = {
                        drugName,
                        amount.getText().toString(),
                        drugUnits.getSelectedItem().toString()
                };

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

    private boolean checkFieldsIsNotEmpty() {
        if(drugNameField.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), R.string.emptyDrug, Toast.LENGTH_SHORT).show();
            return false;
        }
        if(amount.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), R.string.emptyAmount, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
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
            for (String drug : drugs) {
                if(drug.startsWith(drugNameField.getText().toString()))
                    drugNameToHolder(drug);
            }
        }
    };

    static private class DrugDaoGetNames extends AsyncTask<Void, Void, ArrayList<String>> {

        WeakReference<AddDrug> wrActivity;

        public DrugDaoGetNames(AddDrug activity) {
            this.wrActivity = new WeakReference<>(activity);
        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            return (ArrayList<String>) database.drugDao().getNames();
        }

        @Override
        protected void onPostExecute(ArrayList<String> list) {
            wrActivity.get().drugs = list;
            for (String drug : list) {
                wrActivity.get().drugNameToHolder(drug);
            }
        }
    }
}