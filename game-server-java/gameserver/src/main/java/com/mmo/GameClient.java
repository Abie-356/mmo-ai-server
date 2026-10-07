package com.mmo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input; 
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import com.mmo.grpc.AIEngineGrpc;
import com.mmo.grpc.GameBridgeProto;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator; 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameClient extends ApplicationAdapter {
    
    private enum GameState { CHARACTER_SELECT, INTRO, COMBAT }
    private GameState currentState = GameState.CHARACTER_SELECT;
    
    private VideoPlayer videoPlayer;
    private boolean isVideoPlaying = false;
    private volatile boolean introCompletionPending = false;

    // --- PRE-COMBAT & PHASE CINEMATICS ---
    private float fightTextTimer = 0f;
    private boolean isFightTextActive = false;
    private boolean isVideoPhase = false;
    private float videoPhaseTimer = 0f;
    private boolean bossIsServerDead = false;

    private Texture readyTextTex;
    private Texture fightTextTex;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer; 
    private BitmapFont font;
    
    private Texture bgLayer1, bgLayer2, bgLayer3, bgLayer4;
    private GameCharacter king;
    private GameCharacter boss;

    private Texture bossTexForCover;
    private TextureRegion watermarkCover;
    private boolean finalPhaseComplete = false;

    private OrthographicCamera camera;
    private Viewport viewport;
    private final float WORLD_WIDTH = 1280f;
    private final float WORLD_HEIGHT = 720f;

    private List<Projectile> activeProjectiles = new ArrayList<Projectile>();
    private float attackCooldownTimer = 0f;
    private int currentPhase = 1;

    // --- MULTIPLAYER & ENEMIES ---
    private ManagedChannel channel;
    private AIEngineGrpc.AIEngineStub asyncStub; 
    private StreamObserver<GameBridgeProto.EntityState> requestObserver;
    private Map<String, GameBridgeProto.EntityState> networkPlayers = new ConcurrentHashMap<>();
    
    private Map<String, GameCharacter> activeEnemies = new ConcurrentHashMap<>();
    private Map<String, Float> enemyTargetX = new ConcurrentHashMap<>();

    private float networkTimer = 0f;
    private boolean aiConnectionWarningShown = false;
    private float targetBossX = 750f; 

    public static class CharacterProfile {
        public String name, idlePath, runPath, attackPath;
        public int idleFrames;
        public float startX, startY, scaleSize;
        public Texture previewTexture;
        public TextureRegion previewRegion;

        public CharacterProfile(String name, String idlePath, int idleFrames, String runPath, String attackPath, float startX, float startY, float scaleSize) {
            this.name = name; this.idlePath = idlePath; this.idleFrames = idleFrames; this.runPath = runPath; this.attackPath = attackPath;
            this.startX = startX; this.startY = startY; this.scaleSize = scaleSize;
            this.previewTexture = new Texture(Gdx.files.internal(idlePath));
            TextureRegion[][] tmp = TextureRegion.split(previewTexture, previewTexture.getWidth() / idleFrames, previewTexture.getHeight());
            this.previewRegion = tmp[0][0];
        }
        public void dispose() { if (previewTexture != null) previewTexture.dispose(); }
    }

    private List<CharacterProfile> roster = new ArrayList<CharacterProfile>();
    private int selectedIndex = 0;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer(); 
        font = new BitmapFont(); 
        
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);

        roster.add(new CharacterProfile("King Nebuchadnezzar", "assets/nebuchadnezar/Idle.png", 8, "assets/nebuchadnezar/Run.png", "assets/nebuchadnezar/Attack1.png", 30, 130, 150));
        roster.add(new CharacterProfile("Ironeye", "assets/ironeye/Idle.png", 8, "assets/ironeye/Run.png", "assets/ironeye/Attack1.png", 30, 100, 150));
        roster.add(new CharacterProfile("Sekiro", "assets/sekiro/Idle.png", 8, "assets/sekiro/Run.png", "assets/sekiro/Attack1.png", 30, 46, 250));

        bossTexForCover = new Texture(Gdx.files.internal("assets/necromancer/Idle.png"));
        TextureRegion[][] splitTex = TextureRegion.split(bossTexForCover, bossTexForCover.getWidth() / 8, bossTexForCover.getHeight());
        watermarkCover = splitTex[0][0];

        bgLayer1 = new Texture(Gdx.files.internal("assets/background/background.png"));
        bgLayer2 = new Texture(Gdx.files.internal("assets/background/background2.png"));
        bgLayer3 = new Texture(Gdx.files.internal("assets/background/background3.png"));
        bgLayer4 = new Texture(Gdx.files.internal("assets/background/background4.png"));

        boss = new GameCharacter(
            "assets/necromancer/Idle.png", 8, "assets/necromancer/Run.png", 8, "assets/necromancer/Jump.png", 2,
            "assets/necromancer/Attack1.png", 8, "assets/necromancer/Attack2.png", 8, "assets/necromancer/Attack1.png", 8, 
            "assets/necromancer/Take hit.png", 3, "assets/necromancer/Death.png", 7, 750, 19, 380 
        );
        boss.isFacingLeft = true; boss.maxHealth = 15f; boss.currentHealth = 15f; boss.displayedHealth = 15f;

        channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        asyncStub = AIEngineGrpc.newStub(channel); 

        try {
            readyTextTex = new Texture(Gdx.files.internal("assets/ready_text.png"));
            fightTextTex = new Texture(Gdx.files.internal("assets/fight_text.png"));
        } catch (Exception e) {
            System.err.println("Warning: Missing cinematic text PNGs in assets folder.");
        }
    }

    private GameCharacter createEnemy(String type, float x, float y) {
        String idle = "", run = "", jump = "", atk1 = "", atk2 = "", atk3 = "", hurt = "", death = "";
        int iF = 8, rF = 8, jF = 8, a1F = 8, a2F = 8, a3F = 8, hF = 8, dF = 8; 
        float eSize = 100f, eSpeed = 100f;

        switch (type) {
            case "Bat":
                idle = "assets/Bat/fly.png"; iF = 11;
                run = "assets/Bat/fly.png"; rF = 11;
                jump = "assets/Bat/fall.png"; jF = 5;
                atk1 = "assets/Bat/attack.png"; a1F = 12;
                atk2 = "assets/Bat/attack.png"; a2F = 12;
                atk3 = "assets/Bat/attack.png"; a3F = 12;
                hurt = "assets/Bat/hurt.png"; hF = 3;
                death = "assets/Bat/death.png"; dF = 4;
                eSize = 120f; eSpeed = 120f;
                break;
            case "Slime":
                idle = "assets/Slime/idle.png"; iF = 14;
                run = "assets/Slime/walk.png"; rF = 6;
                jump = "assets/Slime/idle.png"; jF = 14; 
                atk1 = "assets/Slime/attack.png"; a1F = 19;
                atk2 = "assets/Slime/attack.png"; a2F = 19;
                atk3 = "assets/Slime/attack.png"; a3F = 19;
                hurt = "assets/Slime/hurt.png"; hF = 3;
                death = "assets/Slime/death.png"; dF = 11;
                eSize = 150f; eSpeed = 120f;
                break;
            case "Rat":
                idle = "assets/Rat/idle.png"; iF = 10;
                run = "assets/Rat/run.png"; rF = 8;
                jump = "assets/Rat/idle.png"; jF = 10; 
                atk1 = "assets/Rat/attack_bite.png"; a1F = 12;
                atk2 = "assets/Rat/attack_bite.png"; a2F = 12;
                atk3 = "assets/Rat/attack_bite.png"; a3F = 12;
                hurt = "assets/Rat/hurt.png"; hF = 3;
                death = "assets/Rat/rat-death.png"; dF = 6;
                eSize = 90f; eSpeed = 150f;
                break;
            case "Mimic":
                idle = "assets/Mimic/idle_closed.png"; iF = 1;
                run = "assets/Mimic/walk.png"; rF = 6;
                jump = "assets/Mimic/idle_closed.png"; jF = 1; 
                atk1 = "assets/Mimic/attack_1.png"; a1F = 14;
                atk2 = "assets/Mimic/attack_2.png"; a2F = 13;
                atk3 = "assets/Mimic/attack_1.png"; a3F = 14;
                hurt = "assets/Mimic/hurt.png"; hF = 3;
                death = "assets/Mimic/death.png"; dF = 6;
                eSize = 160f; eSpeed = 70f;
                break;
        }

        GameCharacter e = new GameCharacter(
            idle, iF, run, rF, jump, jF,
            atk1, a1F, atk2, a2F, atk3, a3F,
            hurt, hF, death, dF, x, y, eSize
        );
        e.speed = eSpeed; e.maxHealth = 100f; e.currentHealth = 100f; e.displayedHealth = 100f;
        return e;
    }

    private void initializeSelectedPlayerAndStartVideo() {
        CharacterProfile profile = roster.get(selectedIndex);
        
        if (profile.name.equals("Ironeye")) {
            king = new GameCharacter(profile.idlePath, profile.idleFrames, profile.runPath, 8, "assets/ironeye/Jump.png", 2, profile.attackPath, 4, profile.attackPath, 4, profile.attackPath, 4, "assets/ironeye/Take Hit.png", 4, "assets/ironeye/Death.png", 4, profile.startX, profile.startY, profile.scaleSize);
        } else if (profile.name.equals("Sekiro")) {
            king = new GameCharacter(profile.idlePath, profile.idleFrames, profile.runPath, 8, "assets/sekiro/Jump.png", 2, profile.attackPath, 6, "assets/sekiro/Attack2.png", 6, "assets/sekiro/Attack2.png", 6, "assets/sekiro/Take Hit.png", 4, "assets/sekiro/Death.png", 6, profile.startX, profile.startY, profile.scaleSize);
        } else {
            king = new GameCharacter(profile.idlePath, profile.idleFrames, profile.runPath, 8, "assets/nebuchadnezar/Jump.png", 2, profile.attackPath, 4, "assets/nebuchadnezar/Attack2.png", 4, "assets/nebuchadnezar/Attack3.png", 4, "assets/nebuchadnezar/Take Hit - white silhouette.png", 4, "assets/nebuchadnezar/Death.png", 6, profile.startX, profile.startY, profile.scaleSize);
        }

        currentState = GameState.INTRO;
        try {
            videoPlayer = VideoPlayerCreator.createVideoPlayer();
            videoPlayer.play(Gdx.files.internal("assets/intro.webm"));
            isVideoPlaying = true;
            videoPlayer.setOnCompletionListener(file -> introCompletionPending = true);
        } catch (Exception e) {
            transitionToCombat();
        }
    }

    private void transitionToCombat() {
        currentState = GameState.COMBAT;
        isVideoPlaying = false;
        if (videoPlayer != null) { videoPlayer.dispose(); videoPlayer = null; }

        if (readyTextTex != null && fightTextTex != null) {
            isFightTextActive = true;
            fightTextTimer = 3.0f;
        } else {
            isFightTextActive = false;
        }
        
        startMultiplayerStream(); 
    }

    private void startMultiplayerStream() {
        requestObserver = asyncStub.joinLobby(new StreamObserver<GameBridgeProto.GameWorldUpdate>() {
            @Override
            public void onNext(GameBridgeProto.GameWorldUpdate update) {
                // 1. READ PLAYERS
                for (GameBridgeProto.EntityState p : update.getPlayersList()) {
                    if (!p.getId().equals("Abishek")) networkPlayers.put(p.getId(), p);
                }
                
                // 2. READ DEDICATED ENEMIES STREAM
                for (GameBridgeProto.EntityState p : update.getEnemiesList()) {
                    if (!activeEnemies.containsKey(p.getId())) {
                        // FIX: Force OpenGL to load textures on the Main Thread
                        Gdx.app.postRunnable(() -> {
                            if (!activeEnemies.containsKey(p.getId())) {
                                GameCharacter enemy = createEnemy(p.getCharacter(), p.getX(), p.getY());
                                activeEnemies.put(p.getId(), enemy);
                                enemyTargetX.put(p.getId(), p.getX());
                            }
                        });
                    } else {
                        GameCharacter e = activeEnemies.get(p.getId());
                        
                        // FIX: Block Python's y=150 update if the bat is dead!
                        if (!e.isDead) {
                            enemyTargetX.put(p.getId(), p.getX());
                            e.y = p.getY(); 
                            e.isFacingLeft = p.getIsFacingLeft();
                            
                            if (p.getAction().equals("Attacking") && !e.isAttacking) e.attack();
                            else if (p.getAction().equals("Running")) e.isMoving = true;
                            else e.isMoving = false;
                        }
                    
                    }
                }
                
                // 3. READ BOSS
                if (update.hasBoss()) {
                    GameBridgeProto.EntityState b = update.getBoss();
                    boolean serverSaysDead = b.getAction().equals("Dead");
                    
                    if (serverSaysDead && !isVideoPhase && !finalPhaseComplete) {
                        if (currentPhase < 3) {
                            isVideoPhase = true;
                            videoPhaseTimer = 10.0f; 
                            bossIsServerDead = true;
                            currentPhase++; 
                            
                            Gdx.app.postRunnable(() -> {
                                for (GameCharacter e : activeEnemies.values()) e.dispose();
                                activeEnemies.clear();
                                enemyTargetX.clear();
                                
                                try {
                                    videoPlayer = VideoPlayerCreator.createVideoPlayer();
                                    if (currentPhase == 2) videoPlayer.play(Gdx.files.internal("assets/intro2.webm"));
                                    else if (currentPhase >= 3) videoPlayer.play(Gdx.files.internal("assets/intro3.webm"));
                                } catch (Exception e) {}
                            });
                        } else {
                            // Phase 3 beaten! Stop playing videos and let him rot.
                            finalPhaseComplete = true; 
                        }
                    } else if (!serverSaysDead && isVideoPhase) {
                        isVideoPhase = false;
                        boss.isDead = false;
                        boss.currentHealth = boss.maxHealth;
                        boss.displayedHealth = boss.maxHealth;
                        boss.hasDealtDamage = false;
                        boss.x = b.getX();
                        targetBossX = b.getX();
                        bossIsServerDead = false;
                        
                        // Clean up video memory when the phase starts
                        Gdx.app.postRunnable(() -> {
                            if (videoPlayer != null) { videoPlayer.dispose(); videoPlayer = null; }
                        });
                    }

                    if (!serverSaysDead) {
                        targetBossX = b.getX(); 
                        if (Math.abs(boss.y - b.getY()) > 50f) {
                            boss.y = b.getY();
                        }
                        boss.isFacingLeft = b.getIsFacingLeft();
                        
                        if (b.getAction().equals("Attacking") && !boss.isAttacking) boss.attack();
                        else if (b.getAction().equals("SecondaryAttacking") && !boss.isSecondaryAttacking) boss.secondaryAttack();
                        else if (b.getAction().equals("Jump")) boss.jump();
                        else if (b.getAction().equals("Running")) boss.isMoving = true;
                        else boss.isMoving = false;
                    }
                }
            }
            @Override
            public void onError(Throwable t) { System.out.println("Lobby connection lost: " + t.getMessage()); }
            @Override
            public void onCompleted() { System.out.println("Lobby Stream Closed."); }
        });
    }

    @Override
    public void resize(int width, int height) { viewport.update(width, height); }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        Gdx.gl.glClearColor(0, 0, 0, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update(); batch.setProjectionMatrix(camera.combined);

        if (currentState == GameState.CHARACTER_SELECT) renderCharacterSelect();
        else if (currentState == GameState.INTRO) renderIntro();
        else renderCombat(deltaTime);
    }

    private void renderCharacterSelect() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) selectedIndex = (selectedIndex - 1 + roster.size()) % roster.size();
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) selectedIndex = (selectedIndex + 1) % roster.size();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) { initializeSelectedPlayerAndStartVideo(); return; }

        shapeRenderer.setProjectionMatrix(camera.combined); shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.05f, 0.1f, 1f); shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        float cardWidth = 320f; float cardHeight = 440f; float startX = (WORLD_WIDTH - (roster.size() * (cardWidth + 60f))) / 2f;
        
        for (int i = 0; i < roster.size(); i++) {
            float cX = startX + (i * (cardWidth + 60f)); float cY = (WORLD_HEIGHT - cardHeight) / 2f - 20f;
            if (i == selectedIndex) { shapeRenderer.setColor(1.0f, 0.8f, 0.1f, 1f); shapeRenderer.rect(cX - 6, cY - 6, cardWidth + 12, cardHeight + 12); }
            shapeRenderer.setColor(0.15f, 0.15f, 0.25f, 1f); shapeRenderer.rect(cX, cY, cardWidth, cardHeight);
        }
        shapeRenderer.end();

        batch.begin(); font.getData().setScale(2.2f); font.draw(batch, "SELECT YOUR CHAMPION", WORLD_WIDTH / 2f - 240f, WORLD_HEIGHT - 70f);
        font.getData().setScale(1.2f); font.draw(batch, "Use A/D to Browse | Press ENTER to Watch Intro & Battle", WORLD_WIDTH / 2f - 280f, WORLD_HEIGHT - 120f);

        for (int i = 0; i < roster.size(); i++) {
            float cX = startX + (i * (cardWidth + 60f)); float cY = (WORLD_HEIGHT - cardHeight) / 2f - 20f; CharacterProfile p = roster.get(i);
            font.getData().setScale(1.4f); font.draw(batch, p.name, cX + 25f, cY + cardHeight - 35f);
            float previewScale = 1f; float previewOffsetY = 0f;
            if (p.name.equals("King Nebuchadnezzar")) { previewScale = 1.1f; previewOffsetY = 49f; } 
            else if (p.name.equals("Ironeye")) { previewScale = 0.95f; } 
            else if (p.name.equals("Sekiro")) { previewScale = 1.76f; previewOffsetY = 70f; }
            float previewSize = 180f * previewScale; previewOffsetY -= previewSize - 180f;
            batch.draw(p.previewRegion, cX + (cardWidth - previewSize) / 2f, cY + 100f + previewOffsetY, previewSize, previewSize);
        }
        batch.end();
    }

    private void renderIntro() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { transitionToCombat(); return; }
        if (isVideoPlaying && videoPlayer != null) {
            videoPlayer.update();
            if (introCompletionPending) { introCompletionPending = false; transitionToCombat(); return; }
            if (videoPlayer == null || currentState != GameState.INTRO) return;
            Texture frame = videoPlayer.getTexture();
            if (frame != null) {
                batch.begin(); batch.draw(frame, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
                batch.draw(watermarkCover, 865f, -110f, 600f, 600f); batch.end();
            }
        }
    }

    private void renderCombat(float deltaTime) {
        
        if (boss.currentHealth <= 0 && !boss.isDead) boss.isDead = true; 

        if (isFightTextActive) {
            fightTextTimer -= deltaTime;
            if (fightTextTimer <= 0) isFightTextActive = false;
        }

        if (isVideoPhase) {
            videoPhaseTimer -= deltaTime;
        }

        // --- 1. COMBAT LOGIC (Skipped during cinematics) ---
        if (!isFightTextActive && !isVideoPhase) {
            if (attackCooldownTimer > 0f) attackCooldownTimer -= deltaTime;
            
            king.isMoving = false; 
            if (!king.isDead) {
                if (Gdx.input.isKeyPressed(Input.Keys.A)) { king.x -= king.speed * deltaTime; king.isMoving = true; king.isFacingLeft = true; }
                if (Gdx.input.isKeyPressed(Input.Keys.D)) { king.x += king.speed * deltaTime; king.isMoving = true; king.isFacingLeft = false; }
                if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) king.jump();
                
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    boolean isIroneye = roster.get(selectedIndex).name.equals("Ironeye");
                    if (!isIroneye || attackCooldownTimer <= 0f) {
                        king.attack();
                        if (isIroneye) {
                            float projX = king.isFacingLeft ? king.x : king.x + king.size;
                            activeProjectiles.add(new Projectile(projX, king.y + (king.size * 0.45f), king.isFacingLeft));
                            attackCooldownTimer = 0.5f; 
                        }
                    }
                }
                if (!roster.get(selectedIndex).name.equals("Ironeye")) {
                    if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) king.secondaryAttack();
                    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) king.superAttack();
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) king.parry();
            }

            if (king.x < -50) king.x = -50; 
            if (king.x > WORLD_WIDTH - 100) king.x = WORLD_WIDTH - 100;
            if (boss.x < -50) boss.x = -50; 
            if (boss.x > WORLD_WIDTH - 250) boss.x = WORLD_WIDTH - 250; 

            Iterator<Projectile> pIter = activeProjectiles.iterator();
            while (pIter.hasNext()) {
                Projectile p = pIter.next(); p.update(deltaTime);
                if (p.active && !boss.isDead && p.getHitbox().overlaps(boss.getHurtbox())) { boss.takeDamage(10f); p.active = false; }
                if (!p.active) pIter.remove();
            }

            boolean isKingSwinging = king.isAttacking || king.isSecondaryAttacking || king.isSuperAttacking;
            boolean skipMeleeCheck = roster.get(selectedIndex).name.equals("Ironeye");

            // KING HITS BOSS
            if (isKingSwinging && !king.hasDealtDamage && !king.isDead && !boss.isDead && !skipMeleeCheck) {
                if (king.getHitbox().overlaps(boss.getHurtbox())) {
                    king.hasDealtDamage = true; 
                    if (king.isSuperAttacking) boss.takeDamage(35f);
                    else boss.takeDamage(15f);
                }
            }
            // BOSS HITS KING
            boolean isBossSwinging = boss.isAttacking || boss.isSecondaryAttacking;
            if (isBossSwinging && !boss.hasDealtDamage && !boss.isDead && !king.isDead) {
                if (king.isParrying) boss.hasDealtDamage = true; 
                else if (boss.getHitbox().overlaps(king.getHurtbox())) {
                    boss.hasDealtDamage = true; 
                    king.takeDamage(10f); 
                }
            }

            // ENEMY MOVEMENT & COLLISION
            for (Map.Entry<String, GameCharacter> entry : activeEnemies.entrySet()) {
                String id = entry.getKey(); GameCharacter e = entry.getValue();

                // 1. GRAVITY: Only runs when DEAD
                if (e.isDead && e.y > 100f) {
                    e.y -= 150f * deltaTime; // Fall speed
                    if (e.y < 100f) e.y = 100f; // Stop perfectly on the floor
                }
                
                // 2. SERVER SYNC & ANIMATION: Only runs when ALIVE
                if (!e.isDead) {
                    float targetX = enemyTargetX.getOrDefault(id, e.x);
                    if (Math.abs(e.x - targetX) > 150f) e.x = targetX;
                    else e.x += (targetX - e.x) * 10f * deltaTime;

                    if (e.isMoving && !e.isAttacking) {
                        if (e.isFacingLeft) e.x -= e.speed * deltaTime;
                        else e.x += e.speed * deltaTime;
                    }
                }

                // KING HITS ENEMY
                if (isKingSwinging && !king.hasDealtDamage && !e.isDead && !king.isDead && !skipMeleeCheck) {
                    if (king.getHitbox().overlaps(e.getHurtbox())) {
                        e.takeDamage(king.isSuperAttacking ? 35f : 15f);
                    }
                }
                // ENEMY HITS KING
                if (e.isAttacking && !e.hasDealtDamage && !e.isDead && !king.isDead) {
                    if (king.isParrying) e.hasDealtDamage = true;
                    else if (e.getHitbox().overlaps(king.getHurtbox())) {
                        e.hasDealtDamage = true;
                        king.takeDamage(5f);
                    }
                }
            }

            // FIX: Only sync Boss X coordinate if ALIVE
            if (!boss.isDead) {
                if (Math.abs(boss.x - targetBossX) > 150f) boss.x = targetBossX;
                else boss.x += (targetBossX - boss.x) * 10f * deltaTime;
            }

        } else {
            king.isMoving = false; boss.isMoving = false;
        }

        // --- 1.5. NETWORK SYNC ---
        networkTimer += deltaTime;
        if (networkTimer >= 0.03f && requestObserver != null) { 
            networkTimer = 0f;
            try {
                String currentAction = "Idle";
                if (king.isDead) currentAction = "Dead"; 
                else if (boss.currentHealth <= 0 && !bossIsServerDead && !finalPhaseComplete) currentAction = "KILLED_BOSS"; // Sends Kill Signal!
                else if (!isFightTextActive && !isVideoPhase) {
                    if (king.isParrying) currentAction = "Parrying";
                    else if (king.isSuperAttacking) currentAction = "SuperAttacking";
                    else if (king.isAttacking || king.isSecondaryAttacking) currentAction = "Attacking";
                    else if (king.isMoving) currentAction = "Running";
                }

                GameBridgeProto.EntityState state = GameBridgeProto.EntityState.newBuilder()
                    .setId("Abishek") 
                    .setCharacter(roster.get(selectedIndex).name)
                    .setX(king.x).setY(king.y) 
                    .setAction(currentAction)
                    .setIsFacingLeft(king.isFacingLeft)
                    .build();
                requestObserver.onNext(state);
            } catch (Exception e) {
                if (!aiConnectionWarningShown) { System.err.println("Stream failed: " + e.getMessage()); aiConnectionWarningShown = true; }
            }
        }

        // --- 2. VISUAL RENDERING ---
        batch.begin();
        batch.draw(bgLayer1, 0, 0, WORLD_WIDTH, WORLD_HEIGHT); batch.draw(bgLayer2, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.draw(bgLayer3, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        
        king.render(batch, deltaTime); 
        if (!bossIsServerDead) boss.render(batch, deltaTime);
        for (GameCharacter e : activeEnemies.values()) e.render(batch, deltaTime); // Render Minions!

        font.getData().setScale(1.2f);
        for (GameBridgeProto.EntityState teammate : networkPlayers.values()) {
            font.draw(batch, teammate.getId() + " (" + teammate.getCharacter() + ")", teammate.getX() + 30f, teammate.getY() + 180f);
        }
        
        batch.draw(bgLayer4, 0, 0, WORLD_WIDTH, WORLD_HEIGHT); 
        batch.end();

        // --- 3. HEALTH BARS ---
        shapeRenderer.setProjectionMatrix(camera.combined); shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Projectile p : activeProjectiles) p.render(shapeRenderer);

        if (!king.isDead) {
            float kX = king.x + (king.size * 0.2f); float kY = king.y + (king.size * 0.55f);
            shapeRenderer.setColor(0f, 0f, 0f, 1f); shapeRenderer.rect(kX - 2, kY - 2, 104f, 12f);
            shapeRenderer.setColor(1.0f, 0.8f, 0.1f, 1f); shapeRenderer.rect(kX, kY, 100f * (king.displayedHealth / king.maxHealth), 8f);
            shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f); shapeRenderer.rect(kX, kY, 100f * (king.currentHealth / king.maxHealth), 8f);
        }
        if (!boss.isDead && !bossIsServerDead) {
            float bX = boss.x + (boss.size * 0.35f); float bY = boss.y + (boss.size * 0.55f);
            shapeRenderer.setColor(0f, 0f, 0f, 1f); shapeRenderer.rect(bX - 2, bY - 2, 154f, 16f);
            shapeRenderer.setColor(1.0f, 0.8f, 0.1f, 1f); shapeRenderer.rect(bX, bY, 150f * (boss.displayedHealth / boss.maxHealth), 12f);
            shapeRenderer.setColor(0.9f, 0.1f, 0.1f, 1f); shapeRenderer.rect(bX, bY, 150f * (boss.currentHealth / boss.maxHealth), 12f);
        }
        
        shapeRenderer.end();

