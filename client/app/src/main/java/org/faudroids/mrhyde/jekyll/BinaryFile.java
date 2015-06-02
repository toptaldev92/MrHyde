package org.faudroids.mrhyde.jekyll;

/**
 * A binary file which can be uploaded to the preview server.
 */
public class BinaryFile {

	private String path;
	private String data;

	public BinaryFile() { }

	public BinaryFile(String path, String data) {
		this.path = path;
		this.data = data;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
