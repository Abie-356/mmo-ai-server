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
    private Texture bgLayer1, bgLayer2, bgLayer3, bgLayer4;

    // Look how simple this is now!
    private GameCharacter king;
    
    // When your teammates add characters, they just add lines like this:
    // private GameCharacter sekiro;
    // private GameCharacter necromancer;

    @Override
    public void create() {
        batch = new SpriteBatch();
        
        bgLayer1 = new Texture(Gdx.files.internal("assets/background/background.png"));
        bgLayer2 = new Texture(Gdx.files.internal("assets/background/background2.png"));
        bgLayer3 = new Texture(Gdx.files.internal("assets/background/background3.png"));
        bgLayer4 = new Texture(Gdx.files.internal("assets/background/background4.png"));

        // Initialize the King (Path, Columns, Rows, X, Y, Size)
        king = new GameCharacter("assets/nebuchadnezar/Idle.png", 8, 1, 30, 130, 150);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        
        // Background
        batch.draw(bgLayer1, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.draw(bgLayer2, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.draw(bgLayer3, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Characters (Pass in the time delta directly to the character)
        king.render(batch, Gdx.graphics.getDeltaTime());

        // Foreground Pillars
        batch.draw(bgLayer4, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        bgLayer1.dispose();
        bgLayer2.dispose();
        bgLayer3.dispose();
        bgLayer4.dispose();
        king.dispose(); // Clean up character memory
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("MMO Tactical Arena");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);
        
        System.out.println("Launching 2D Game Client with Object-Oriented Sprites...");
        new Lwjgl3Application(new GameClient(), config);
    }
}