package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.github.LoginManager;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;


public class MainDrawerActivity extends AbstractRoboDrawerActivity {

    @Inject LoginManager loginManager;
    @Inject RepositoryManager repositoryManager;

    @Override
    public void init(Bundle savedInstanceState) {
        // repositories
        addSection(newSection(getString(R.string.section_favourite_repositories), R.drawable.ic_heart_white, new FavouriteReposFragment()));
        addSection(newSection(getString(R.string.section_all_repositories), R.drawable.ic_list, new AllReposFragment()));

        // show favourites repo per default if not empty
        if (repositoryManager.hasFavouriteRepositories()) setDefaultSectionLoaded(0);
        else setDefaultSectionLoaded(1);

        //account information
        LoginManager.Account account = loginManager.getAccount();
        addAccount(new MaterialAccount(
                getResources(),
                account.getLogin(),
                account.getEmail(),
                account.getAvatar(),
                R.drawable.drawer_background));
        setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);

        //settings and feedback
        this.addBottomSection(newSection(getString(R.string.section_settings), R.drawable.ic_settings, new SettingsFragment()));

        String address = getString(R.string.feedback_mail_address);
        String subject = getString(
                R.string.feedback_mail_subject,
                getString(R.string.app_name));
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        Intent mailer = Intent.createChooser(intent, getString(R.string.feedback_mail_chooser));
        this.addBottomSection(newSection(getString(R.string.section_feedback), R.drawable.ic_email, mailer));
    }

}
