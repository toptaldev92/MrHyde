package org.faudroids.mrhyde.jekyll;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.git.FileManagerFactory;

import javax.inject.Inject;

public class JekyllManagerFactory {

	private FileManagerFactory fileManagerFactory;

	@Inject
	JekyllManagerFactory(FileManagerFactory fileManagerFactory) {
		this.fileManagerFactory = fileManagerFactory;
	}


	public JekyllManager createJekyllManager(Repository repository) {
		return new JekyllManager(fileManagerFactory.createFileManager(repository));
	}

}
