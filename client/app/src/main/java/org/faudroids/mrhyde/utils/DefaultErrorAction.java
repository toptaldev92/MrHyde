package org.faudroids.mrhyde.utils;


import android.app.AlertDialog;
import android.content.Context;

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
			new AlertDialog.Builder(context)
					.setTitle(R.string.error_internal_title)
					.setMessage(R.string.error_internal_message)
					.setPositiveButton(android.R.string.ok, null)
					.show();

		}

	}

}
