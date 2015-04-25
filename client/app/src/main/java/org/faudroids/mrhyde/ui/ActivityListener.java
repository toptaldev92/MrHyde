package org.faudroids.mrhyde.ui;

/**
 * Listener for fragments to communicate with their parent activity.
 */
public interface ActivityListener {

	void setTitle(String title);
	void setOnBackPressedListener(OnBackPressedListener listener);
	void removeOnBackPressedListener();

}
