package com.mmo;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.ArrayList;

public class GameCharacter {
    private ArrayList<Texture> textures = new ArrayList<>();
    private Animation<TextureRegion> idleAnim, runAnim, jumpAnim, attackAnim, attack2Anim, superAnim, parryAnim, deathAnim;
    
    public float stateTime = 0f;
    public float attackTimer = 0f;
    public float superTimer = 0f;
    public float parryTimer = 0f;
    
    public float x, y, size;
    
    // --- State Variables ---
    public boolean isMoving = false;
    public boolean isFacingLeft = false;
    public boolean isJumping = false;
    public boolean hasDealtDamage = false; 
    public boolean isDead = false;
    
    public boolean isAttacking = false;
    public boolean isSecondaryAttacking = false;
    public boolean isSuperAttacking = false;
    public boolean isParrying = false;
    
    public float speed = 300f; 
    public float yVelocity = 0f;
    public float gravity = -1800f; 
    public float jumpStrength = 750f; 
    public float floorY; 
    
    public float superCooldownRemaining = 0f;
    public final float SUPER_COOLDOWN = 3.0f;
    public final float PARRY_WINDOW = 0.3f; 

    public float maxHealth = 100f;
    public float currentHealth = 100f;

    public GameCharacter(
            String idlePath, int idleCols,
            String runPath, int runCols,
            String jumpPath, int jumpCols,
            String attackPath, int attackCols,
            String attack2Path, int attack2Cols,
            String superPath, int superCols,
            String parryPath, int parryCols,
            String deathPath, int deathCols, // Added Death Animation
            float startX, float startY, float size) {
        
        this.x = startX;
        this.y = startY;
        this.floorY = startY;
        this.size = size;

        this.idleAnim = createAnimation(idlePath, idleCols, 0.1f);
        this.runAnim = createAnimation(runPath, runCols, 0.1f);
        this.jumpAnim = createAnimation(jumpPath, jumpCols, 0.1f);
        this.attackAnim = createAnimation(attackPath, attackCols, 0.08f);
        this.attack2Anim = createAnimation(attack2Path, attack2Cols, 0.08f);
        this.superAnim = createAnimation(superPath, superCols, 0.15f);
        this.parryAnim = createAnimation(parryPath, parryCols, 0.075f);
        this.deathAnim = createAnimation(deathPath, deathCols, 0.15f);
    }

    private Animation<TextureRegion> createAnimation(String path, int cols, float frameDuration) {
        return createAnimation(path, cols, 1, frameDuration); 
    }

    private Animation<TextureRegion> createAnimation(String path, int cols, int rows, float frameDuration) {
        Texture sheet = new Texture(Gdx.files.internal(path));
        textures.add(sheet);
        TextureRegion[][] tmp = TextureRegion.split(sheet, sheet.getWidth() / cols, sheet.getHeight() / rows);
        TextureRegion[] frames = new TextureRegion[cols];
        for (int i = 0; i < cols; i++) frames[i] = tmp[0][i]; 
        return new Animation<TextureRegion>(frameDuration, frames);
    }

    public void jump() {
        if (!isJumping && !isSuperAttacking && !isParrying && !isDead) {
            isJumping = true;
            yVelocity = jumpStrength;
        }
    }

    public void attack() {
        if (!isAttacking && !isSecondaryAttacking && !isSuperAttacking && !isParrying && !isDead) {
            isAttacking = true;
            attackTimer = 0f; 
            hasDealtDamage = false; 
        }
    }

    public void secondaryAttack() {
        if (!isAttacking && !isSecondaryAttacking && !isSuperAttacking && !isParrying && !isDead) {
            isSecondaryAttacking = true;
            attackTimer = 0f; 
            hasDealtDamage = false; 
        }
    }

    public void superAttack() {
        if (!isSuperAttacking && !isAttacking && !isSecondaryAttacking && !isParrying && superCooldownRemaining <= 0 && !isDead) {
            isSuperAttacking = true;
            superTimer = 0f;
            superCooldownRemaining = SUPER_COOLDOWN;
            hasDealtDamage = false; 
        }
    }

