package org.faudroids.mrhyde.git;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.inject.Inject;

/**
 * Helper methods for dealing with files.
 */
public class FileUtils {

	private static final int FIRST_FEW_BYTES = 8000;

	@Inject
	FileUtils() { }


	public boolean isImage(String fileName) {
		fileName = fileName.toLowerCase();
		return (fileName.endsWith(".png")
				|| fileName.endsWith(".jpg")
				|| fileName.endsWith(".jpeg")
				|| fileName.endsWith(".bmp")
				|| fileName.endsWith(".gif"));
	}


	/**
	 * Checks if a file is binary by scanning the max first few bytes and searching for a NUL byte.
	 * Courtesy to http://stackoverflow.com/a/6134127
	 */
	public boolean isBinary(File file) throws IOException {
		DataInputStream in = new DataInputStream(new FileInputStream(file));
		int count = 0;
		while (count < FIRST_FEW_BYTES) {
			try {
				byte data = in.readByte();
				if (data == 0) return true;
			} catch (EOFException eof) {
				return false;
			}
			++count;
		}
		return false;
	}

}
