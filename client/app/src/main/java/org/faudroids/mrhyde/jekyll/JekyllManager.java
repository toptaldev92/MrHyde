package org.faudroids.mrhyde.jekyll;

import android.content.Context;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.AbstractNode;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileData;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileNode;
import org.roboguice.shaded.goole.common.base.Optional;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Handles Jekyll specific tasks for one repository.
 */
public class JekyllManager {

	private static final String
			DIR_POSTS = "_posts",
			DIR_DRAFTS = "_drafts";

	private static final Pattern
			POST_TITLE_PATTERN = Pattern.compile("(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)(.*)\\..*"),
			DRAFT_TITLE_PATTERN = Pattern.compile("(.*)\\..*");

	private static final DateFormat POST_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	private final Context context;
	private final FileManager fileManager;

	JekyllManager(Context context, FileManager fileManager) {
		this.context = context;
		this.fileManager = fileManager;
	}


	/**
	 * Returns all posts sorted by date with the newest first.
	 */
	public Observable<List<Post>> getAllPosts() {
		return fileManager.getTree()
				.flatMap(new Func1<DirNode, Observable<List<Post>>>() {
					@Override
					public Observable<List<Post>> call(DirNode dirNode) {
						// check if post dir exists
						List<Post> posts = new ArrayList<>();
						if (!dirNode.getEntries().containsKey(DIR_POSTS)) return Observable.just(posts);

						// parse titles
						DirNode postsDir = (DirNode) dirNode.getEntries().get(DIR_POSTS);
						for (AbstractNode postNode : postsDir.getEntries().values()) {
							if (!(postNode instanceof FileNode)) continue;
							Optional<Post> post = parsePost((FileNode) postNode);
							if (post.isPresent()) posts.add(post.get());
						}

						// sort by date
						Collections.sort(posts);
						Collections.reverse(posts);

						return Observable.just(posts);
					}
				});
	}


	/**
	 * Returns all drafts sorted by title.
	 */
	public Observable<List<Draft>> getAllDrafts() {
		return fileManager.getTree()
				.flatMap(new Func1<DirNode, Observable<List<Draft>>>() {
					@Override
					public Observable<List<Draft>> call(DirNode dirNode) {
						// check if drafts dir exists
						List<Draft> drafts = new ArrayList<>();
						if (!dirNode.getEntries().containsKey(DIR_DRAFTS)) return Observable.just(drafts);

						// parse titles
						DirNode draftsDir = (DirNode) dirNode.getEntries().get(DIR_DRAFTS);
						for (AbstractNode draftNode: draftsDir.getEntries().values()) {
							if (!(draftNode instanceof FileNode)) continue;
							Optional<Draft> draft = parseDraftTitle((FileNode) draftNode);
							if (draft.isPresent()) drafts.add(draft.get());
						}

						// sort by title
						Collections.sort(drafts);

						return Observable.just(drafts);
					}
				});
	}


	/**
	 * Converts a jekyll post title to the corresponding jekyll filename.
	 */
	public String postTitleToFilename(String title) {
		if (!title.isEmpty()) title = "-" + title;
		return POST_DATE_FORMAT.format(Calendar.getInstance().getTime()) + draftTitleToFilename(title);
	}


	/**
	 * Converts a jekyll draft title to the corresponding filename.
	 */
	public String draftTitleToFilename(String title) {
		return title.replaceAll(" ", "-") + ".md";
	}


	/**
	 * Creates and returns a new post file (locally).
	 */
	public Observable<Post> createNewPost(final String title) {
		return fileManager.getTree()
				.flatMap(new Func1<DirNode, Observable<Post>>() {
					@Override
					public Observable<Post> call(DirNode rootNode) {
						try {
							// create post file and setup front matter
							FileNode postNode = fileManager.createNewFile(assertDir(rootNode, DIR_POSTS), postTitleToFilename(title));
							setupDefaultFrontMatter(postNode, title);
							return Observable.just(new Post(title, Calendar.getInstance().getTime(), postNode));

						} catch (IOException ioe) {
							return Observable.error(ioe);
						}
					}
				});
	}


	/**
	 * Creates and returns a new draft file (locally).
	 */
	public Observable<Draft> createNewDraft(final String title) {
		return fileManager.getTree()
				.flatMap(new Func1<DirNode, Observable<Draft>>() {
					@Override
					public Observable<Draft> call(DirNode rootNode) {
						try {
							// create draft file and setup front matter
							FileNode draftNode = fileManager.createNewFile(assertDir(rootNode, DIR_DRAFTS), draftTitleToFilename(title));
							setupDefaultFrontMatter(draftNode, title);
							return Observable.just(new Draft(title, draftNode));

						} catch (IOException ioe) {
							return Observable.error(ioe);
						}
					}
				});
	}


	private DirNode assertDir(DirNode rootNode, String dirName) {
		AbstractNode dir = rootNode.getEntries().get(dirName);
		if (dir == null) {
			dir = fileManager.createNewDir(rootNode, dirName);
		}
		return (DirNode) dir;
	}


	private void setupDefaultFrontMatter(FileNode fileNode, String title) throws IOException {
		FileData data = new FileData(fileNode, context.getString(R.string.default_front_matter, title).getBytes());
		fileManager.writeFile(data);
	}



	/**
	 * Removes all local changes and clears all caches..
	 */
	public void resetRepository() {
		fileManager.resetRepository();
	}


	private Optional<Post> parsePost(FileNode node) {
		String fileName = node.getPath();

		// check for match
		Matcher matcher = POST_TITLE_PATTERN.matcher(fileName);
		if (!matcher.matches()) return Optional.absent();

		try {
			// get date
			int year = Integer.valueOf(matcher.group(1));
			int month = Integer.valueOf(matcher.group(2));
			int day = Integer.valueOf(matcher.group(3));
			Calendar calendar = Calendar.getInstance();
			calendar.set(year, month, day);

			// get title
			String title = formatTitle(matcher.group(4));

			return Optional.of(new Post(title, calendar.getTime(), node));

		} catch (NumberFormatException nfe) {
			Timber.w(nfe, "failed to parse post tile \"" + fileName + "\"");
			return Optional.absent();
		}
	}


	private Optional<Draft> parseDraftTitle(FileNode node) {
		// check for match
		Matcher matcher = DRAFT_TITLE_PATTERN.matcher(node.getPath());
		if (!matcher.matches()) return Optional.absent();

		// get title
		String title = formatTitle(matcher.group(1));

		return Optional.of(new Draft(title, node));
	}


	/**
	 * Removes dashes (-) from a title, replaces those with spaces ( ) and capitalizes
	 * the first character.
	 */
	private String formatTitle(String title) {
		title = title.replaceAll("-", " ");
		title = title.trim();
		title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
		return title;
	}

}
