package com.radiohyrule.android.library;

import com.radiohyrule.android.app.BaseFragment;

public class LibraryFragment extends BaseFragment {

	@Override
	public String getTitle() {
		return "Library";
	}
	
	public void switchToView(ViewId viewId) {
		// TODO
	}

	public enum ViewId {
		Albums,
		Artists,
		Songs,
	}
}
