package com.ztype.game;

import com.badlogic.gdx.ApplicationAdapter;
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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ZTypePcGame extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 900f;
    private static final float WORLD_HEIGHT = 1400f;
    private static final float DANGER_LINE_Y = 20f;
    private static final float SPAWN_INTERVAL_SCALE = 1.28f;
    private static final float ENEMY_SPEED_SCALE = 0.76f;

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
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply(true);

        for (int i = 0; i < 70; i++) {
            stars.add(new Star(
                    MathUtils.random(0f, WORLD_WIDTH),
                    MathUtils.random(0f, WORLD_HEIGHT),
                    MathUtils.random(0f, 10f),
                    MathUtils.random(0, 3)));
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
        });

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
        if (!running || paused) {
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
            if (enemy.y <= DANGER_LINE_Y) {
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
            spawnInterval = Math.max(minSpawnInterval, (1.5f - (level - 1) * 0.11f) * SPAWN_INTERVAL_SCALE);
            enemyBaseSpeed = (28f + (level - 1) * 3f) * ENEMY_SPEED_SCALE;
        }
    }

    private void spawnEnemy() {
        String word = WORDS[MathUtils.random(WORDS.length - 1)];
        float margin = 80f;
        float x = MathUtils.random(margin, WORLD_WIDTH - margin);
        float speed = (enemyBaseSpeed + MathUtils.random(0f, 18f) + level * 4f) * ENEMY_SPEED_SCALE;
        enemies.add(new Enemy(enemyIdSeed++, word, x, WORLD_HEIGHT + 10f, speed, 20f));
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
        shapeRenderer.rect(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);

        for (Star star : stars) {
            float speed = 16f + star.band * 8f;
            star.y -= speed * dt;
            if (star.y < 0f) {
                star.y = WORLD_HEIGHT;
            }

            float alpha = 0.2f + star.alphaBand * 0.15f;
            shapeRenderer.setColor(0.78f, 0.86f, 1f, alpha);
            shapeRenderer.rect(star.x, star.y, 2f, 2f);
        }

        shapeRenderer.setColor(0.98f, 0.44f, 0.57f, 0.35f);
        shapeRenderer.rect(0f, DANGER_LINE_Y, WORLD_WIDTH, 2f);
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
        uiFont.draw(batch, "Score: " + score, 22f, WORLD_HEIGHT - 20f);
        uiFont.draw(batch, "Lives: " + lives, 22f, WORLD_HEIGHT - 58f);
        uiFont.draw(batch, "Level: " + level, 22f, WORLD_HEIGHT - 96f);
        uiFont.draw(batch, "Target: " + targetWord, 22f, WORLD_HEIGHT - 134f);

        if (paused && running) {
            uiFont.setColor(Color.valueOf("c7d2fe"));
            uiFont.draw(batch, "Paused", WORLD_WIDTH / 2f - 32f, WORLD_HEIGHT / 2f + 24f);
            uiFont.setColor(Color.WHITE);
        }

        if (!running) {
            uiFont.setColor(Color.valueOf("fecdd3"));
            uiFont.draw(batch, "Game Over", WORLD_WIDTH / 2f - 55f, WORLD_HEIGHT / 2f + 24f);
            uiFont.setColor(Color.WHITE);
            uiFont.draw(batch, "Final Score: " + score, WORLD_WIDTH / 2f - 65f, WORLD_HEIGHT / 2f - 2f);
            uiFont.draw(batch, "Press R to Restart", WORLD_WIDTH / 2f - 80f, WORLD_HEIGHT / 2f - 28f);
        }
        batch.end();
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
}
