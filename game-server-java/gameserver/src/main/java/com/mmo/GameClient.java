package com.mmo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class GameClient extends ApplicationAdapter {
    private SpriteBatch batch;
    // Variables for your 4 parallax layers
    private Texture bgLayer1, bgLayer2, bgLayer3, bgLayer4;

    @Override
    public void create() {
        batch = new SpriteBatch();
        
        // Load the images directly from your resources/assets folder
        bgLayer1 = new Texture(Gdx.files.internal("assets/background/background.png"));
        bgLayer2 = new Texture(Gdx.files.internal("assets/background/background2.png"));
        bgLayer3 = new Texture(Gdx.files.internal("assets/background/background3.png"));
        bgLayer4 = new Texture(Gdx.files.internal("assets/background/background4.png"));
    }

    @Override
    public void render() {
        // Clear the screen to black before drawing the next frame
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        
        // Draw the layers in order! (Assuming 1 is the sky/back, and 4 is the floor/front)
        // This stretches them to fit the 1280x720 window
        batch.draw(bgLayer1, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.draw(bgLayer2, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.draw(bgLayer3, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.draw(bgLayer4, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        batch.end();
    }

    @Override
    public void dispose() {
        // Prevent memory leaks when closing the game
        batch.dispose();
        bgLayer1.dispose();
        bgLayer2.dispose();
        bgLayer3.dispose();
        bgLayer4.dispose();
    }

    // --- The Window Launcher ---
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("MMO Tactical Arena");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60); // Lock to 60 FPS
        
        System.out.println("Launching 2D Game Client...");
        new Lwjgl3Application(new GameClient(), config);
    }
}