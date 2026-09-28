package com.mmo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class GameCharacter {
    // Animations
    public Animation idleAnim, runAnim, jumpAnim;
    public Animation attack1Anim, attack2Anim, attack3Anim;
    public Animation hitAnim, deathAnim;
    
    // Textures (kept to dispose of them properly later and prevent memory leaks)
    private Texture tIdle, tRun, tJump, tAtk1, tAtk2, tAtk3, tHit, tDeath;

    public float stateTime;
    
    // Public variables accessed by GameClient & Python AI
    public float x, y, size, floorY;
    public float speed = 200f;
    public float maxHealth = 100f;
    public float currentHealth = 100f;
    
    // Combat and Movement States
    public boolean isFacingLeft = false;
    public boolean isMoving = false;
    public boolean isAttacking = false;
    public boolean isSecondaryAttacking = false;
    public boolean isSuperAttacking = false;
    public boolean isParrying = false;
    public boolean isDead = false;
    public boolean hasDealtDamage = false;
    
    // Jump physics
    private float verticalVelocity = 0f;
    private final float GRAVITY = -900f;
    private final float JUMP_POWER = 400f;
    private boolean isJumping = false;

    // The giant constructor that accepts all 8 animation files (Required by GameClient)
    public GameCharacter(
        String idlePath, int idleCols,
        String runPath, int runCols,
        String jumpPath, int jumpCols,
        String atk1Path, int atk1Cols,
        String atk2Path, int atk2Cols,
        String atk3Path, int atk3Cols,
        String hitPath, int hitCols,
        String deathPath, int deathCols,
        float startX, float startY, float size) {
        
        this.x = startX;
        this.y = startY;
        this.floorY = startY;
        this.size = size;
        
        // Load textures and create animations
        tIdle = new Texture(Gdx.files.internal(idlePath));
        idleAnim = createAnim(tIdle, idleCols, 1);
        
        tRun = new Texture(Gdx.files.internal(runPath));
        runAnim = createAnim(tRun, runCols, 1);
        
        tJump = new Texture(Gdx.files.internal(jumpPath));
        jumpAnim = createAnim(tJump, jumpCols, 1);
        
        tAtk1 = new Texture(Gdx.files.internal(atk1Path));
        attack1Anim = createAnim(tAtk1, atk1Cols, 1);
        
        tAtk2 = new Texture(Gdx.files.internal(atk2Path));
        attack2Anim = createAnim(tAtk2, atk2Cols, 1);
        
        tAtk3 = new Texture(Gdx.files.internal(atk3Path));
        attack3Anim = createAnim(tAtk3, atk3Cols, 1);
        
        tHit = new Texture(Gdx.files.internal(hitPath));
        hitAnim = createAnim(tHit, hitCols, 1);
        
        tDeath = new Texture(Gdx.files.internal(deathPath));
        deathAnim = createAnim(tDeath, deathCols, 1);
    }

    // Helper method to slice sprite sheets
    private Animation createAnim(Texture sheet, int cols, int rows) {
        TextureRegion[][] tmp = TextureRegion.split(sheet, sheet.getWidth() / cols, sheet.getHeight() / rows);
        TextureRegion[] frames = new TextureRegion[cols * rows];
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                frames[index++] = tmp[i][j];
            }
        }
        return new Animation(0.1f, new com.badlogic.gdx.utils.Array(frames));
    }
    
    // --- COMBAT & MOVEMENT METHODS ---
    public void jump() {
        if (!isJumping && !isDead) {
            verticalVelocity = JUMP_POWER;
            isJumping = true;
        }
    }
    
    public void attack() {
        if (!isAttacking && !isSecondaryAttacking && !isSuperAttacking && !isDead) {
            isAttacking = true;
            stateTime = 0f;
            hasDealtDamage = false;
        }
    }
    
    public void secondaryAttack() {
        if (!isAttacking && !isSecondaryAttacking && !isSuperAttacking && !isDead) {
            isSecondaryAttacking = true;
            stateTime = 0f;
            hasDealtDamage = false;
        }
    }
    
    public void superAttack() {
        if (!isAttacking && !isSecondaryAttacking && !isSuperAttacking && !isDead) {
            isSuperAttacking = true;
            stateTime = 0f;
            hasDealtDamage = false;
        }
    }
    
    public void parry() {
        if (!isParrying && !isDead) {
            isParrying = true;
            stateTime = 0f;
        }
    }
    
    public void takeDamage(float amount) {
        if (isDead) return;
        currentHealth -= amount;
        if (currentHealth <= 0) {
            currentHealth = 0;
            isDead = true;
            stateTime = 0f;
        }
    }
    
    public Rectangle getHitbox() {
        // Shrink the invisible collision box to the center 30% of the sprite
        float hitboxWidth = size * 0.3f;
        float hitboxHeight = size * 0.8f;
        float offsetX = x + (size * 0.35f); 
        
        return new Rectangle(offsetX, y, hitboxWidth, hitboxHeight);
    }
    
    // --- RENDERING & PHYSICS ---
    public void render(SpriteBatch batch, float deltaTime) {
        stateTime += deltaTime;
        
        // Handle Gravity
        if (isJumping) {
            y += verticalVelocity * deltaTime;
            verticalVelocity += GRAVITY * deltaTime;
            if (y <= floorY) {
                y = floorY;
                isJumping = false;
                verticalVelocity = 0;
            }
        }
        
        // Determine which animation to play based on current state priority
        Animation currentAnim = idleAnim;
        boolean looping = true;
        
        if (isDead) {
            currentAnim = deathAnim;
            looping = false;
        } else if (isParrying) {
            currentAnim = hitAnim; // Reuse hit animation for parry visual
            if (currentAnim.isAnimationFinished(stateTime)) isParrying = false;
        } else if (isAttacking) {
            currentAnim = attack1Anim;
            looping = false;
            if (currentAnim.isAnimationFinished(stateTime)) isAttacking = false;
        } else if (isSecondaryAttacking) {
            currentAnim = attack2Anim;
            looping = false;
            if (currentAnim.isAnimationFinished(stateTime)) isSecondaryAttacking = false;
        } else if (isSuperAttacking) {
            currentAnim = attack3Anim;
            looping = false;
            if (currentAnim.isAnimationFinished(stateTime)) isSuperAttacking = false;
        } else if (isJumping) {
            currentAnim = jumpAnim;
        } else if (isMoving) {
            currentAnim = runAnim;
        }
        
        TextureRegion frame = (TextureRegion) currentAnim.getKeyFrame(stateTime, looping);
        
        // Flip the sprite if facing left
        float drawX = x;
        float drawWidth = size;
        if (isFacingLeft) {
            drawX = x + size;
            drawWidth = -size;
        }
        
        batch.draw(frame, drawX, y, drawWidth, size);
    }
    
    // Cleanup memory when game closes
    public void dispose() {
        tIdle.dispose();
        tRun.dispose();
        tJump.dispose();
        tAtk1.dispose();
        tAtk2.dispose();
        tAtk3.dispose();
        tHit.dispose();
        tDeath.dispose();
    }
}