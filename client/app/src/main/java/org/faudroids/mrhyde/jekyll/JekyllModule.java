package org.faudroids.mrhyde.jekyll;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;

import retrofit.RestAdapter;

public class JekyllModule implements Module {

    @Override
    public void configure(Binder binder) {
        // nothing to do for now
    }

    @Provides
    public JekyllApi provideJekyllApi() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://faudroid.markab.uberspace.de")
                .build();
        return restAdapter.create(JekyllApi.class);
    }
}
