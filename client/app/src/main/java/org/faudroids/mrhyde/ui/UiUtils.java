package org.faudroids.mrhyde.ui;

import android.app.Activity;
import android.app.Fragment;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;


final class UiUtils {

    @Inject
    UiUtils() { }

    @SuppressWarnings("unchecked")
    public void replaceFragment(Fragment currentFragment, Fragment nextFragment) {
        ((MaterialNavigationDrawer<Fragment>) currentFragment.getActivity()).setFragmentChild(nextFragment, "Foobar");
	}


    /**
     * Methods is static as it is required before dependency injection has taken place (yes, hack ...)
     */
    public static ActionBarListener activityToActionBarListener(Activity activity) {
        try {
            return (ActionBarListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + ActionBarListener.class.getName());
        }
    }

}
