package org.faudroids.mrhyde.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.JekyllManagerFactory;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
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

	@InjectView(R.id.header_posts) private View postsHeader;
	@InjectView(R.id.list_posts) private ListView postsListView;
	private PostsListAdapter postsListAdapter;
	@InjectView(R.id.item_no_posts) private View noPostsView;

	@InjectView(R.id.header_drafts) private View draftsHeader;
	@InjectView(R.id.list_drafts) private ListView draftsListView;
	private DraftsListAdapter draftsListAdapter;
	@InjectView(R.id.item_no_drafts) private View noDraftsView;

	@InjectView(R.id.card_all_files) private View allFilesView;

	@InjectView(R.id.add) private FloatingActionsMenu addButton;
	@InjectView(R.id.add_post) private FloatingActionButton addPostButton;
	@InjectView(R.id.add_draft) private FloatingActionButton addDraftButton;
	@InjectView(R.id.tint) private View tintView;

	private Repository repository;
	@Inject private JekyllManagerFactory jekyllManagerFactory;
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
						setupFirstThreeEntries(jekyllContent.posts, postsListAdapter, noPostsView);
						setupFirstThreeEntries(jekyllContent.drafts, draftsListAdapter, noDraftsView);
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(RepoOverviewActivity.this, "failed to load posts"))
						.add(new HideSpinnerAction(RepoOverviewActivity.this))
						.build()));

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
	}



	private <T> void setupFirstThreeEntries(List<T> items, ArrayAdapter<T> listAdapter, View emptyView) {
		// setup list
		if (items.isEmpty()) {
			emptyView.setVisibility(View.VISIBLE);

		} else {
			emptyView.setVisibility(View.GONE);

			// get first 3 posts
			List<T> firstItems = new ArrayList<>();
			for (int i = 0; i < 3 && i < items.size(); ++i) {
				firstItems.add(items.get(i));
			}
			listAdapter.clear();
			listAdapter.addAll(firstItems);
			listAdapter.notifyDataSetChanged();
		}
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
