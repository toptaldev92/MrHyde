package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.TreeEntry;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.github.ApiWrapper;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;

import roboguice.inject.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public final class FileFragment extends AbstractFragment {

	private static final String
			EXTRA_REPOSITORY = "EXTRA_REPOSITORY",
			EXTRA_TREE_ENTRY = "EXTRA_TREE_ENTRY";

	public static FileFragment createInstance(Repository repository, TreeEntry treeEntry) {
		FileFragment fragment = new FileFragment();
		Bundle extras = new Bundle();
		extras.putSerializable(EXTRA_REPOSITORY, repository);
		extras.putSerializable(EXTRA_TREE_ENTRY, treeEntry);
		fragment.setArguments(extras);
		return fragment;
	}


	@Inject ApiWrapper apiWrapper;
	@InjectView(R.id.text) EditText editText;

	public FileFragment() {
		super(R.layout.fragment_file);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		editText.setMovementMethod(new ScrollingMovementMethod());
		final Repository repository = (Repository) getArguments().getSerializable(EXTRA_REPOSITORY);
		final TreeEntry treeEntry = (TreeEntry) getArguments().getSerializable(EXTRA_TREE_ENTRY);

		apiWrapper.getBlob(repository, treeEntry.getSha())
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<Blob>() {
					@Override
					public void call(Blob blob) {
						editText.setText(parseBlob(blob));
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
						Timber.e(throwable, "failed to get content");
					}
				});
	}


	private String parseBlob(Blob blob) {
		if (blob.getEncoding().equals(Blob.ENCODING_UTF8)) return blob.getContent();

		// base 64 encoded otherwise
		byte[] bytes = Base64.decode(blob.getContent(), Base64.DEFAULT);
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Timber.e(e, "failed to decode blob");
			return e.getMessage();
		}
	}

}
