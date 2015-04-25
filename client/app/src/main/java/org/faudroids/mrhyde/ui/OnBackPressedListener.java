package org.faudroids.mrhyde.ui;

public interface OnBackPressedListener {

	/**
	 * @return true if the pressed was handled and should not be further
	 * processed, false otherwise.
	 */
	boolean onBackPressed();

}
