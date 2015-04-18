package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.TreeEntry;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.RepositoryManager;

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


	@Inject RepositoryManager repositoryManager;
	@InjectView(R.id.text) EditText editText;
	@InjectView(R.id.submit) Button submitButton;

	private FileManager fileManager;
	private Repository repository;
	private TreeEntry treeEntry;

	public FileFragment() {
		super(R.layout.fragment_file);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		repository = (Repository) getArguments().getSerializable(EXTRA_REPOSITORY);
		treeEntry = (TreeEntry) getArguments().getSerializable(EXTRA_TREE_ENTRY);
		fileManager = repositoryManager.getFileManager(repository);

		editText.setMovementMethod(new ScrollingMovementMethod());
		submitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fileManager.writeFile(treeEntry, editText.getText().toString());
				getFragmentManager().popBackStack();
			}
		});

		fileManager.getFile(treeEntry)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<String>() {
					@Override
					public void call(String content) {
						editText.setText(content);
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
						Timber.e(throwable, "failed to get content");
					}
				});
	}

}
