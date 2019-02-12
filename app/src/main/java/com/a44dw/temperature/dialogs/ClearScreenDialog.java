package com.a44dw.temperature.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.a44dw.temperature.R;

import java.util.Random;

public class ClearScreenDialog extends DialogFragment {

    ClearScreenDialogListener pListener;
    public static final int DIALOG_CLEAR_ALL = 0;
    public static final int DIALOG_CLEAR_BUT = 1;

    public ClearScreenDialog() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        pListener = (ClearScreenDialog.ClearScreenDialogListener)getActivity();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ConstraintLayout holder = (ConstraintLayout) inflater.inflate(R.layout.dialog_clear_screen, null);

        TextView view1 = holder.findViewById(R.id.dialog_clear_all);
        TextView view2 = holder.findViewById(R.id.dialog_clear_but);

        Random random = new Random();

        view1.setBackgroundColor(Color.argb(40, random.nextInt(256), random.nextInt(256), random.nextInt(256)));
        view1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pListener.onDialogPositiveClick(ClearScreenDialog.this, DIALOG_CLEAR_ALL);
                dismiss();
            }
        });

        view2.setBackgroundColor(Color.argb(40, random.nextInt(256), random.nextInt(256), random.nextInt(256)));
        view2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pListener.onDialogPositiveClick(ClearScreenDialog.this, DIALOG_CLEAR_BUT);
                dismiss();
            }
        });

        random = null;

        builder.setView(holder);

        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });


        return builder.create();
    }

    public interface ClearScreenDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, int what);
    }
}
