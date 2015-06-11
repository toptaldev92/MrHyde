package org.faudroids.mrhyde.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.ortiz.touch.TouchImageView;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileData;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.git.NodeUtils;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

@ContentView(R.layout.activity_image_viewer)
public final class ImageViewerActivity extends AbstractActionBarActivity {

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	private static final String STATE_CONTENT = "STATE_CONTENT";

	@InjectView(R.id.image) private TouchImageView imageView;

	@Inject private RepositoryManager repositoryManager;
	@Inject private NodeUtils nodeUtils;
	private FileManager fileManager;
	private FileData fileData; // image currently being viewed


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load arguments
		final Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		fileManager = repositoryManager.getFileManager(repository);

		// load image
		if (savedInstanceState != null && savedInstanceState.getSerializable(STATE_CONTENT) != null) {
			fileData = (FileData) savedInstanceState.getSerializable(STATE_CONTENT);
			setupImage();

		} else {
			showSpinner();
			compositeSubscription.add(fileManager.getTree()
					.flatMap(new Func1<DirNode, Observable<FileData>>() {
						@Override
						public Observable<FileData> call(DirNode rootNode) {
							FileNode node = (FileNode) nodeUtils.restoreInstanceState(getIntent().getExtras(), rootNode);
							return fileManager.readFile(node);
						}
					})
					.compose(new DefaultTransformer<FileData>())
					.subscribe(new Action1<FileData>() {
						@Override
						public void call(FileData file) {
							hideSpinner();
							ImageViewerActivity.this.fileData = file;
							setupImage();
						}
					}, new ErrorActionBuilder()
							.add(new DefaultErrorAction(this, "failed to get image content"))
							.add(new HideSpinnerAction(this))
							.build()));
		}
	}


	private void setupImage() {
		setTitle(fileData.getFileNode().getPath());
		Bitmap bitmap = BitmapFactory.decodeByteArray(fileData.getData(), 0, fileData.getData().length);
		imageView.setImageBitmap(bitmap);
		imageView.setZoom(0.999999f);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(STATE_CONTENT, fileData);
	}

}
