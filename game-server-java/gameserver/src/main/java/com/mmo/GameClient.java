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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.mmo.grpc.AIEngineGrpc;
import com.mmo.grpc.GameBridgeProto;

public class GameClient extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    
    private Texture bgLayer1, bgLayer2, bgLayer3, bgLayer4;
    private GameCharacter king;
    private GameCharacter boss;

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

        king = new GameCharacter(
            "assets/nebuchadnezar/Idle.png", 8,
            "assets/nebuchadnezar/Run.png", 8,
            "assets/nebuchadnezar/Jump.png", 2,
            "assets/nebuchadnezar/Attack1.png", 4,
            "assets/nebuchadnezar/Attack2.png", 4,
            "assets/nebuchadnezar/Attack3.png", 4,
            "assets/nebuchadnezar/Take Hit - white silhouette.png", 4,
            "assets/nebuchadnezar/Death.png", 4, // ADDED DEATH
            30, 130, 150 
        );

        boss = new GameCharacter(
            "assets/necromancer/Idle.png", 8, 
            "assets/necromancer/Run.png", 8,
            "assets/necromancer/Jump.png", 2,
            "assets/necromancer/Attack1.png", 8, 
            "assets/necromancer/Attack2.png", 8,
            "assets/necromancer/Attack1.png", 8, 
            "assets/necromancer/Take hit.png", 3, // Kept your 3-frame fix!
            "assets/necromancer/Death.png", 8,    // ADDED DEATH (adjust column count if needed)
            750, 19, 380 
        );
        boss.isFacingLeft = true;
        
        boss.maxHealth = 500f;
        boss.currentHealth = 500f;

        System.out.println("Connecting to Python AI...");
        channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        aiStub = AIEngineGrpc.newBlockingStub(channel);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        
        king.isMoving = false; 
        if (!king.isDead) {
            if (Gdx.input.isKeyPressed(Input.Keys.A)) { king.x -= king.speed * deltaTime; king.isMoving = true; king.isFacingLeft = true; }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) { king.x += king.speed * deltaTime; king.isMoving = true; king.isFacingLeft = false; }
            if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) king.jump();
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) king.attack();
            if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) king.secondaryAttack();
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) king.superAttack();
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) king.parry();
        }

        if (king.x < -50) king.x = -50; 
        if (king.x > WORLD_WIDTH - 100) king.x = WORLD_WIDTH - 100;
        
        if (boss.x < -50) boss.x = -50; 
        if (boss.x > WORLD_WIDTH - 250) boss.x = WORLD_WIDTH - 250; 

        // Combat Hit Detection (Now factors in dynamic weapon reach!)
        boolean isKingSwinging = king.isAttacking || king.isSecondaryAttacking || king.isSuperAttacking;
        if (isKingSwinging && !king.hasDealtDamage && !king.isDead && !boss.isDead) {
            if (king.getHitbox().overlaps(boss.getHitbox())) {
                king.hasDealtDamage = true; 
                if (king.isSuperAttacking) boss.takeDamage(35f);
                else boss.takeDamage(15f);
            }
        }
        
        boolean isBossSwinging = boss.isAttacking || boss.isSecondaryAttacking;
        if (isBossSwinging && !boss.hasDealtDamage && !boss.isDead && !king.isDead) {
            if (king.isParrying) {
                boss.hasDealtDamage = true; 
            } else if (boss.getHitbox().overlaps(king.getHitbox())) {
                boss.hasDealtDamage = true; 
                king.takeDamage(10f); 
            }
        }

        networkTimer += deltaTime;
        if (networkTimer >= 0.4f && !boss.isDead) { 
            networkTimer = 0f;
            try {
                String currentAction = "Idle";
                if (king.isParrying) currentAction = "Parrying";
                else if (king.isSuperAttacking) currentAction = "SuperAttacking";
                else if (king.isAttacking || king.isSecondaryAttacking) currentAction = "Attacking";
                else if (king.isMoving) currentAction = "Running";

                GameBridgeProto.GameState state = GameBridgeProto.GameState.newBuilder()
                    .setPlayerId("Nebuchadnezzar")
                    .setPlayerX(king.x)
                    .setPlayerY(boss.x) 
                    .setAction(currentAction)
                    .build();

                String aiCommand = aiStub.sendState(state).getGeneratedCommand();

                if (aiCommand.contains("MOVE_LEFT")) { boss.isMoving = true; boss.isFacingLeft = true; } 
                else if (aiCommand.contains("MOVE_RIGHT")) { boss.isMoving = true; boss.isFacingLeft = false; } 
                else if (aiCommand.contains("ATTACK")) { boss.isMoving = false; boss.attack(); }
                else boss.isMoving = false;
            } catch (Exception e) {}
        }

        if (boss.isMoving && !boss.isDead && !boss.isAttacking) {
            float bossSpeed = 160f; 
            if (boss.isFacingLeft) boss.x -= bossSpeed * deltaTime;
            else boss.x += bossSpeed * deltaTime;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(bgLayer1, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.draw(bgLayer2, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.draw(bgLayer3, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        
        // Characters now render when dead (to show the death animation)
        king.render(batch, deltaTime);
        boss.render(batch, deltaTime);
        
        batch.draw(bgLayer4, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Health bars vanish when dead
        if (!king.isDead) {
            shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1);
            shapeRenderer.rect(king.x + (king.size * 0.2f), king.y + (king.size * 0.55f), 100f * (king.currentHealth / king.maxHealth), 8);
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
        king.dispose();
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