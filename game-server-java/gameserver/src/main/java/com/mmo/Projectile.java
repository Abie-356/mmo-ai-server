package com.mmo;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Projectile {
    public float x, y;
    public float speed = 700f; // Fast arrow speed
    public boolean isActive = true;
    public boolean isFacingLeft;
    
    // Custom arrow hitbox dimensions (much smaller than the 150px King)
    public float width = 50f; 
    public float height = 15f;

    public Projectile(float startX, float startY, float kingSize, boolean isFacingLeft) {
        this.isFacingLeft = isFacingLeft;
        
        // Spawn the arrow perfectly aligned with the character's chest/bow
        this.y = startY + (kingSize * 0.45f);
        
        if (this.isFacingLeft) {
            // Spawn on the left edge of the player and invert speed to fly left
            this.x = startX;
            this.speed = -700f; 
        } else {
            // Spawn on the right edge of the player and fly right
            this.x = startX + (kingSize * 0.6f);
            this.speed = 700f; 
        }
    }

    public void update(float deltaTime) {
        x += speed * deltaTime;
    }

    public Rectangle getHitbox() {
        // Creates a tight collision box so the tip of the arrow has to actually touch the boss
        return new Rectangle(x, y, width, height);
    }

    public void render(SpriteBatch batch, Texture texture) {
        // Use LibGDX's native source coordinates and flip booleans (flipX = isFacingLeft)
        batch.draw(texture, x, y, width, height, 0, 0, texture.getWidth(), texture.getHeight(), isFacingLeft, false);
    }
}