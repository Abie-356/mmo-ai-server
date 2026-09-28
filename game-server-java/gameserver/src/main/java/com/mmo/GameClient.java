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
import com.badlogic.gdx.files.FileHandle;

// NEW: Video imports
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.mmo.grpc.AIEngineGrpc;
import com.mmo.grpc.GameBridgeProto;

public class GameClient extends ApplicationAdapter {
    
    // --- STATE MACHINE ---
    private enum GameState { INTRO, COMBAT }
    private GameState currentState = GameState.INTRO;
    
    private VideoPlayer videoPlayer;
    private boolean isVideoPlaying = false;

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

        // --- LOAD VIDEO ---
        try {
            videoPlayer = VideoPlayerCreator.createVideoPlayer();
            videoPlayer.play(Gdx.files.internal("assets/intro.webm"));
            isVideoPlaying = true;
            
            // Auto-transition when the evil laugh finishes (Lambda fix)
            videoPlayer.setOnCompletionListener(file -> transitionToCombat());
            
        } catch (Exception e) {
            System.out.println("Warning: Video failed to load. Skipping straight to combat.");
            transitionToCombat();
        }

        // --- LOAD ASSETS ---
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
            "assets/nebuchadnezar/Death.png", 4, 
            30, 130, 150 
        );

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
        boss.displayedHealth = 500f;

        System.out.println("Connecting to Python AI...");
        channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        aiStub = AIEngineGrpc.newBlockingStub(channel);
    }

    private void transitionToCombat() {
        currentState = GameState.COMBAT;
        isVideoPlaying = false;
        if (videoPlayer != null) {
            videoPlayer.dispose();
            videoPlayer = null;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // State Machine Switch
        if (currentState == GameState.INTRO) {
            renderIntro();
        } else {
            renderCombat(deltaTime);
        }
    }

    private void renderIntro() {
        // Allow the player to skip the cinematic by pressing Space or Escape
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            transitionToCombat();
            return;
        }

        if (isVideoPlaying && videoPlayer != null) {
            videoPlayer.update();
            
            // GUARD: If the video completed during the update call, stop here immediately
            if (videoPlayer == null || currentState != GameState.INTRO) return;
            
            Texture frame = videoPlayer.getTexture();
            if (frame != null) {
                batch.begin();
                // Drawing to WORLD dimensions ensures the FitViewport perfectly letterboxes the video
                batch.draw(frame, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
                batch.end();
            }
        }
    }

    private void renderCombat(float deltaTime) {
        // --- 1. Player Input & Boundaries ---
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

        // --- 2. Combat & Hit Detection ---
        boolean isKingSwinging = king.isAttacking || king.isSecondaryAttacking || king.isSuperAttacking;
        if (isKingSwinging && !king.hasDealtDamage && !king.isDead && !boss.isDead) {
            if (king.getHitbox().overlaps(boss.getHurtbox())) {
                king.hasDealtDamage = true; 
                if (king.isSuperAttacking) boss.takeDamage(35f);
                else boss.takeDamage(15f);
            }
        }
        
        boolean isBossSwinging = boss.isAttacking || boss.isSecondaryAttacking;
        if (isBossSwinging && !boss.hasDealtDamage && !boss.isDead && !king.isDead) {
            if (king.isParrying) {
                boss.hasDealtDamage = true; 
            } else if (boss.getHitbox().overlaps(king.getHurtbox())) {
                boss.hasDealtDamage = true; 
                king.takeDamage(10f); 
            }
        }

        // --- 3. High-Speed Network AI ---
        networkTimer += deltaTime;
        if (networkTimer >= 0.4f) { 
            networkTimer = 0f;
            try {
                String currentAction = "Idle";
                if (king.isDead) currentAction = "Dead"; 
                else if (king.isParrying) currentAction = "Parrying";
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
                else if (aiCommand.contains("SECONDARY_ATTACK")) { 
                    boss.isMoving = false; 
                    boss.isFacingLeft = (king.x < boss.x); 
                    boss.secondaryAttack(); 
                }
                else if (aiCommand.contains("ATTACK")) { 
                    boss.isMoving = false; 
                    boss.isFacingLeft = (king.x < boss.x); 
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
        batch.begin();
        batch.draw(bgLayer1, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.draw(bgLayer2, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.draw(bgLayer3, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        
        king.render(batch, deltaTime);
        boss.render(batch, deltaTime);
        
        batch.draw(bgLayer4, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        // --- 5. Health Bar Rendering ---
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        if (!king.isDead) {
            float kX = king.x + (king.size * 0.2f);
            float kY = king.y + (king.size * 0.55f);
            shapeRenderer.setColor(0f, 0f, 0f, 1f);
            shapeRenderer.rect(kX - 2, kY - 2, 104f, 12f);
            shapeRenderer.setColor(1.0f, 0.8f, 0.1f, 1f);
            shapeRenderer.rect(kX, kY, 100f * (king.displayedHealth / king.maxHealth), 8f);
            shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f);
            shapeRenderer.rect(kX, kY, 100f * (king.currentHealth / king.maxHealth), 8f);
        }
        
        if (!boss.isDead) {
            float bX = boss.x + (boss.size * 0.35f);
            float bY = boss.y + (boss.size * 0.55f);
            shapeRenderer.setColor(0f, 0f, 0f, 1f);
            shapeRenderer.rect(bX - 2, bY - 2, 154f, 16f);
            shapeRenderer.setColor(1.0f, 0.8f, 0.1f, 1f);
            shapeRenderer.rect(bX, bY, 150f * (boss.displayedHealth / boss.maxHealth), 12f);
            shapeRenderer.setColor(0.9f, 0.1f, 0.1f, 1f);
            shapeRenderer.rect(bX, bY, 150f * (boss.currentHealth / boss.maxHealth), 12f);
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
        if (videoPlayer != null) videoPlayer.dispose();
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