package com.ztype.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("ZType LibGDX Clone");
        config.setWindowedMode(960, 640);
        config.setForegroundFPS(60);
        config.useVsync(true);

        new Lwjgl3Application(new ZTypeGame(), config);
    }
}
