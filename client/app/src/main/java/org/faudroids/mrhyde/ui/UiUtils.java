package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import javax.inject.Inject;

public class UiUtils {

	private final Context context;

	@Inject
	UiUtils(Context context) {
		this.context = context;
	}


	/**
	 * Creates a simple alert dialog with one {@link EditText} for getting user input.
	 * @param titleResource Title of dialog.
	 * @param messageResource Default text inside the {@link EditText} field.
	 * @param inputListener callback that will be called when user input is ready.
	 */
	public AlertDialog createInputDialog(int titleResource, int messageResource, final OnInputListener inputListener) {
		final EditText inputView = new EditText(context);
		inputView.setInputType(InputType.TYPE_CLASS_TEXT);
		inputView.setText(context.getString(messageResource));
		inputView.setSelectAllOnFocus(true);

		return new AlertDialog.Builder(context)
				.setTitle(titleResource)
				.setView(inputView)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String input = inputView.getText().toString();
						if (inputListener != null) inputListener.onInput(input);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create();
	}


	public interface OnInputListener {
		void onInput(String input);
	}

}
