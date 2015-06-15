package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.squareup.picasso.Picasso;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.JekyllManagerFactory;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.utils.ObservableScrollView;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;

@ContentView(R.layout.activity_repo_overview)
public final class RepoOverviewActivity extends AbstractActionBarActivity {

	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();
	private static final Typeface SANS_SERIF_LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);

	public static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	@InjectView(R.id.scroll_view) private ObservableScrollView scrollView;
	@InjectView(R.id.image_overview_background) private ImageView overviewBackgroundImage;
	@InjectView(R.id.image_repo_owner) private ImageView repoOwnerImage;
	@InjectView(R.id.text_post_count) private TextView postDraftCountView;
	@InjectView(R.id.button_favourite) private ImageButton favouriteButton;

	@InjectView(R.id.header_posts) private View postsHeader;
	@InjectView(R.id.list_posts) private ListView postsListView;
	private PostsListAdapter postsListAdapter;
	@InjectView(R.id.item_no_posts) private View noPostsView;

	@InjectView(R.id.card_drafts) private View draftsCard;
	@InjectView(R.id.header_drafts) private View draftsHeader;
	@InjectView(R.id.list_drafts) private ListView draftsListView;
	private DraftsListAdapter draftsListAdapter;

	@InjectView(R.id.card_all_files) private View allFilesView;

	@InjectView(R.id.add) private FloatingActionsMenu addButton;
	@InjectView(R.id.add_post) private FloatingActionButton addPostButton;
	@InjectView(R.id.add_draft) private FloatingActionButton addDraftButton;
	@InjectView(R.id.tint) private View tintView;

	private Repository repository;
	@Inject private JekyllManagerFactory jekyllManagerFactory;
	@Inject private RepositoryManager repositoryManager;
	private JekyllManager jekyllManager;

	@Inject private ActivityIntentFactory intentFactory;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get arguments
		repository = (Repository) this.getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		jekyllManager = jekyllManagerFactory.createJekyllManager(repository);
		setTitle(repository.getName());

		// setup posts lists
		postsListAdapter = new PostsListAdapter(this);
		postsListView.setAdapter(postsListAdapter);

		// setup drafts lists
		draftsListAdapter = new DraftsListAdapter(this);
		draftsListView.setAdapter(draftsListAdapter);

		// load content
		loadJekyllContent();

		// setup posts clicks
		postsHeader.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(RepoOverviewActivity.this, "Dummy", Toast.LENGTH_SHORT).show();
			}
		});

		// setup drafts clicks
		draftsHeader.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(RepoOverviewActivity.this, "Dummy", Toast.LENGTH_SHORT).show();
			}
		});

		// setup all files card
		allFilesView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(RepoOverviewActivity.this, DirActivity.class);
				intent.putExtra(DirActivity.EXTRA_REPOSITORY, repository);
				startActivity(intent);
			}
		});

		// setup add buttons
		addButton.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
			@Override
			public void onMenuExpanded() {
				tintView.animate().alpha(1).setDuration(200).start();
			}

			@Override
			public void onMenuCollapsed() {
				tintView.animate().alpha(0).setDuration(200).start();
			}
		});
		addPostButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
				showNewPostDialog();
			}
		});
		addDraftButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
			}
		});
		tintView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
			}
		});

		// setup scroll partially hides top image
		final Drawable actionBarDrawable = new ColorDrawable(getResources().getColor(R.color.colorPrimary));
		actionBarDrawable.setAlpha(0);
		getSupportActionBar().setBackgroundDrawable(actionBarDrawable);
		scrollView.setOnScrollListener(new ObservableScrollView.OnScrollListener() {
			@Override
			public void onScrollChanged(ScrollView scrollView, int l, int t, int oldL, int oldT) {
				// show action bar color
				final int headerHeight = overviewBackgroundImage.getHeight() - getSupportActionBar().getHeight();
				final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
				final int newAlpha = (int) (ratio * 255);
				actionBarDrawable.setAlpha(newAlpha);

				// resize owner icon
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) repoOwnerImage.getLayoutParams();
				float minSize = getResources().getDimension(R.dimen.overview_owner_icon_size_min);
				float maxSize = getResources().getDimension(R.dimen.overview_owner_icon_size_max);
				float minLeftMargin = getResources().getDimension(R.dimen.overview_owner_icon_margin_left);
				float minTopMargin = getResources().getDimension(R.dimen.overview_owner_icon_margin_top);
				float size = (minSize + (maxSize - minSize) * (1 - ratio));
				params.height = (int) size;
				params.width = (int) size;
				params.leftMargin = (int) (minLeftMargin + (maxSize - size) / 2); // keep left margin stable
				params.topMargin = (int) (minTopMargin + (maxSize - size)); // moves icon down while resizing
				repoOwnerImage.setLayoutParams(params);
			}
		});

		// load owner image
		Picasso.with(this)
				.load(repository.getOwner().getAvatarUrl())
				.resizeDimen(R.dimen.overview_owner_icon_size_max, R.dimen.overview_owner_icon_size_max)
				.placeholder(R.drawable.octocat_black)
				.into(repoOwnerImage);

		// setup favourite button
		if (repositoryManager.isRepositoryFavourite(repository)) {
			favouriteButton.setSelected(true);
		}
		favouriteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (favouriteButton.isSelected()) {
					repositoryManager.unmarkRepositoryAsFavourite(repository);
					favouriteButton.setSelected(false);
					Toast.makeText(RepoOverviewActivity.this, getString(R.string.unmarked_toast), Toast.LENGTH_SHORT).show();
				} else {
					repositoryManager.markRepositoryAsFavourite(repository);
					favouriteButton.setSelected(true);
					Toast.makeText(RepoOverviewActivity.this, getString(R.string.marked_toast), Toast.LENGTH_SHORT).show();
				}
			}
		});
	}


	private void loadJekyllContent() {
		showSpinner();
		compositeSubscription.add(Observable.zip(
				jekyllManager.getAllPosts(),
				jekyllManager.getAllDrafts(),
				new Func2<List<Post>, List<Draft>, JekyllContent>() {
					@Override
					public JekyllContent call(List<Post> posts, List<Draft> drafts) {
						return new JekyllContent(posts, drafts);
					}
				})
				.compose(new DefaultTransformer<JekyllContent>())
				.subscribe(new Action1<JekyllContent>() {
					@Override
					public void call(JekyllContent jekyllContent) {
						hideSpinner();

						// setup header
						postDraftCountView.setText(getString(
								R.string.post_darft_count,
								getResources().getQuantityString(R.plurals.posts_count, jekyllContent.posts.size(), jekyllContent.posts.size()),
								getResources().getQuantityString(R.plurals.drafts_count, jekyllContent.drafts.size(), jekyllContent.drafts.size())));

						// setup cards
						setupFirstThreeEntries(jekyllContent.posts, postsListAdapter);
						setupFirstThreeEntries(jekyllContent.drafts, draftsListAdapter);

						// setup empty views
						if (!jekyllContent.posts.isEmpty()) noPostsView.setVisibility(View.GONE);
						if (jekyllContent.drafts.isEmpty()) draftsCard.setVisibility(View.GONE);


					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(RepoOverviewActivity.this, "failed to load posts"))
						.add(new HideSpinnerAction(RepoOverviewActivity.this))
						.build()));
	}


	private <T> void setupFirstThreeEntries(List<T> items, ArrayAdapter<T> listAdapter) {
		// get first 3 posts
		if (!items.isEmpty()) {
			List<T> firstItems = new ArrayList<>();
			for (int i = 0; i < 3 && i < items.size(); ++i) {
				firstItems.add(items.get(i));
			}
			listAdapter.clear();
			listAdapter.addAll(firstItems);
			listAdapter.notifyDataSetChanged();
		}
	}


	private void showNewPostDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.new_post)
				.setNegativeButton(android.R.string.cancel, null);

		// create custom dialog view
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_new_post, null, false);

		// update filename view when title changes
		final EditText titleView = (EditText) view.findViewById(R.id.input);
		final TextView fileNameView = (TextView) view.findViewById(R.id.text_filename);
		fileNameView.setText(jekyllManager.postTitleToFilename(getString(R.string.your_awesome_title)));
		titleView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void afterTextChanged(Editable s) { }

			@Override
			public void onTextChanged(CharSequence postTitle, int start, int before, int count) {
				fileNameView.setText(jekyllManager.postTitleToFilename(postTitle.toString()));
			}
		});

		// show dialog
		builder
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// open editor with new post file
						// no need for a spinner because content has been downloaded previously
						compositeSubscription.add(jekyllManager.createNewPost(titleView.getText().toString())
								.compose(new DefaultTransformer<Post>())
								.subscribe(new Action1<Post>() {
									@Override
									public void call(Post post) {
										Intent newPostIntent = intentFactory.createTextEditorIntent(repository, post.getFileNode(), true);
										startActivity(newPostIntent);
									}
								}, new ErrorActionBuilder()
										.add(new DefaultErrorAction(RepoOverviewActivity.this, "failed to create post"))
										.build()));
					}
				})
				.setView(view)
				.show();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.repo_overview, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_commit:
				startActivity(intentFactory.createCommitIntent(repository));
				return true;

			case R.id.action_preview:
				startActivity(intentFactory.createPreviewIntent(repository));
				return true;

			case R.id.action_discard_changes:
				new AlertDialog.Builder(this)
						.setTitle(R.string.discard_changes_title)
						.setMessage(R.string.discard_changes_message)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								jekyllManager.resetRepository();
								loadJekyllContent();
							}
						})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}


	/**
	 * List adapter for displaying {@link org.faudroids.mrhyde.jekyll.Post}s.
	 */
	private class PostsListAdapter extends ArrayAdapter<Post> {

		public PostsListAdapter(Context context) {
			super(context, R.layout.item_overview_post);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			final Post post = getItem(position);

			// create view
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.item_overview_post, parent, false);

			// set title
			TextView titleView = (TextView) view.findViewById(R.id.text_title);
			titleView.setText(post.getTitle());
			titleView.setTypeface(SANS_SERIF_LIGHT);

			// set date
			TextView dateView = (TextView) view.findViewById(R.id.text_date);
			dateView.setText(DATE_FORMAT.format(post.getDate()));

			// set on click
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(intentFactory.createTextEditorIntent(repository, post.getFileNode(), false));
				}
			});

			return view;
		}
	}


	/**
	 * List adapter for displaying {@link org.faudroids.mrhyde.jekyll.Draft}s.
	 */
	private class DraftsListAdapter extends ArrayAdapter<Draft> {

		public DraftsListAdapter(Context context) {
			super(context, R.layout.item_overview_draft);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			final Draft draft = getItem(position);

			// create view
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.item_overview_draft, parent, false);

			// set title
			TextView titleView = (TextView) view.findViewById(R.id.text_title);
			titleView.setText(draft.getTitle());
			titleView.setTypeface(SANS_SERIF_LIGHT);

			// set on click
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(intentFactory.createTextEditorIntent(repository, draft.getFileNode(), false));
				}
			});

			return view;
		}
	}


	/**
	 * Container class for holding all loaded Jekyll content
	 */
	private static class JekyllContent {

		private final List<Post> posts;
		private final List<Draft> drafts;

		public JekyllContent(List<Post> posts, List<Draft> drafts) {
			this.posts = posts;
			this.drafts = drafts;
		}

	}

}
