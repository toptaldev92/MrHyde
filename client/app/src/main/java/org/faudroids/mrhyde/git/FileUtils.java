package org.faudroids.mrhyde.git;

import javax.inject.Inject;

/**
 * Helper methods for dealing with files.
 */
public class FileUtils {

	@Inject
	FileUtils() { }


	public boolean isImage(String fileName) {
		return (fileName.endsWith(".png")
				|| fileName.endsWith(".PNG")
				|| fileName.endsWith(".jpg")
				|| fileName.endsWith(".JPG")
				|| fileName.endsWith(".jpeg")
				|| fileName.endsWith(".JPEG")
				|| fileName.endsWith(".bmp")
				|| fileName.endsWith(".BMP")
				|| fileName.endsWith(".gif")
				|| fileName.endsWith(".GIF"));
	}

}
