package com.radiohyrule.android.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.radiohyrule.android.R;
import com.radiohyrule.android.fragments.HelpAboutFragment;
import com.radiohyrule.android.fragments.NewListenFragment;

import butterknife.Bind;
import butterknife.ButterKnife;

public class NewMainActivity extends Activity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG_LISTEN = "listen";
    public static final String TAG_ABOUT = "about";

    private static final String EXTRA_INITIAL_FRAGMENT_TAG = "MainActivity.INITIAL_FRAG_TAG";

    public static Intent createIntent(Context context, String startTag){
        Intent intent = new Intent(context, NewMainActivity.class);
        intent.putExtra(EXTRA_INITIAL_FRAGMENT_TAG, startTag);
        return intent;
    }

    @Bind(R.id.drawer_layout) DrawerLayout drawerLayout;
    @Bind(R.id.navigation_drawer) NavigationView navigationView;
    @Bind(R.id.toolbar) Toolbar toolbar;

    private String initialFragTag = TAG_LISTEN;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        if(getIntent().hasExtra(EXTRA_INITIAL_FRAGMENT_TAG)) {
            initialFragTag = getIntent().getStringExtra(EXTRA_INITIAL_FRAGMENT_TAG);
        }
        if (savedInstanceState != null) {
            initialFragTag = savedInstanceState.getString(EXTRA_INITIAL_FRAGMENT_TAG, initialFragTag);
        }

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            @Override
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(getCurrentFragment() == null){
            switchToFragment(initialFragTag);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String fragTag = intent.getStringExtra(EXTRA_INITIAL_FRAGMENT_TAG);
        if (fragTag != null) {
            switchToFragment(fragTag);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_item_listen:
                switchToFragment(TAG_LISTEN);
                break;

            case R.id.nav_item_about:
                switchToFragment(TAG_ABOUT);
                break;
        }

        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    public void switchToFragment(String tag) {
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(tag) != null) {
            return; //Don't replace identical fragments
        }

        Fragment newFragment = getFragmentForTag(tag);

        switch (tag) {
            case TAG_LISTEN:
                navigationView.getMenu().findItem(R.id.nav_item_listen).setChecked(true);
                break;
            case TAG_ABOUT:
                navigationView.getMenu().findItem(R.id.nav_item_about).setChecked(true);
                break;
        }

        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.content_frame, newFragment, tag);
        transaction.commitAllowingStateLoss();
        fm.executePendingTransactions();

        //bookkeeping
        updateActionBar();
        drawerLayout.closeDrawers();

    }

    private void updateActionBar() {
        invalidateOptionsMenu();
        ActionBar actionbar = getSupportActionBar();
        if (actionbar == null)
            return;

        actionbar.setTitle(R.string.app_name);

        Fragment currentFragment = getCurrentFragment();
        if (currentFragment != null && currentFragment.getTag().equals(TAG_ABOUT)) {
            actionbar.setTitle(R.string.about);
        }
    }

    @Nullable
    private Fragment getCurrentFragment() {
        return getFragmentManager().findFragmentById(R.id.content_frame);
    }

    private Fragment getFragmentForTag(String tag) {
        Fragment fragment;
        switch (tag) {
            case TAG_LISTEN:
                fragment = new NewListenFragment();
                break;

            case TAG_ABOUT:
            default:
                fragment = HelpAboutFragment.newInstance(R.raw.about_rh);
                break;
        }
        return fragment;
    }

}
