package org.faudroids.mrhyde.jekyll;

import android.content.Context;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.git.FileManagerFactory;

import javax.inject.Inject;

public class JekyllManagerFactory {

	private final Context context;
	private final FileManagerFactory fileManagerFactory;

	@Inject
	JekyllManagerFactory(Context context, FileManagerFactory fileManagerFactory) {
		this.context = context;
		this.fileManagerFactory = fileManagerFactory;
	}


	public JekyllManager createJekyllManager(Repository repository) {
		return new JekyllManager(context, fileManagerFactory.createFileManager(repository));
	}

}
