package org.faudroids.mrhyde.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

public final class ReposFragment extends AbstractFragment {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");

	@Inject RepositoryManager repositoryManager;
	@Inject UiUtils uiUtils;

	@InjectView(R.id.progressbar) ProgressBar progressBar;
	@InjectView(R.id.list) RecyclerView recyclerView;
	private RepositoryAdapter repoAdapter;
	private RecyclerView.LayoutManager layoutManager;


	public ReposFragment() {
		super(R.layout.fragment_repos);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// set title
		actionBarListener.setTitle(getString(R.string.title_repos));

		// setup list
		layoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(layoutManager);
		repoAdapter = new RepositoryAdapter();
		recyclerView.setAdapter(repoAdapter);

		progressBar.setVisibility(View.VISIBLE);
		compositeSubscription.add(repositoryManager.getRepositories()
				.compose(new DefaultTransformer<List<Repository>>())
				.subscribe(new Action1<List<Repository>>() {
					@Override
					public void call(List<Repository> repositories) {
						repoAdapter.setItems(repositories);
						progressBar.setVisibility(View.GONE);
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
						progressBar.setVisibility(View.GONE);
						Timber.e(throwable, "failed to get repos");
					}
				}));
	}


	public class RepositoryAdapter extends RecyclerView.Adapter<RepositoryAdapter.RepoViewHolder> {

		private final List<Repository> repositoryList = new ArrayList<>();


		@Override
		public RepoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_repo, parent, false);
			return new RepoViewHolder(view);
		}


		@Override
		public void onBindViewHolder(RepoViewHolder holder, int position) {
			holder.setRepo(repositoryList.get(position));
		}


		@Override
		public int getItemCount() {
			return repositoryList.size();
		}


		public void setItems(List<Repository> repositoryList) {
			this.repositoryList.clear();
			this.repositoryList.addAll(repositoryList);
			notifyDataSetChanged();
		}


		public class RepoViewHolder extends RecyclerView.ViewHolder {

			private final View view;
			private final ImageView iconView;
			private final TextView titleView, subTitleView;

			public RepoViewHolder(View view) {
				super(view);
				this.view = view;
				this.iconView = (ImageView) view.findViewById(R.id.icon);
				this.titleView = (TextView) view.findViewById(R.id.title);
				this.subTitleView = (TextView) view.findViewById(R.id.subtitle);
			}

			public void setRepo(final Repository repo) {
				Picasso.with(getActivity())
						.load(repo.getOwner().getAvatarUrl())
						.resizeDimen(R.dimen.card_icon_size, R.dimen.card_icon_size)
						.placeholder(R.drawable.octocat)
						.transform(new CircleTransformation())
						.into(iconView);
				titleView.setText(repo.getOwner().getLogin() + "/" + repo.getName());
				subTitleView.setText(getString(R.string.repos_last_update, dateFormat.format(repo.getUpdatedAt())));
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment newFragment = DirFragment.createInstance(repo);
						uiUtils.replaceFragment(ReposFragment.this, newFragment);
					}
				});
			}
		}

	}

}
