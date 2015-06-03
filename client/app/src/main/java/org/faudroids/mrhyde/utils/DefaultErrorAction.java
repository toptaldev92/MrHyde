package org.faudroids.mrhyde.utils;


import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.faudroids.mrhyde.R;

import java.net.UnknownHostException;

import retrofit.RetrofitError;
import timber.log.Timber;

public class DefaultErrorAction extends AbstractErrorAction {

	private final Context context;
	private final String logMessage;

	public DefaultErrorAction(Context context, String logMessage) {
		this.context = context;
		this.logMessage = logMessage;
	}

	@Override
	protected void doCall(Throwable throwable) {
		if ((throwable instanceof RetrofitError && ((RetrofitError) throwable).getKind().equals(RetrofitError.Kind.NETWORK))
				|| (throwable instanceof UnknownHostException)) {

			// network problems
			Timber.d(throwable, logMessage);
			new AlertDialog.Builder(context)
					.setTitle(R.string.error_network_title)
					.setMessage(R.string.error_network_message)
					.setPositiveButton(android.R.string.ok, null)
					.show();

		} else {
			// default internal message
			Timber.e(throwable, logMessage);
			AlertDialog.Builder builder = new AlertDialog.Builder(context)
					.setTitle(R.string.error_internal_title)
					.setPositiveButton(android.R.string.ok, null);

			LayoutInflater inflater = LayoutInflater.from(context);
			View messageView = inflater.inflate(R.layout.dialog_internal_error, null, false);

			View expandDetailsView = messageView.findViewById(R.id.details_expand);
			final TextView detailsTextView = (TextView) messageView.findViewById(R.id.details);
			detailsTextView.setText(Log.getStackTraceString(throwable));

			expandDetailsView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// toggle details
					if (detailsTextView.getVisibility() == View.GONE) {
						detailsTextView.setVisibility(View.VISIBLE);
					} else {
						detailsTextView.setVisibility(View.GONE);
					}

				}
			});

			builder
					.setView(messageView)
					.show();
		}

	}

}
