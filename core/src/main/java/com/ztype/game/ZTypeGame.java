package com.ztype.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ZTypeGame extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 900f;
    private static final float WORLD_HEIGHT = 1400f;
    private static final float DANGER_LINE_Y = 20f;
    private static final float KEYBOARD_AREA_HEIGHT = 420f;
    private static final float KEYBOARD_SHIFT_DOWN = 28f;
    private static final float MOBILE_SPAWN_INTERVAL_SCALE = 1.28f;
    private static final float MOBILE_ENEMY_SPEED_SCALE = 0.76f;
    private static final float MOBILE_ACTION_ROW_Y = 4f;
    private static final float DESKTOP_SPAWN_INTERVAL_SCALE = 1f;
    private static final float DESKTOP_ENEMY_SPEED_SCALE = 1f;

    private static final String[] WORDS = {
            "code", "bug", "array", "loop", "class", "object", "event", "canvas", "game", "score",
            "logic", "input", "enemy", "level", "delta", "pixel", "timer", "stack", "queue", "value",
            "script", "render", "target", "rocket", "planet", "meteor", "galaxy", "signal", "vector", "kernel"
    };

    private final Array<Enemy> enemies = new Array<>();
    private final Array<Star> stars = new Array<>();

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont uiFont;
    private GlyphLayout glyphLayout;
    private OrthographicCamera camera;
    private Viewport viewport;
    private final Vector3 touchPoint = new Vector3();
    private float worldHeight;

    private boolean running;
    private boolean paused;
    private int score;
    private int lives;
    private int level;
    private int enemyIdSeed;
    private int lockedEnemyId;

    private float elapsedTime;
    private float spawnTimer;
    private float spawnInterval;
    private float minSpawnInterval;
    private float enemyBaseSpeed;
    private float enemyMinY;
    private boolean showTouchKeyboard;
    private float mobileSpawnScale;
    private float mobileSpeedScale;

    private final Array<KeyButton> keyButtons = new Array<>();

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        uiFont = new BitmapFont();
        glyphLayout = new GlyphLayout();

        font.getData().setScale(2.0f);
        uiFont.getData().setScale(2.0f);

        camera = new OrthographicCamera();
        showTouchKeyboard = isTouchPlatform();
        worldHeight = WORLD_HEIGHT;
        if (showTouchKeyboard) {
            float aspect = (float) Gdx.graphics.getHeight() / (float) Gdx.graphics.getWidth();
            worldHeight = Math.max(WORLD_HEIGHT, WORLD_WIDTH * aspect);
        }
        viewport = showTouchKeyboard
                ? new FitViewport(WORLD_WIDTH, worldHeight, camera)
                : new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply(true);

        enemyMinY = showTouchKeyboard ? DANGER_LINE_Y + KEYBOARD_AREA_HEIGHT : DANGER_LINE_Y;
        mobileSpawnScale = showTouchKeyboard ? MOBILE_SPAWN_INTERVAL_SCALE : DESKTOP_SPAWN_INTERVAL_SCALE;
        mobileSpeedScale = showTouchKeyboard ? MOBILE_ENEMY_SPEED_SCALE : DESKTOP_ENEMY_SPEED_SCALE;

        for (int i = 0; i < 70; i++) {
            stars.add(new Star(MathUtils.random(0f, WORLD_WIDTH), MathUtils.random(0f, WORLD_HEIGHT), MathUtils.random(0f, 10f), MathUtils.random(0, 3)));
            stars.get(i).y = MathUtils.random(0f, worldHeight);
        }

        resetGame();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (!running) {
                    if (character == 'r' || character == 'R') {
                        resetGame();
                        return true;
                    }
                    return false;
                }
                char ch = Character.toLowerCase(character);
                if (ch >= 'a' && ch <= 'z') {
                    onLetterInput(ch);
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                viewport.unproject(touchPoint.set(screenX, screenY, 0f));
                return handleTouchInput(touchPoint.x, touchPoint.y);
            }
        });

        rebuildKeyboardLayout();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private boolean isTouchPlatform() {
        Application.ApplicationType appType = Gdx.app.getType();
        return appType == Application.ApplicationType.Android || appType == Application.ApplicationType.iOS;
    }

    private void resetGame() {
        running = true;
        paused = false;
        score = 0;
        lives = 5;
        level = 1;
        enemyIdSeed = 1;
        lockedEnemyId = -1;

        elapsedTime = 0f;
        spawnTimer = 0f;
        spawnInterval = 1.5f;
        minSpawnInterval = 0.5f;
        enemyBaseSpeed = 28f;

        enemies.clear();
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        update(dt);

        Gdx.gl.glClearColor(0.02f, 0.03f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawBackground(dt);
        drawEnemies();
        drawHud();
    }

    private void update(float dt) {
        if (!running) {
            return;
        }

        if (paused) {
            return;
        }

        elapsedTime += dt;
        updateDifficulty();

        spawnTimer += dt;
        if (spawnTimer >= spawnInterval) {
            spawnEnemy();
            spawnTimer = 0f;
        }

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.y -= enemy.speed * dt;
            if (enemy.y <= enemyMinY) {
                if (lockedEnemyId == enemy.id) {
                    lockedEnemyId = -1;
                }
                enemies.removeIndex(i);
                loseLife();
                if (!running) {
                    return;
                }
            }
        }
    }

    private void updateDifficulty() {
        int newLevel = 1 + (int) (elapsedTime / 18f);
        if (newLevel != level) {
            level = newLevel;
            spawnInterval = Math.max(minSpawnInterval, (1.5f - (level - 1) * 0.11f) * mobileSpawnScale);
            enemyBaseSpeed = (28f + (level - 1) * 3f) * mobileSpeedScale;
        }
    }

    private void spawnEnemy() {
        String word = WORDS[MathUtils.random(WORDS.length - 1)];
        float margin = 80f;
        float x = MathUtils.random(margin, WORLD_WIDTH - margin);
        float speed = (enemyBaseSpeed + MathUtils.random(0f, 18f) + level * 4f) * mobileSpeedScale;
        enemies.add(new Enemy(enemyIdSeed++, word, x, worldHeight + 10f, speed, 20f));
    }

    private Enemy getLockedEnemy() {
        if (lockedEnemyId == -1) {
            return null;
        }
        for (Enemy enemy : enemies) {
            if (enemy.id == lockedEnemyId) {
                return enemy;
            }
        }
        return null;
    }

    private Enemy chooseLockByFirstChar(char ch) {
        Enemy target = null;
        for (Enemy enemy : enemies) {
            if (enemy.word.charAt(0) == ch) {
                if (target == null || enemy.y < target.y) {
                    target = enemy;
                }
            }
        }
        return target;
    }

    private void onLetterInput(char ch) {
        Enemy locked = getLockedEnemy();
        if (locked == null) {
            Enemy target = chooseLockByFirstChar(ch);
            if (target == null) {
                return;
            }
            lockedEnemyId = target.id;
            target.progress = 1;
            if (target.progress >= target.word.length()) {
                killEnemy(target.id);
            }
            return;
        }

        char expect = locked.word.charAt(locked.progress);
        if (expect != ch) {
            return;
        }

        locked.progress += 1;
        if (locked.progress >= locked.word.length()) {
            killEnemy(locked.id);
        }
    }

    private void killEnemy(int enemyId) {
        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.id == enemyId) {
                score += enemy.word.length() * 10;
                enemies.removeIndex(i);
                if (lockedEnemyId == enemyId) {
                    lockedEnemyId = -1;
                }
                return;
            }
        }
    }

    private void loseLife() {
        lives -= 1;
        if (lives <= 0) {
            lives = 0;
            running = false;
            lockedEnemyId = -1;
        }
    }

    private void drawBackground(float dt) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.03f, 0.07f, 0.15f, 1f);
        shapeRenderer.rect(0f, 0f, WORLD_WIDTH, worldHeight);

        for (Star star : stars) {
            float speed = 16f + star.band * 8f;
            star.y -= speed * dt;
            if (star.y < 0f) {
                star.y = worldHeight;
            }

            float alpha = 0.2f + star.alphaBand * 0.15f;
            shapeRenderer.setColor(0.78f, 0.86f, 1f, alpha);
            shapeRenderer.rect(star.x, star.y, 2f, 2f);
        }

        shapeRenderer.setColor(0.98f, 0.44f, 0.57f, 0.35f);
        shapeRenderer.rect(0f, enemyMinY, WORLD_WIDTH, 2f);

        if (showTouchKeyboard) {
            shapeRenderer.setColor(0.05f, 0.08f, 0.18f, 0.95f);
            shapeRenderer.rect(0f, 0f, WORLD_WIDTH, KEYBOARD_AREA_HEIGHT);
        }
        shapeRenderer.end();
    }

    private void drawEnemies() {
        Enemy lockedEnemy = getLockedEnemy();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Enemy enemy : enemies) {
            boolean isLocked = lockedEnemy != null && lockedEnemy.id == enemy.id;
            if (isLocked) {
                shapeRenderer.setColor(0.98f, 0.44f, 0.57f, 1f);
            } else {
                shapeRenderer.setColor(0.49f, 0.83f, 0.99f, 1f);
            }
            shapeRenderer.circle(enemy.x, enemy.y, enemy.radius);
        }
        shapeRenderer.end();

        batch.begin();
        for (Enemy enemy : enemies) {
            String done = enemy.word.substring(0, enemy.progress);
            String todo = enemy.word.substring(enemy.progress);

            glyphLayout.setText(font, enemy.word);
            float startX = enemy.x - glyphLayout.width / 2f;
            float textY = enemy.y - 40f;

            font.setColor(Color.valueOf("86efac"));
            font.draw(batch, done, startX, textY);

            glyphLayout.setText(font, done);
            float doneWidth = glyphLayout.width;
            font.setColor(Color.WHITE);
            font.draw(batch, todo, startX + doneWidth, textY);
        }
        batch.end();
    }

    private void drawHud() {
        Enemy locked = getLockedEnemy();
        String targetWord = locked == null ? "None" : locked.word;

        batch.begin();
        uiFont.setColor(Color.WHITE);
        uiFont.draw(batch, "Score: " + score, 22f, worldHeight - 20f);
        uiFont.draw(batch, "Lives: " + lives, 22f, worldHeight - 58f);
        uiFont.draw(batch, "Level: " + level, 22f, worldHeight - 96f);
        uiFont.draw(batch, "Target: " + targetWord, 22f, worldHeight - 134f);

        if (paused && running) {
            uiFont.setColor(Color.valueOf("c7d2fe"));
            uiFont.draw(batch, "Paused", WORLD_WIDTH / 2f - 32f, worldHeight / 2f + 24f);
            uiFont.setColor(Color.WHITE);
        }

        if (!running) {
            uiFont.setColor(Color.valueOf("fecdd3"));
            uiFont.draw(batch, "Game Over", WORLD_WIDTH / 2f - 55f, worldHeight / 2f + 24f);
            uiFont.setColor(Color.WHITE);
            uiFont.draw(batch, "Final Score: " + score, WORLD_WIDTH / 2f - 65f, worldHeight / 2f - 2f);
            uiFont.draw(batch, "Tap Restart or Press R", WORLD_WIDTH / 2f - 95f, worldHeight / 2f - 28f);
        }
        batch.end();

        if (showTouchKeyboard) {
            drawKeyboard();
        }
    }

    private void rebuildKeyboardLayout() {
        keyButtons.clear();
        if (!showTouchKeyboard) {
            return;
        }

        float areaTop = KEYBOARD_AREA_HEIGHT;
        float outerPadding = 16f;
        float gap = 8f;
        float rowHeight = 76f;
        float down = KEYBOARD_SHIFT_DOWN;

        addKeyRow("QWERTYUIOP", areaTop - outerPadding - rowHeight - down, outerPadding, gap, rowHeight);
        addKeyRow("ASDFGHJKL", areaTop - outerPadding * 2f - rowHeight * 2f - down, outerPadding + 38f, gap, rowHeight);
        addKeyRow("ZXCVBNM", areaTop - outerPadding * 3f - rowHeight * 3f - down, outerPadding + 96f, gap, rowHeight);

        float actionY = MOBILE_ACTION_ROW_Y;
        float actionGap = 12f;
        float actionWidth = (WORLD_WIDTH - outerPadding * 2f - actionGap) / 2f;
        float actionHeight = 64f;
        keyButtons.add(KeyButton.action("Pause", 0f, outerPadding, actionY, actionWidth, actionHeight));
        keyButtons.add(KeyButton.action("Restart", 1f, outerPadding + actionWidth + actionGap, actionY, actionWidth, actionHeight));
    }

    private void addKeyRow(String letters, float y, float leftPadding, float gap, float rowHeight) {
        int count = letters.length();
        float rowWidth = WORLD_WIDTH - leftPadding * 2f;
        float keyWidth = (rowWidth - (count - 1) * gap) / count;
        for (int i = 0; i < count; i++) {
            char letter = letters.charAt(i);
            float x = leftPadding + i * (keyWidth + gap);
            keyButtons.add(KeyButton.letter(letter, x, y, keyWidth, rowHeight));
        }
    }

    private void drawKeyboard() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (KeyButton key : keyButtons) {
            if (key.type == KeyButton.TYPE_LETTER) {
                shapeRenderer.setColor(0.17f, 0.28f, 0.46f, 1f);
            } else {
                shapeRenderer.setColor(0.25f, 0.31f, 0.52f, 1f);
            }
            shapeRenderer.rect(key.bounds.x, key.bounds.y, key.bounds.width, key.bounds.height);
        }
        shapeRenderer.end();

        batch.begin();
        for (KeyButton key : keyButtons) {
            glyphLayout.setText(uiFont, key.label);
            float textX = key.bounds.x + (key.bounds.width - glyphLayout.width) / 2f;
            float textY = key.bounds.y + (key.bounds.height + glyphLayout.height) / 2f;
            uiFont.setColor(Color.WHITE);
            uiFont.draw(batch, key.label, textX, textY);
        }
        batch.end();
    }

    private boolean handleTouchInput(float worldX, float worldY) {
        if (!showTouchKeyboard) {
            return false;
        }
        for (KeyButton key : keyButtons) {
            if (key.bounds.contains(worldX, worldY)) {
                if (key.type == KeyButton.TYPE_LETTER) {
                    if (running && !paused) {
                        onLetterInput(Character.toLowerCase(key.letter));
                    }
                    return true;
                }
                if (key.actionCode == 0f) {
                    if (running) {
                        paused = !paused;
                    }
                    return true;
                }
                if (key.actionCode == 1f) {
                    resetGame();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void resize(int width, int height) {
        int backBufferWidth = Gdx.graphics.getBackBufferWidth();
        int backBufferHeight = Gdx.graphics.getBackBufferHeight();
        viewport.update(backBufferWidth, backBufferHeight, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        uiFont.dispose();
    }

    private static class Enemy {
        final int id;
        final String word;
        final float x;
        float y;
        final float speed;
        final float radius;
        int progress;

        Enemy(int id, String word, float x, float y, float speed, float radius) {
            this.id = id;
            this.word = word;
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.radius = radius;
            this.progress = 0;
        }
    }

    private static class Star {
        final float x;
        float y;
        final float band;
        final int alphaBand;

        Star(float x, float y, float band, int alphaBand) {
            this.x = x;
            this.y = y;
            this.band = band;
            this.alphaBand = alphaBand;
        }
    }

    private static class KeyButton {
        static final int TYPE_LETTER = 0;
        static final int TYPE_ACTION = 1;

        final int type;
        final char letter;
        final String label;
        final float actionCode;
        final Rectangle bounds;

        private KeyButton(int type, char letter, String label, float actionCode, Rectangle bounds) {
            this.type = type;
            this.letter = letter;
            this.label = label;
            this.actionCode = actionCode;
            this.bounds = bounds;
        }

        static KeyButton letter(char letter, float x, float y, float width, float height) {
            return new KeyButton(TYPE_LETTER, letter, String.valueOf(letter), -1f, new Rectangle(x, y, width, height));
        }

        static KeyButton action(String label, float actionCode, float x, float y, float width, float height) {
            return new KeyButton(TYPE_ACTION, '\0', label, actionCode, new Rectangle(x, y, width, height));
        }
    }
}
