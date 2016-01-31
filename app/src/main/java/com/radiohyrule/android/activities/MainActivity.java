package com.radiohyrule.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.radiohyrule.android.BuildConfig;
import com.radiohyrule.android.R;
import com.radiohyrule.android.app.NavigationManager;
import com.radiohyrule.android.fragments.BaseFragment;
import com.radiohyrule.android.player.IPlayer;
import com.radiohyrule.android.player.PlayerServiceClient;
import com.squareup.picasso.Picasso;

public class MainActivity
        extends Activity //todo drop ActionBarSherlock
        implements NavigationManager.NavigationItemChangedListener {

    protected static final String LOG_TAG = MainActivity.class.getCanonicalName();
    public static final String EXTRA_SELECT_NAVIGATION_ITEM_LISTEN = MainActivity.class.getCanonicalName()+".EXTRA_SELECT_NAVIGATION_ITEM_LISTEN";

    // UI

    protected DrawerLayout navigationDrawerLayout;
    protected ListView navigationListView;
    protected ActionBarDrawerToggle navigationDrawerToggle;

    protected NavigationManager navigationManager;

    protected String title;

    // App logic

    protected PlayerServiceClient playerServiceClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // set up the image downloader
        Picasso.with(this).setIndicatorsEnabled(BuildConfig.DEBUG);
        //Picasso.with(this).setLoggingEnabled(true); //Do not leave enabled, not even for debug builds. Local temp use only

        // create service connection
        if (playerServiceClient == null) {
            playerServiceClient = new PlayerServiceClient(this);
        }

        // setup UI

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navigationManager = new NavigationManager(this);
        navigationManager.setNavigationItemChangedListener(this);

        setTitle(getResources().getString(R.string.app_name));
        navigationDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationListView = (ListView) findViewById(R.id.navigation_drawer);

        // set a custom shadow that overlays the main content when the drawer
        // opens
        navigationDrawerLayout.setDrawerShadow(R.drawable.navigation_drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        navigationListView.setAdapter(navigationManager.getListAdapter());
        navigationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) { selectItem(position); }
        });

        // enable ActionBar app icon to behave as action to toggle navigation drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        navigationDrawerToggle = new ActionBarDrawerToggle(this, navigationDrawerLayout,
                R.drawable.ic_navigation_drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            public void onDrawerOpened(View drawerView) {
                updateTitle(null);
            }
            public void onDrawerClosed(View view) {
                updateTitle(title);
            }
        };
        navigationDrawerLayout.setDrawerListener(navigationDrawerToggle);

        // initially activate Listen fragment
        if(savedInstanceState == null) {
            selectItem(navigationManager.getDefaultNavigationItemPosition());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        navigationDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        navigationDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        playerServiceClient.setContext(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();

        if(extras.getBoolean(EXTRA_SELECT_NAVIGATION_ITEM_LISTEN, false)) {
            selectItem(navigationManager.getListenNavigationItemPosition());
        }
    }

    protected void selectItem(int position) {
        // get fragment for selected position
        BaseFragment fragment = navigationManager.prepareFragmentForDisplay(position);
        if(fragment == null) {
            Log.e(LOG_TAG, "no fragment for position " + String.valueOf(position));
            position = navigationManager.getDefaultNavigationItemPosition();
            fragment = navigationManager.prepareFragmentForDisplay(position);

            if(fragment == null) {
                Log.e(LOG_TAG, "no fragment for fallback position " + String.valueOf(position));
                return;
            }
        }

        // set new fragment
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        // set activity title
        setTitle(fragment.getTitle());

        // close drawer
        navigationDrawerLayout.closeDrawer(navigationListView);
    }

    public void setTitle(final String title) {
        if(!this.title.equals(title)) {
            this.title = title;
            runOnUiThread(new Runnable() {
                @Override
                public void run() { updateTitle(title); }
            });
        }
    }

    public void updateTitle(String title) {
        if(getActionBar() != null){
            getActionBar().setTitle(title != null ? title : getString(R.string.app_name));
        }
    }

    public void onTitleChanged(String title) {
        setTitle(title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        if(item.getItemId() == android.R.id.home) {
            if(navigationDrawerLayout.isDrawerOpen(navigationListView)) {
                navigationDrawerLayout.closeDrawer(navigationListView);
            } else {
                navigationDrawerLayout.openDrawer(navigationListView);
            }
        }

        // handle action buttons
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void OnSelectedNavigationItemChanged(int position, BaseFragment fragment) {
        // set activity title
        setTitle(fragment.getTitle());
    }

    public IPlayer getPlayer() { return playerServiceClient; }
}