    public void parry() {
        if (!isParrying && !isAttacking && !isSecondaryAttacking && !isSuperAttacking && !isDead) {
            isParrying = true;
            parryTimer = 0f;
        }
    }

    public void takeDamage(float amount) {
        if (isDead) return;
        currentHealth -= amount;
        if (currentHealth <= 0) {
            currentHealth = 0;
            isDead = true;
            stateTime = 0f; // Reset state time so death anim plays from frame 1
        } else {
            isParrying = true; 
            parryTimer = 0f;
        }
    }

    // Dynamic Hitbox: Stretches forward when attacking!
    // Dynamic Hitbox: Stretches forward when attacking!
    public Rectangle getHitbox() {
        float paddingX = size * 0.35f; 
        float paddingY = size * 0.1f;  
        
        if (isAttacking || isSecondaryAttacking || isSuperAttacking) {
            float weaponReach = size * 0.15f; // REDUCED from 0.4f to 0.15f
            if (isFacingLeft) {
                return new Rectangle((x + paddingX) - weaponReach, y + paddingY, (size - (paddingX * 2)) + weaponReach, size - (paddingY * 2));
            } else {
                return new Rectangle(x + paddingX, y + paddingY, (size - (paddingX * 2)) + weaponReach, size - (paddingY * 2));
            }
        }
        
        return new Rectangle(x + paddingX, y + paddingY, size - (paddingX * 2), size - (paddingY * 2));
    }

    public void render(SpriteBatch batch, float deltaTime) {
        stateTime += deltaTime;
        if (superCooldownRemaining > 0) superCooldownRemaining -= deltaTime;
        
        // Always apply gravity so corpses fall to the floor
        if (isJumping || y > floorY) {
            yVelocity += gravity * deltaTime;
            y += yVelocity * deltaTime;
            if (y <= floorY) {
                y = floorY;
                isJumping = false;
                yVelocity = 0f;
            }
        }

        if (isDead) {
            TextureRegion currentFrame = deathAnim.getKeyFrame(stateTime, false); // false = stop on final frame
            if (currentFrame.isFlipX() != isFacingLeft) currentFrame.flip(true, false);
            batch.draw(currentFrame, x, y, size, size);
            return; // Skip living logic
        }

        if (isAttacking || isSecondaryAttacking) {
            attackTimer += deltaTime;
            if (isAttacking && attackAnim.isAnimationFinished(attackTimer)) isAttacking = false;
            if (isSecondaryAttacking && attack2Anim.isAnimationFinished(attackTimer)) isSecondaryAttacking = false;
        }

        if (isSuperAttacking) {
            superTimer += deltaTime;
            if (superAnim.isAnimationFinished(superTimer)) isSuperAttacking = false;
        }

        if (isParrying) {
            parryTimer += deltaTime;
            if (parryTimer >= PARRY_WINDOW) isParrying = false;
        }

        TextureRegion currentFrame;
        if (isParrying) currentFrame = parryAnim.getKeyFrame(parryTimer, false);
        else if (isSuperAttacking) currentFrame = superAnim.getKeyFrame(superTimer, false);
        else if (isAttacking) currentFrame = attackAnim.getKeyFrame(attackTimer, false);
        else if (isSecondaryAttacking) currentFrame = attack2Anim.getKeyFrame(attackTimer, false);
        else if (isJumping) currentFrame = jumpAnim.getKeyFrame(stateTime, true);
        else if (isMoving) currentFrame = runAnim.getKeyFrame(stateTime, true);
        else currentFrame = idleAnim.getKeyFrame(stateTime, true);
        
        if (currentFrame.isFlipX() != isFacingLeft) currentFrame.flip(true, false);
        batch.draw(currentFrame, x, y, size, size);
    }

    public void dispose() {
        for (Texture t : textures) t.dispose();
    }
}