package org.faudroids.mrhyde.ui;

import android.os.Bundle;

import org.faudroids.mrhyde.github.LoginManager;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import roboguice.util.RoboContext;


public class MainDrawerActivity extends AbstractRoboDrawerActivity implements ActionBarListener, RoboContext {

    @Inject LoginManager loginManager;

    @Override
    public void init(Bundle savedInstanceState) {
        addSection(newSection("Repositories", new ReposFragment()));

        LoginManager.Account account = loginManager.getAccount();
        addAccount(new MaterialAccount(
                getResources(),
                account.getLogin(),
                account.getEmail(),
                account.getAvatar(),
                null));
        setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
    }


    @Override
    public void setTitle(String title) {
        getToolbar().setTitle(title);
    }

}
