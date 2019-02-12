package com.a44dw.temperature.activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.a44dw.temperature.R;

public class Credits extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);
        //редактируем ActionBar:
        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
            //вешаем стрелку назад
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            TextView verNum = findViewById(R.id.verNum);
            verNum.setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case(R.id.mail): {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"elevation1987@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Ти - маленький дневник температуры");
                try{
                    startActivity(Intent.createChooser(intent, "Отправить через..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getApplicationContext(), "Не установлено ни одно почтовое приложение!", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case(R.id.stars): {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.a44dw.temperature"));
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "Не установлен ни один браузер!", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        finish();
        return true;
    }
}
