package com.mmo;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class Projectile {
    public float x, y;
    public float speed;
    public boolean active;
    public boolean isFacingLeft;
    private float width = 24f;
    private float height = 4f;

    public Projectile(float startX, float startY, boolean facingLeft) {
        this.x = startX;
        this.y = startY;
        this.isFacingLeft = facingLeft;
        
        // Straight line motion (No parabolic gravity arc)
        this.speed = facingLeft ? -900f : 900f; 
        this.active = true;
    }

    public void update(float deltaTime) {
        if (!active) return;
        x += speed * deltaTime;
        
        // Deactivate and clean up if it flies way off-screen
        if (x < -300 || x > 2000) {
            active = false;
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (!active) return;
        // Drawing a neon pink/magenta laser for Ironeye's arrow
        shapeRenderer.setColor(0.9f, 0.2f, 0.8f, 1f); 
        shapeRenderer.rect(x, y, width, height);
    }

    public Rectangle getHitbox() {
        return new Rectangle(x, y, width, height);
    }
}