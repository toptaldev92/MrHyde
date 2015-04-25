package org.faudroids.mrhyde.ui;

import android.os.Bundle;

import org.faudroids.mrhyde.github.LoginManager;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;


public class MainDrawerActivity extends AbstractRoboDrawerActivity implements ActivityListener {

    @Inject LoginManager loginManager;
    private OnBackPressedListener onBackPressedListener;

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
    public void onBackPressed() {
        if (onBackPressedListener == null || !onBackPressedListener.onBackPressed()) {
            super.onBackPressed();
        }
    }


    @Override
    public void setTitle(String title) {
        getToolbar().setTitle(title);
    }


    @Override
    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.onBackPressedListener = listener;
    }


    @Override
    public void removeOnBackPressedListener() {
        this.onBackPressedListener = null;
    }

}
