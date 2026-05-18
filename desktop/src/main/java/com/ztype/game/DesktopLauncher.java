package com.ztype.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.glutils.HdpiMode;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("ZType LibGDX Clone");
        Graphics.DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        int safeHeight = Math.max(900, displayMode.height - 180);
        int windowHeight = Math.min(1400, safeHeight);
        int windowWidth = Math.max(620, Math.round(windowHeight * (900f / 1400f)));
        config.setWindowedMode(windowWidth, windowHeight);
        config.setWindowPosition((displayMode.width - windowWidth) / 2, (displayMode.height - windowHeight) / 2);
        config.setHdpiMode(HdpiMode.Pixels);
        config.setForegroundFPS(60);
        config.useVsync(true);

        new Lwjgl3Application(new ZTypePcGame(), config);
    }
}
