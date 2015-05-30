package org.faudroids.mrhyde.ui;

import android.content.Context;
import android.net.Uri;

import org.faudroids.mrhyde.git.FileData;
import org.faudroids.mrhyde.git.FileNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func0;

public class ImageUtils {

	private final Context context;

	@Inject
	ImageUtils(Context context) {
		this.context = context;
	}


	public Observable<FileData> loadImage(final FileNode fileNode, final Uri imageUri) {
		return Observable.defer(new Func0<Observable<FileData>>() {
			@Override
			public Observable<FileData> call() {
				try {
					// read image
					InputStream imageStream = context.getContentResolver().openInputStream(imageUri);
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int readLength;
					byte[] data = new byte[256];
					while ((readLength = imageStream.read(data, 0, data.length)) != -1) {
						buffer.write(data, 0, readLength);
					}
					buffer.flush();
					return Observable.just(new FileData(fileNode, buffer.toByteArray()));

				} catch (IOException ioe) {
					return Observable.error(ioe);
				}
			}
		});
	}

}