// --- 4. PHASE CINEMATIC OVERLAY ---
        if (isVideoPhase) {
            // 1. Draw a pitch-black background to hide the frozen game
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(camera.combined); 
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 1f); // 1f makes it fully opaque black
            shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            // 2. Play the video frame on top
            if (videoPlayer != null) {
                videoPlayer.update();
                Texture frame = videoPlayer.getTexture();
                if (frame != null) {
                    batch.begin(); 
                    batch.draw(frame, 0, 0, WORLD_WIDTH, WORLD_HEIGHT); 

                    batch.draw(watermarkCover, 865f, -110f, 600f, 600f); 

                    batch.end();
                }
            }
        }

        // --- 5. INITIAL READY TEXT OVERLAY ---
        if (isFightTextActive && readyTextTex != null && fightTextTex != null) {
            batch.begin();
            float scale = 1f + (float)Math.sin(fightTextTimer * 10f) * 0.05f; 
            Texture currentTex = (fightTextTimer > 1.5f) ? readyTextTex : fightTextTex;
            float w = (fightTextTimer > 1.5f) ? 750f * scale : 600f * scale;
            float h = 225f * scale;
            batch.draw(currentTex, (WORLD_WIDTH - w) / 2f, (WORLD_HEIGHT - h) / 2f + 80f, w, h);
            batch.end();
        }
    }

    @Override
    public void dispose() {
        batch.dispose(); shapeRenderer.dispose(); font.dispose();
        bgLayer1.dispose(); bgLayer2.dispose(); bgLayer3.dispose(); bgLayer4.dispose();
        if (readyTextTex != null) readyTextTex.dispose(); if (fightTextTex != null) fightTextTex.dispose();
        if (bossTexForCover != null) bossTexForCover.dispose(); for (CharacterProfile p : roster) p.dispose();
        if (king != null) king.dispose(); if (boss != null) boss.dispose(); if (videoPlayer != null) videoPlayer.dispose();
        for (GameCharacter e : activeEnemies.values()) e.dispose();
        if (channel != null) channel.shutdown();
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("MMO Tactical Arena"); config.setWindowedMode(1280, 720); config.useVsync(true); config.setForegroundFPS(60);
        new Lwjgl3Application(new GameClient(), config);
    }
}