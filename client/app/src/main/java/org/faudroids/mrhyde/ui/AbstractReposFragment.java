package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.RepositoryManager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.InjectView;

abstract class AbstractReposFragment extends AbstractFragment {

	private static final DateFormat dateFormat = DateFormat.getDateInstance();

	@Inject RepositoryManager repositoryManager;

	@InjectView(R.id.list) RecyclerView recyclerView;
	protected RepositoryAdapter repoAdapter;
	private RecyclerView.LayoutManager layoutManager;


	public AbstractReposFragment() {
		this(R.layout.fragment_repos);
	}


	public AbstractReposFragment(int layoutResource) {
		super(layoutResource);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// setup list
		layoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(layoutManager);
		repoAdapter = new RepositoryAdapter();
		recyclerView.setAdapter(repoAdapter);
		loadRepositories();
	}


	protected abstract void loadRepositories();


	protected void onRepositorySelected(Repository repository) {
		Intent intent = new Intent(AbstractReposFragment.this.getActivity(), DirActivity.class);
		intent.putExtra(DirActivity.EXTRA_REPOSITORY, repository);
		startActivity(intent);
	}


	protected final class RepositoryAdapter extends RecyclerView.Adapter<RepositoryAdapter.RepoViewHolder> {

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


		public void setItems(Collection<Repository> repositoryList) {
			this.repositoryList.clear();
			this.repositoryList.addAll(repositoryList);
			notifyDataSetChanged();
		}


		public class RepoViewHolder extends RecyclerView.ViewHolder {

			private final View containerView;
			private final ImageView iconView;
			private final TextView titleView, subTitleView;

			public RepoViewHolder(View view) {
				super(view);
				this.containerView = view.findViewById(R.id.container);
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
				containerView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onRepositorySelected(repo);
					}
				});
			}
		}

	}

}
