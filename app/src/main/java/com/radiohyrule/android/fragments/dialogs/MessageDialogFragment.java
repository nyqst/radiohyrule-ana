package com.radiohyrule.android.fragments.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;

import com.radiohyrule.android.R;

/**
 * General-purpose dialog fragment for showing simple messages to the user
 */
public class MessageDialogFragment extends DialogFragment {
    private static final String EXTRA_TITLE = "extra_title";
    private static final String EXTRA_TEXT = "extra_text";

    public static MessageDialogFragment newInstance(String title, String text){
        MessageDialogFragment fragment = new MessageDialogFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_TEXT, text);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if(args == null){
            return super.onCreateDialog(savedInstanceState);
        }

        String text = args.getString(EXTRA_TEXT, "");
        String title = args.getString(EXTRA_TITLE, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
        builder.setMessage(text).setTitle(title);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }
}
