package com.radiohyrule.android.activities;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import com.radiohyrule.android.fragments.dialogs.MessageDialogFragment;

public class Activity extends AppCompatActivity {

    public static final String FRAG_TAG_DIALOG = "dialog";

    private boolean canShowDialog = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPause() {
        super.onPause();
        canShowDialog = false;
    }
    //which is called first is ambiguous
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        canShowDialog = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        canShowDialog = true;
    }


    public void showMessageDialog(String title, String text){
        DialogFragment newFragment = MessageDialogFragment.newInstance(title, text);
        showDialog(newFragment);
    }
    //For symmetry
    public void hideDialog(){
        Fragment prev = getFragmentManager().findFragmentByTag(FRAG_TAG_DIALOG);
        if (prev != null) {
            DialogFragment df = (DialogFragment) prev;
            df.dismissAllowingStateLoss();
        }
    }

    /**
     * Manages the fragment transactions necessary to show a dialog fragment
     */
    public void showDialog(DialogFragment fragment){
        if(!canShowDialog)
            return;
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment prev = fm.findFragmentByTag(FRAG_TAG_DIALOG);
        if (prev != null) {
            ft.remove(prev);
        }
        fragment.show(ft, FRAG_TAG_DIALOG);
    }
}
