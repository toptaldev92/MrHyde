package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.github.LoginManager;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import timber.log.Timber;


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
        this.addBottomSection(newSection("Settings", R.drawable.ic_action_settings, new SettingsFragment()));

        String address = getString(R.string.feedback_mail_address);
        String subject = getString(
                R.string.feedback_mail_subject,
                getString(R.string.app_name));
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        Intent mailer = Intent.createChooser(intent, getString(R.string.feedback_mail_chooser));
        this.addBottomSection(newSection("Feedback", R.drawable.ic_action_email, mailer));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.clear:
                try {
                    delete(getFilesDir());
                } catch (IOException e) {
                    Timber.e(e, "failed to delete files");
                    throw new RuntimeException(e);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void delete(File file) throws IOException {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                delete(f);
            }
        }
        if (!file.equals(getFilesDir())) file.delete();
    }

}
