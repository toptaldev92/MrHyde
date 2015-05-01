package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.github.LoginManager;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;


public class MainDrawerActivity extends AbstractRoboDrawerActivity {

    @Inject LoginManager loginManager;

    @Override
    public void init(Bundle savedInstanceState) {
        //favorite repositories
        addSection(newSection("Repositories", new ReposFragment()));

        //account information
        LoginManager.Account account = loginManager.getAccount();
        addAccount(new MaterialAccount(
                getResources(),
                account.getLogin(),
                account.getEmail(),
                account.getAvatar(),
                null));
        setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);

        //settings and feedback
        this.addBottomSection(newSection("Settings", R.drawable.ic_settings, new SettingsFragment()));

        String address = getString(R.string.feedback_mail_address);
        String subject = getString(
                R.string.feedback_mail_subject,
                getString(R.string.app_name));
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        Intent mailer = Intent.createChooser(intent, getString(R.string.feedback_mail_chooser));
        this.addBottomSection(newSection("Feedback", R.drawable.ic_email, mailer));
    }

}
