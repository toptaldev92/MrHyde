package org.faudroids.mrhyde.jekyll;

import android.content.Context;
import android.util.Base64;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.AbstractNode;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileData;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.git.NodeUtils;
import org.faudroids.mrhyde.github.LoginManager;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import timber.log.Timber;


/**
 * Unfortunately JGit does not seem to support binary diffs. As a result binary feels need to be
 * sent 'separately' to the preview server (base 64 encoded). This class is responsible for
 * getting the non binary Git diff, loading the binary files and packing everything into
 * a {@link RepoDetails}.
 */
public class RepoDetailsFactory {

	private final String clientSecret;
	private final NodeUtils nodeUtils;
	private final LoginManager loginManager;

	@Inject
	RepoDetailsFactory(Context context, NodeUtils nodeUtils, LoginManager loginManager) {
		this.clientSecret = context.getString(R.string.jekyllServerClientSecret);
		this.nodeUtils = nodeUtils;
		this.loginManager = loginManager;
	}


	public Observable<RepoDetails> createRepoDetails(final FileManager fileManager) {
		return Observable.zip(
				fileManager.getTree(),
				fileManager.getNonBinaryDiff(),
				new Func2<DirNode, String, PartialRepoDetails>() {
					@Override
					public PartialRepoDetails call(final DirNode rootNode, String nonBinaryDiff) {
						return new PartialRepoDetails(rootNode, nonBinaryDiff);
					}
				})
				.flatMap(new Func1<PartialRepoDetails, Observable<RepoDetails>>() {
					@Override
					public Observable<RepoDetails> call(final PartialRepoDetails partialRepoDetails) {
						Timber.d("non binary diff is " + partialRepoDetails.nonBinaryDiff);
						return fileManager.getChangedBinaryFiles()
								.flatMap(new Func1<Set<String>, Observable<RepoDetails>>() {
									@Override
									public Observable<RepoDetails> call(Set<String> binaryFiles) {
										return Observable.from(binaryFiles)
												.flatMap(new Func1<String, Observable<FileData>>() {
													@Override
													public Observable<FileData> call(String binaryFile) {
														Timber.d("reading binary file " + binaryFile);
														AbstractNode node = nodeUtils.getNodeByPath(partialRepoDetails.rootNode, binaryFile);
														if (node instanceof DirNode)
															return Observable.empty();
														else
															return fileManager.readFile((FileNode) node);
													}
												})
												.flatMap(new Func1<FileData, Observable<BinaryFile>>() {
													@Override
													public Observable<BinaryFile> call(FileData data) {
														BinaryFile binaryFile = new BinaryFile(
																data.getFileNode().getFullPath(),
																Base64.encodeToString(data.getData(), Base64.DEFAULT));
														return Observable.just(binaryFile);
													}
												})
												.toList()
												.flatMap(new Func1<List<BinaryFile>, Observable<RepoDetails>>() {
													@Override
													public Observable<RepoDetails> call(List<BinaryFile> binaryFiles) {
														// TODO this is not that great security wise. In the long run use https://help.github.com/articles/git-automation-with-oauth-tokens/
														String cloneUrl = "https://"
																+ loginManager.getAccount().getAccessToken()
																+ ":x-oauth-basic@"
																+ fileManager.getRepository().getCloneUrl().replaceFirst("https://", "");
														return Observable.just(new RepoDetails(
																cloneUrl,
																partialRepoDetails.nonBinaryDiff,
																binaryFiles,
																clientSecret));
													}
												});
									}
								});
					}
				});
	}


	private static class PartialRepoDetails {

		private final DirNode rootNode;
		private final String nonBinaryDiff;

		public PartialRepoDetails(DirNode rootNode, String nonBinaryDiff) {
			this.rootNode = rootNode;
			this.nonBinaryDiff = nonBinaryDiff;
		}

	}
}
