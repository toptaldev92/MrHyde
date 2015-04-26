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
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.io.IOException;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

@ContentView(R.layout.activity_file)
public final class FileActivity extends AbstractActionBarActivity {

	static final String
			EXTRA_REPOSITORY = "EXTRA_REPOSITORY",
			EXTRA_TREE_ENTRY = "EXTRA_TREE_ENTRY",
			EXTRA_IS_NEW_FILE = "EXTRA_IS_NEW_FILE";


	@Inject RepositoryManager repositoryManager;
	@InjectView(R.id.title) EditText editText;
	@InjectView(R.id.submit) Button submitButton;

	private FileManager fileManager;
	private Repository repository;
	private TreeEntry treeEntry;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		boolean isNewFile = getIntent().getBooleanExtra(EXTRA_IS_NEW_FILE, false);
		repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		treeEntry = (TreeEntry) getIntent().getSerializableExtra(EXTRA_TREE_ENTRY);
		fileManager = repositoryManager.getFileManager(repository);
		setTitle(treeEntry.getPath());

		editText.setMovementMethod(new ScrollingMovementMethod());
		submitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					fileManager.writeFile(treeEntry, editText.getText().toString());
				} catch (IOException ioe) {
					Timber.e(ioe, "failed to write file");
					// TODO
				}
				setResult(RESULT_OK);
				finish();
			}
		});

		if (!isNewFile) {
			compositeSubscription.add(fileManager.getFile(treeEntry)
					.compose(new DefaultTransformer<String>())
					.subscribe(new Action1<String>() {
						@Override
						public void call(String content) {
							editText.setText(content);
						}
					}, new Action1<Throwable>() {
						@Override
						public void call(Throwable throwable) {
							Toast.makeText(FileActivity.this, "That didn't work, check log", Toast.LENGTH_LONG).show();
							Timber.e(throwable, "failed to get content");
						}
					}));
		}
	}

}
