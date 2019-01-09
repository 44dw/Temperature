package com.a44dw.temperature;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class AddNote extends AppCompatActivity {

    EditText note;
    public static final String EXTRA_NOTE = "note";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        note = findViewById(R.id.addNoteField);
    }

    public void onClickListener(View view) {
        Intent intent = new Intent();
        String text = note.getText().toString();
        intent.putExtra(EXTRA_NOTE, text);
        setResult(RESULT_OK, intent);
        finish();
    }
}
