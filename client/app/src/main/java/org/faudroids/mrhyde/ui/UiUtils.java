package org.faudroids.mrhyde.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import org.faudroids.mrhyde.R;

import javax.inject.Inject;


final class UiUtils {

    @Inject
    UiUtils() { }

    protected final void replaceFragment(Fragment currentFragment, Fragment nextFragment) {
		FragmentManager fragmentManager = currentFragment.getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.addToBackStack(null);
        transaction.replace(R.id.fragment_container, nextFragment);
        transaction.commit();
	}

}
