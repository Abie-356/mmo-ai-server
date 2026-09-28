package com.mmo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input; 
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.Iterator;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.mmo.grpc.AIEngineGrpc;
import com.mmo.grpc.GameBridgeProto;

public class GameClient extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer; 
    
    // Backgrounds & Boss
    private Texture bgLayer1, bgLayer2, bgLayer3, bgLayer4;
    private GameCharacter boss;

    // --- TEAMMATE CODE (UNTOUCHED) ---
    private GameCharacter king; 

    // --- YOUR SEPARATE IRONEYE CODE ---
    private GameCharacter ironeye;
    private Texture ironeyeArrowTexture;
    private ArrayList ironeyeArrows;

    private OrthographicCamera camera;
    private Viewport viewport;
    private final float WORLD_WIDTH = 1280f;
    private final float WORLD_HEIGHT = 720f;

    private ManagedChannel channel;
    private AIEngineGrpc.AIEngineBlockingStub aiStub;
    private float networkTimer = 0f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer(); 
        
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);

        bgLayer1 = new Texture(Gdx.files.internal("assets/background/background.png"));
        bgLayer2 = new Texture(Gdx.files.internal("assets/background/background2.png"));
        bgLayer3 = new Texture(Gdx.files.internal("assets/background/background3.png"));
        bgLayer4 = new Texture(Gdx.files.internal("assets/background/background4.png"));

        // 1. Teammate's default character (Kept safe for git)
        king = new GameCharacter(
            "assets/nebuchadnezar/Idle.png", 8,
            "assets/nebuchadnezar/Run.png", 8,
            "assets/nebuchadnezar/Jump.png", 2,
            "assets/nebuchadnezar/Attack1.png", 4,
            "assets/nebuchadnezar/Attack2.png", 4,
            "assets/nebuchadnezar/Attack3.png", 4,
            "assets/nebuchadnezar/Take Hit - white silhouette.png", 4,
            "assets/nebuchadnezar/Death.png", 4, 
            30f, 130f, 150f 
        );

        // 2. Your separate Ironeye character initialization function
        initIronEyeCharacter();

        boss = new GameCharacter(
            "assets/necromancer/Idle.png", 8, 
            "assets/necromancer/Run.png", 8,
            "assets/necromancer/Jump.png", 2,
            "assets/necromancer/Attack1.png", 8, 
            "assets/necromancer/Attack2.png", 8,
            "assets/necromancer/Attack1.png", 8, 
            "assets/necromancer/Take hit.png", 3, 
            "assets/necromancer/Death.png", 8,    
            750, 19, 380 
        );
        boss.isFacingLeft = true;
        boss.maxHealth = 500f;
        boss.currentHealth = 500f;

        System.out.println("Connecting to Python AI...");
        channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        aiStub = AIEngineGrpc.newBlockingStub(channel);
    }

    // --- DEDICATED IRONEYE SETUP FUNCTION ---
    private void initIronEyeCharacter() {
        ironeye = new GameCharacter(
            "assets/ironeye/Idle.png", 8,
            "assets/ironeye/Run.png", 8,
            "assets/ironeye/Jump.png", 2,
            "assets/ironeye/Attack1.png", 4,
            "assets/ironeye/Attack2.png", 4,
            "assets/ironeye/Attack3.png", 4,
            "assets/ironeye/Take Hit - while silhouette.png", 4,
            "assets/ironeye/Death.png", 4, 
            30f, 100f, 150f 
        );
        ironeyeArrowTexture = new Texture(Gdx.files.internal("assets/ironeye/projectile.png"));
        ironeyeArrows = new ArrayList();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        
        // --- 1. Ironeye Input & Boundaries ---
        ironeye.isMoving = false; 
        if (!ironeye.isDead) {
            if (Gdx.input.isKeyPressed(Input.Keys.A)) { ironeye.x -= ironeye.speed * deltaTime; ironeye.isMoving = true; ironeye.isFacingLeft = true; }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) { ironeye.x += ironeye.speed * deltaTime; ironeye.isMoving = true; ironeye.isFacingLeft = false; }
            if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) ironeye.jump();
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) ironeye.attack();
            if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) ironeye.secondaryAttack();
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) ironeye.superAttack();
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) ironeye.parry();
        }

        if (ironeye.x < -50) ironeye.x = -50; 
        if (ironeye.x > WORLD_WIDTH - 100) ironeye.x = WORLD_WIDTH - 100;
        
        if (boss.x < -50) boss.x = -50; 
        if (boss.x > WORLD_WIDTH - 250) boss.x = WORLD_WIDTH - 250; 

        // --- 2. Ironeye Combat & Arrow Projectile Logic ---
        boolean isIronEyeSwinging = ironeye.isAttacking || ironeye.isSecondaryAttacking || ironeye.isSuperAttacking;
        if (isIronEyeSwinging && !ironeye.hasDealtDamage && !ironeye.isDead && !boss.isDead) {
            ironeye.hasDealtDamage = true; 
            ironeyeArrows.add(new Projectile(ironeye.x, ironeye.y, ironeye.size, ironeye.isFacingLeft));
        }
        
        Iterator iter = ironeyeArrows.iterator();
        while (iter.hasNext()) {
            Projectile arrow = (Projectile) iter.next();
            arrow.update(deltaTime);

            if (arrow.isActive && !boss.isDead && arrow.getHitbox().overlaps(boss.getHitbox())) {
                if (ironeye.isSuperAttacking) boss.takeDamage(35f);
                else boss.takeDamage(15f);
                
                arrow.isActive = false; 
            }

            if (!arrow.isActive || arrow.x < -200 || arrow.x > WORLD_WIDTH + 200) {
                iter.remove();
            }
        }
        
        boolean isBossSwinging = boss.isAttacking || boss.isSecondaryAttacking;
        if (isBossSwinging && !boss.hasDealtDamage && !boss.isDead && !ironeye.isDead) {
            if (ironeye.isParrying) {
                boss.hasDealtDamage = true; 
            } else if (boss.getHitbox().overlaps(ironeye.getHitbox())) {
                boss.hasDealtDamage = true; 
                ironeye.takeDamage(10f); 
            }
        }

        // --- 3. High-Speed Network AI ---
        networkTimer += deltaTime;
        if (networkTimer >= 0.4f) { 
            networkTimer = 0f;
            try {
                String currentAction = "Idle";
                
                if (ironeye.isDead) currentAction = "Dead"; 
                else if (ironeye.isParrying) currentAction = "Idle"; 
                else if (ironeye.isSuperAttacking) currentAction = "SuperAttacking";
                else if (ironeye.isAttacking || ironeye.isSecondaryAttacking) currentAction = "Attacking";
                else if (ironeye.isMoving) currentAction = "Running";

                GameBridgeProto.GameState state = GameBridgeProto.GameState.newBuilder()
                    .setPlayerId("Ironeye")
                    .setPlayerX(ironeye.x)
                    .setPlayerY(boss.x) 
                    .setAction(currentAction)
                    .build();

                String aiCommand = aiStub.sendState(state).getGeneratedCommand();

                if (aiCommand.contains("MOVE_LEFT")) { boss.isMoving = true; boss.isFacingLeft = true; } 
                else if (aiCommand.contains("MOVE_RIGHT")) { boss.isMoving = true; boss.isFacingLeft = false; } 
                else if (aiCommand.contains("SECONDARY_ATTACK")) { 
                    boss.isMoving = false; 
                    boss.isFacingLeft = (ironeye.x < boss.x); 
                    boss.secondaryAttack(); 
                }
                else if (aiCommand.contains("ATTACK")) { 
                    boss.isMoving = false; 
                    boss.isFacingLeft = (ironeye.x < boss.x); 
                    boss.attack(); 
                }
                else boss.isMoving = false;
                
                if (aiCommand.contains("JUMP")) boss.jump();

            } catch (Exception e) {}
        }

        if (boss.isMoving && !boss.isDead && !boss.isAttacking && !boss.isSecondaryAttacking) {
            float bossSpeed = 160f; 
            if (boss.isFacingLeft) boss.x -= bossSpeed * deltaTime;
            else boss.x += bossSpeed * deltaTime;
        }

        // --- 4. Visual Rendering ---
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(bgLayer1, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.draw(bgLayer2, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.draw(bgLayer3, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        
        // Render your isolated Ironeye character and arrows
        ironeye.render(batch, deltaTime);
        boss.render(batch, deltaTime);
        
        for (int i = 0; i < ironeyeArrows.size(); i++) {
            Projectile arrow = (Projectile) ironeyeArrows.get(i);
            arrow.render(batch, ironeyeArrowTexture);
        }
        
        batch.draw(bgLayer4, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        if (!ironeye.isDead) {
            shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1);
            shapeRenderer.rect(ironeye.x + (ironeye.size * 0.2f), ironeye.y + (ironeye.size * 0.55f), 100f * (ironeye.currentHealth / ironeye.maxHealth), 8);
        }
        
        if (!boss.isDead) {
            shapeRenderer.setColor(0.9f, 0.1f, 0.1f, 1);
            shapeRenderer.rect(boss.x + (boss.size * 0.35f), boss.y + (boss.size * 0.55f), 150f * (boss.currentHealth / boss.maxHealth), 12);
        }
        
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        bgLayer1.dispose();
        bgLayer2.dispose();
        bgLayer3.dispose();
        bgLayer4.dispose();
        ironeyeArrowTexture.dispose();
        if (king != null) king.dispose();
        ironeye.dispose();
        boss.dispose();
        if (channel != null) channel.shutdown();
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("MMO Tactical Arena");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new GameClient(), config);
    }
}