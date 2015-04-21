package org.faudroids.mrhyde.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import org.faudroids.mrhyde.R;

import javax.inject.Inject;


final class UiUtils {

    @Inject
    UiUtils() { }

    public void replaceFragment(Fragment currentFragment, Fragment nextFragment) {
		FragmentManager fragmentManager = currentFragment.getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.addToBackStack(null);
        transaction.replace(R.id.fragment_container, nextFragment);
        transaction.commit();
	}


    /**
     * Methods is static as it is required before dependencyinjection has taken place (yes, hack ...)
     */
    public static ActionBarListener activityToActionBarListener(Activity activity) {
        try {
            return (ActionBarListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + ActionBarListener.class.getName());
        }
    }

}
