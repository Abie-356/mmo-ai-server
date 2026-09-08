package com.mmo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class GameCharacter {
    private Texture sheet;
    private Animation<TextureRegion> animation;
    private float stateTime;
    
    // Public variables so the AI can update their positions later
    public float x, y, size;

    public GameCharacter(String texturePath, int cols, int rows, float startX, float startY, float size) {
        this.sheet = new Texture(Gdx.files.internal(texturePath));
        this.x = startX;
        this.y = startY;
        this.size = size;

        // Slice the sprite sheet
        TextureRegion[][] tmp = TextureRegion.split(sheet, sheet.getWidth() / cols, sheet.getHeight() / rows);
        TextureRegion[] frames = new TextureRegion[cols * rows];
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                frames[index++] = tmp[i][j];
            }
        }
        
        this.animation = new Animation<TextureRegion>(0.1f, frames);
        this.stateTime = 0f;
    }

    public void render(SpriteBatch batch, float deltaTime) {
        stateTime += deltaTime;
        TextureRegion currentFrame = animation.getKeyFrame(stateTime, true);
        batch.draw(currentFrame, x, y, size, size);
    }

    public void dispose() {
        if (sheet != null) sheet.dispose();
    }
}