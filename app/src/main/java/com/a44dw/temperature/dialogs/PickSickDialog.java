package com.a44dw.temperature.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.a44dw.temperature.App;
import com.a44dw.temperature.R;
import com.a44dw.temperature.database.AppDatabase;
import com.a44dw.temperature.entities.SickPerson;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class PickSickDialog extends DialogFragment {

    //Ссылка на MainActivity, передаётся через конструктор
    EditText name;
    LayoutInflater inflater;
    LinearLayout holder;
    PickSickDialogListener pListener;
    static AppDatabase database;
    List<SickPerson> personList;

    public PickSickDialog() {}

    //При создании диалога
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        inflater = getActivity().getLayoutInflater();
        holder = addHolder();
        //получаем представление БД и список персон
        if(database == null) database = App.getInstance().getDatabase();
        new SickPersonDaoGetNames(this).execute();
        builder.setView(holder);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });
        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });
        return builder.create();
    }

    //меняет обработчик кнопки ОК, предотвращая диалог от закрытия
    //(onResume вызывается после onCreate, где это сделать невозможно)
    @Override
    public void onResume() {
        super.onResume();
        final AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            Button negativeButton = d.getButton(Dialog.BUTTON_NEGATIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(name.getText().toString().length() == 0) {
                        Toast.makeText(getContext(), "введите имя!", Toast.LENGTH_SHORT).show();
                    } else if(personList.contains(name.getText().toString())) {
                        Toast.makeText(getContext(), "больной с таким именем уже существует!", Toast.LENGTH_SHORT).show();
                    } else {
                        pListener.onDialogPositiveClick(PickSickDialog.this,
                                new SickPerson(name.getText().toString()));
                        d.dismiss();
                    }
                }
            });
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(App.getInstance().getPerson() == null) {
                        Toast.makeText(getContext(), "выберите или введите имя!", Toast.LENGTH_SHORT).show();
                    } else {
                        d.dismiss();
                    }
                }
            });
        }
    }

    private void fillSickPersonHolder(List<SickPerson> sickPeople) {
        this.personList = sickPeople;
        Random random = new Random();
        //создаём TextView и заполняем их именами из списка
        for(final SickPerson person : personList) {
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_sick_name, holder, false);
            layout.setBackgroundColor(Color.argb(40, random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            TextView sickName = layout.findViewById(R.id.sickName);
            sickName.setText(person.getName());
            //Делаем его кликабельным и при нажатии вызываем тот же метод, что и при нажатии на кнопку ОК
            sickName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pListener.onDialogPositiveClick(PickSickDialog.this, person);
                    dismiss();
                }
            });
            //аппендим TextView к holder
            holder.addView(layout, 1);
        }
    }

    private LinearLayout addHolder() {
        LinearLayout holder = (LinearLayout)inflater.inflate(R.layout.dialog_choose_sick, null);
        //после "надувания" интерфейса, запоминаем поле EditText, чтобы затем считать из него данные
        name = holder.findViewById(R.id.dialogNewName);
        return holder;
    }

    public interface PickSickDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, SickPerson person);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        pListener = (PickSickDialog.PickSickDialogListener)getActivity();
    }

    static private class SickPersonDaoGetNames extends AsyncTask<Void, Void, List<SickPerson>> {

        private WeakReference<DialogFragment> wrDialogFragment;

        public SickPersonDaoGetNames(DialogFragment dialogFragment) {
            wrDialogFragment = new WeakReference<>(dialogFragment);
        }

        @Override
        protected List<SickPerson> doInBackground(Void... voids) {
            return database.sickPersonDao().getAll();
        }

        @Override
        protected void onPostExecute(List<SickPerson> sickPeople) {
            ((PickSickDialog) wrDialogFragment.get()).fillSickPersonHolder(sickPeople);
        }
    }
}