package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.git.NodeUtils;
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
			EXTRA_IS_NEW_FILE = "EXTRA_IS_NEW_FILE";


	@Inject RepositoryManager repositoryManager;
	@InjectView(R.id.title) EditText editText;
	@InjectView(R.id.submit) Button submitButton;

	@Inject NodeUtils nodeUtils;
	private FileManager fileManager;
	private FileNode fileNode; // file currently being edited


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load arguments
		final boolean isNewFile = getIntent().getBooleanExtra(EXTRA_IS_NEW_FILE, false);
		final Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		fileManager = repositoryManager.getFileManager(repository);

		// setup ui
		editText.setMovementMethod(new ScrollingMovementMethod());
		submitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					fileManager.writeFile(fileNode, editText.getText().toString());
				} catch (IOException ioe) {
					Timber.e(ioe, "failed to write file");
					// TODO
				}
				setResult(RESULT_OK);
				finish();
			}
		});

		// load selected file
		fileManager.getTree()
				.compose(new DefaultTransformer<DirNode>())
				.subscribe(new Action1<DirNode>() {
					@Override
					public void call(DirNode rootNode) {
						fileNode = (FileNode) nodeUtils.restoreInstanceState(getIntent().getExtras(), rootNode);
						setTitle(fileNode.getPath());

						if (!isNewFile) {
							compositeSubscription.add(fileManager.getFile(fileNode)
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
				});
	}

}
