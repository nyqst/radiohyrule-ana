package com.radiohyrule.android.library;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.radiohyrule.android.R;
import com.radiohyrule.android.app.BaseFragment;
import com.radiohyrule.android.app.NavigationManager;

public class LibraryFragment extends BaseFragment implements OnTabChangeListener {
    protected static final String tagLibraryFragment = "com.radiohyrule.android.library.LibraryFragment";

    protected NavigationManager navigationManager;
    protected List<TabSpec> tabSpecs;
    protected TabHost tabHost;
    protected ViewId currentTabViewId = ViewId._NotSet;

    protected boolean viewCreated = false;

    public LibraryFragment setNavigationManager(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        return this;
    }

    public synchronized View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
        // setup tabs
        tabSpecs = new ArrayList<TabSpec>();
        tabSpecs.add(new TabSpec(ViewId.Albums, "tabAlbums", "Albums", R.id.library_tab_albums));
        tabSpecs.add(new TabSpec(ViewId.Artists, "tabArtists", "Artists", R.id.library_tab_artists));
        tabSpecs.add(new TabSpec(ViewId.Songs, "tabSongs", "Songs", R.id.library_tab_songs));

        // setup tab UI
        View view = inflater.inflate(R.layout.fragment_library, null);
        tabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        tabHost.setup();
        for(TabSpec tab : tabSpecs) {
            tabHost.addTab(tab.createTab(tabHost));
        }
        if(currentTabViewId == ViewId._NotSet)
            currentTabViewId = tabSpecs.get(0).viewId;

        viewCreated = true;

        return view;
    }

    @Override
    public synchronized void onDestroyView() {
        super.onDestroyView();

        tabSpecs = null;
        tabHost = null;
        currentTabViewId = ViewId._NotSet;

        viewCreated = false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);

        tabHost.setOnTabChangedListener(this);
        // manually load first tab
        String tag = getTabSpecByViewId(currentTabViewId).tag;
        tabHost.setCurrentTabByTag(tag);
    }

    @Override
    public String getTitle() {
        return "Library";
    }

    public synchronized void switchToSubview(ViewId viewId) {
        if(viewCreated) {
            TabSpec tab = getTabSpecByViewId(viewId);
            if(tab != null) {
                tabHost.setCurrentTabByTag(tab.tag);
                onTabChanged(tab.tag);
            }
        } else {
            currentTabViewId = viewId;
        }
    }

    @Override
    public void onTabChanged(String tabTag) {
        TabSpec tabSpec = getTabSpecByTag(tabTag);
        if(tabSpec != null) {

            // load tab content
            if(getFragmentManager().findFragmentByTag(tabTag) == null) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(tabSpec.contentViewId, new LibraryTabFragment(), tabTag)
                        .commit();
            }
            currentTabViewId = tabSpec.viewId;

            // inform navigation manager
            if(this.navigationManager != null) {
                this.navigationManager.OnSelectedLibraryChildChanged(this, currentTabViewId);
            }
        }
    }

    protected TabSpec getTabSpecByTag(String tag) {
        for(TabSpec tab : tabSpecs)
            if(tab.tag.equals(tag))
                return tab;
        Log.e(tagLibraryFragment, "no tab spec for tag " + tag);
        return null;
    }

    protected TabSpec getTabSpecByViewId(ViewId viewId) {
        for(TabSpec tab : tabSpecs)
            if(tab.viewId.equals(viewId))
                return tab;
        Log.e(tagLibraryFragment, "no tab spec for view id " + viewId.toString());
        return null;
    }

    public enum ViewId {
        Albums,
        Artists,
        Songs,

        _Count,
        _NotSet,
    }

    protected class TabSpec {
        protected ViewId viewId;
        protected String tag;
        protected CharSequence indicator;
        protected int contentViewId;

        public TabSpec(ViewId viewId, String tag, CharSequence indicator, int contentViewId) {
            this.viewId = viewId;
            this.tag = tag;
            this.indicator = indicator;
            this.contentViewId = contentViewId;
        }

        public TabHost.TabSpec createTab(TabHost tabHost) {
            return tabHost.newTabSpec(tag)
                    .setIndicator(indicator)
                    .setContent(contentViewId);
        }
    }
}
