package org.faudroids.mrhyde.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.git.NodeUtils;

import javax.inject.Inject;

/**
 * Helper methods for creating intents to start the various activities.
 * Not static and part of individual activities as to not break the dependency injection chain.
 */
public class ActivityIntentFactory {

	private final Context context;
	private final NodeUtils nodeUtils;

	@Inject
	ActivityIntentFactory(Context context, NodeUtils nodeUtils) {
		this.context = context;
		this.nodeUtils = nodeUtils;
	}


	public Intent createTextEditorIntent(Repository repository, FileNode fileNode, boolean isNewFile) {
		Intent intent = new Intent(context, TextEditorActivity.class);
		Bundle extras = createFileExtras(repository, fileNode);
		extras.putBoolean(TextEditorActivity.EXTRA_IS_NEW_FILE, isNewFile);
		intent.putExtras(extras);
		return intent;
	}


	public Intent createImageViewerIntent(Repository repository, FileNode fileNode) {
		Intent intent = new Intent(context, ImageViewerActivity.class);
		intent.putExtras(createFileExtras(repository, fileNode));
		return intent;
	}


	private Bundle createFileExtras(Repository repository, FileNode fileNode) {
		Bundle extras = new Bundle();
		extras.putSerializable(ImageViewerActivity.EXTRA_REPOSITORY, repository);
		nodeUtils.saveInstanceState(extras, fileNode);
		return extras;
	}

}
