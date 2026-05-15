package com.ztype.game.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.ztype.game.ZTypeGame;

public class HtmlLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        return new GwtApplicationConfiguration(900, 1400);
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new ZTypeGame();
    }
}
