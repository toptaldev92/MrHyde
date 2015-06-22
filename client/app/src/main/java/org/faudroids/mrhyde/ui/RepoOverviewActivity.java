package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import org.faudroids.mrhyde.jekyll.AbstractJekyllContent;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.JekyllManagerFactory;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.utils.JekyllUiUtils;
import org.faudroids.mrhyde.ui.utils.ObservableScrollView;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

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

	public static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	private static final int
			REQUEST_SHOW_LIST = 42,
			REQUEST_SHOW_ALL_FILES = 43;

	@InjectView(R.id.scroll_view) private ObservableScrollView scrollView;
	@InjectView(R.id.image_overview_background) private ImageView overviewBackgroundImage;
	@InjectView(R.id.image_repo_owner) private ImageView repoOwnerImage;
	@InjectView(R.id.text_post_count) private TextView postDraftCountView;
	@InjectView(R.id.button_favourite) private ImageButton favouriteButton;
	private Drawable actionBarDrawable;

	@Inject JekyllUiUtils jekyllUiUtils;
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
		showSpinner();
		loadJekyllContent();

		// setup posts clicks
		postsHeader.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(intentFactory.createPostsIntent(repository), REQUEST_SHOW_LIST);
			}
		});

		// setup drafts clicks
		draftsHeader.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(intentFactory.createDraftsIntent(repository), REQUEST_SHOW_LIST);
			}
		});

		// setup all files card
		allFilesView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(RepoOverviewActivity.this, DirActivity.class);
				intent.putExtra(DirActivity.EXTRA_REPOSITORY, repository);
				startActivityForResult(intent, REQUEST_SHOW_ALL_FILES);
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
				jekyllUiUtils.showNewPostDialog(jekyllManager, repository, new JekyllUiUtils.OnContentCreatedListener<Post>() {
					@Override
					public void onContentCreated(Post post) {
						loadJekyllContent();
					}
				});
			}
		});
		addDraftButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
				jekyllUiUtils.showNewDraftDialog(jekyllManager, repository, new JekyllUiUtils.OnContentCreatedListener<Draft>() {
					@Override
					public void onContentCreated(Draft draft) {
						loadJekyllContent();
					}
				});
			}
		});
		tintView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
			}
		});

		// load owner image
		Picasso.with(this)
				.load(repository.getOwner().getAvatarUrl())
				.resizeDimen(R.dimen.overview_owner_icon_size_max, R.dimen.overview_owner_icon_size_max)
				.placeholder(R.drawable.octocat_black)
				.into(repoOwnerImage);

		// setup scroll partially hides top image
		actionBarDrawable = new ColorDrawable(getResources().getColor(R.color.colorPrimary));
		getSupportActionBar().setBackgroundDrawable(actionBarDrawable);
		scrollView.setOnScrollListener(new ObservableScrollView.OnScrollListener() {
			@Override
			public void onScrollChanged(ScrollView scrollView, int l, int t, int oldL, int oldT) {
				RepoOverviewActivity.this.onScrollChanged();
			}
		});

		// delay first action bar update until after onCreate (image seizges are unknown otherwise)
		overviewBackgroundImage.post(new Runnable() {
			@Override
			public void run() {
				onScrollChanged();
			}
		});

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
						if (isSpinnerVisible()) hideSpinner();
						actionBarDrawable.setAlpha(0); // delay until spinner is hidden
						invalidateOptionsMenu(); // re-enable options menu

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
						else noPostsView.setVisibility(View.VISIBLE);
						if (jekyllContent.drafts.isEmpty()) draftsCard.setVisibility(View.GONE);
						else draftsCard.setVisibility(View.VISIBLE);

					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(RepoOverviewActivity.this, "failed to load posts"))
						.add(new HideSpinnerAction(RepoOverviewActivity.this))
						.build()));
	}


	// updates action bar and repo image during scroll
	private void onScrollChanged() {
		// show action bar color
		final int headerHeight = overviewBackgroundImage.getHeight() - getSupportActionBar().getHeight();
		final float ratio = (float) Math.min(Math.max(scrollView.getScrollY(), 0), headerHeight) / headerHeight;

		final int newAlpha = (int) (ratio * 255);
		actionBarDrawable.setAlpha(newAlpha);

		// resize owner icon
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) repoOwnerImage.getLayoutParams();
		float minSize = getResources().getDimension(R.dimen.overview_owner_icon_size_min);
		float maxSize = getResources().getDimension(R.dimen.overview_owner_icon_size_max);
		float minLeftMargin = getResources().getDimension(R.dimen.overview_owner_icon_margin_left);
		float minTopMargin = getResources().getDimension(R.dimen.overview_owner_icon_margin_top);
		float topMarginAddition = getResources().getDimension(R.dimen.overview_owner_icon_margin_top_addition);
		float size = (minSize + (maxSize - minSize) * (1 - ratio));
		params.height = (int) size;
		params.width = (int) size;
		params.leftMargin = (int) (minLeftMargin + (maxSize - size) / 2); // keep left margin stable
		params.topMargin = (int) (minTopMargin + (maxSize - size) + topMarginAddition * ratio); // moves icon down while resizing
		repoOwnerImage.setLayoutParams(params);
	}


	private <T> void setupFirstThreeEntries(List<T> items, ArrayAdapter<T> listAdapter) {
		// get first 3 posts
		List<T> firstItems = new ArrayList<>();
		for (int i = 0; i < 3 && i < items.size(); ++i) {
			firstItems.add(items.get(i));
		}
		listAdapter.clear();
		listAdapter.addAll(firstItems);
		listAdapter.notifyDataSetChanged();
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			// update posts / drafts list (might have changed)
			case REQUEST_SHOW_LIST:
			case REQUEST_SHOW_ALL_FILES:
				loadJekyllContent();
				break;
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.repo_overview, menu);
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// hide menu during loading
		if (isSpinnerVisible()) {
			menu.findItem(R.id.action_commit).setVisible(false);
			menu.findItem(R.id.action_preview).setVisible(false);
			menu.findItem(R.id.action_discard_changes).setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
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
								showSpinner();
								loadJekyllContent();
							}
						})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private <T> List<T> getAllItemsFromAdapter(ArrayAdapter<T> adapter) {
		List<T> items = new ArrayList<>();
		for (int i = 0; i < adapter.getCount(); ++i) {
			items.add(adapter.getItem(i));
		}
		return items;
	}


	private abstract class AbstractListAdapter<T extends AbstractJekyllContent> extends ArrayAdapter<T> {

		private final int viewResource;

		public AbstractListAdapter(Context context, int viewResource) {
			super(context, viewResource);
			this.viewResource = viewResource;
		}


		public View getView(int position, View convertView, ViewGroup parent) {
			// get item + view
			final T item = getItem(position);
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(viewResource, parent, false);

			// setup click to edit
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(intentFactory.createTextEditorIntent(repository, item.getFileNode(), false));
				}
			});

			doGetView(view, item);
			return view;
		}

		protected abstract void doGetView(View view, T item);
	}


	/**
	 * List adapter for displaying {@link org.faudroids.mrhyde.jekyll.Post}s.
	 */
	private class PostsListAdapter extends AbstractListAdapter<Post> {

		public PostsListAdapter(Context context) {
			super(context, R.layout.item_overview_post);
		}

		@Override
		protected void doGetView(View view, Post post) {
			jekyllUiUtils.setPostOverview(view, post, repository);
		}
	}


	/**
	 * List adapter for displaying {@link org.faudroids.mrhyde.jekyll.Draft}s.
	 */
	private class DraftsListAdapter extends AbstractListAdapter<Draft> {

		public DraftsListAdapter(Context context) {
			super(context, R.layout.item_overview_draft);
		}

		@Override
		protected void doGetView(View view, Draft draft) {
			jekyllUiUtils.setDraftOverview(view, draft, repository);
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
