package com.battle_royale.game;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import com.battle_royale.model.Obstacle;
import com.battle_royale.model.Player;
import com.battle_royale.model.Projectile;
import com.battle_royale.model.Vector2D;
import com.battle_royale.utils.Constants;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ConcurrentHashMap<Integer, Player> players;
    private final CopyOnWriteArrayList<Projectile> projectiles;
    private final ConcurrentHashMap<Integer, Player.Input> playerInputs;
    private final CopyOnWriteArrayList<Obstacle> obstacles;

    private static final int GRID_SIZE = 20;
    private static final double MIN_OBSTACLE_SIZE = 30;
    private static final double MAX_OBSTACLE_SIZE = 80;
    private static final double SPAWN_SAFE_ZONE_RADIUS = 100;
    private static final int TOTAL_OBSTACLES = 30;
    private static final double MIN_DISTANCE_BETWEEN_OBSTACLES = 120;
    private final List<Vector2D> spawnPoints;

    public GameState() {
        this.players = new ConcurrentHashMap<>();
        this.projectiles = new CopyOnWriteArrayList<>();
        this.playerInputs = new ConcurrentHashMap<>();
        this.obstacles = new CopyOnWriteArrayList<>();
        this.spawnPoints = initializeSpawnPoints();
        initializeObstacles(TOTAL_OBSTACLES, MIN_DISTANCE_BETWEEN_OBSTACLES);
    }

    private List<Vector2D> initializeSpawnPoints() {
        List<Vector2D> points = new ArrayList<>();
        points.add(new Vector2D(100, 100)); // Top-left
        points.add(new Vector2D(Constants.GAME_WIDTH - 100, 100));
        points.add(new Vector2D(100, Constants.GAME_HEIGHT - 100));
        points.add(new Vector2D(Constants.GAME_WIDTH - 100, Constants.GAME_HEIGHT - 100));
        points.add(new Vector2D(Constants.GAME_WIDTH / 2, 100));
        points.add(new Vector2D(Constants.GAME_WIDTH / 2, Constants.GAME_HEIGHT - 100));
        points.add(new Vector2D(100, Constants.GAME_HEIGHT / 2));
        points.add(new Vector2D(Constants.GAME_WIDTH - 100, Constants.GAME_HEIGHT / 2));
        return points;
    }

    private static class Rectangle implements Serializable {
        private static final long serialVersionUID = 1L;
        double x, y, width, height;

        Rectangle(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private boolean isInSafeZone(Vector2D position, double width, double height) {
        Rectangle obstacleRect = new Rectangle(
                position.x,
                position.y,
                width,
                height
        );

        for (Vector2D spawnPoint : spawnPoints) {
            double closestX = Math.max(obstacleRect.x, Math.min(spawnPoint.x, obstacleRect.x + obstacleRect.width));
            double closestY = Math.max(obstacleRect.y, Math.min(spawnPoint.y, obstacleRect.y + obstacleRect.height));

            double distanceX = spawnPoint.x - closestX;
            double distanceY = spawnPoint.y - closestY;
            double distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);

            if (distanceSquared < SPAWN_SAFE_ZONE_RADIUS * SPAWN_SAFE_ZONE_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private void initializeObstacles(int count, double minDistanceThreshold) {
        int maxAttempts = (int) Math.pow(10.0, 5);
        int currentAttempts = 0;

        while (obstacles.size() < count && currentAttempts < maxAttempts) {
            double width = ThreadLocalRandom.current().nextDouble(MIN_OBSTACLE_SIZE, MAX_OBSTACLE_SIZE);
            double height = ThreadLocalRandom.current().nextDouble(MIN_OBSTACLE_SIZE, MAX_OBSTACLE_SIZE);
            double x = ThreadLocalRandom.current().nextDouble(50, Constants.GAME_WIDTH - width);
            double y = ThreadLocalRandom.current().nextDouble(50, Constants.GAME_HEIGHT - height);

            Vector2D position = new Vector2D(x, y);
            Obstacle newObstacle = new Obstacle(position, width, height);

            boolean overlaps = obstacles.stream().anyMatch(obstacle ->
                    isOverlapping(newObstacle, obstacle, 20)
            );

            boolean inSafeZone = isInSafeZone(position, width, height);
            boolean meetsDistanceThreshold = obstacles.stream().noneMatch(obstacle ->
                    isWithinDistance(newObstacle, obstacle, minDistanceThreshold)
            );

            if (!overlaps && !inSafeZone && meetsDistanceThreshold) {
                obstacles.add(newObstacle);

                if (!isMapTraversable()) {
                    obstacles.remove(newObstacle);
                }
            }

            currentAttempts++;
        }
    }

    private boolean isWithinDistance(Obstacle a, Obstacle b, double threshold) {
        double dx = Math.abs(a.getPosition().getX() - b.getPosition().getX());
        double dy = Math.abs(a.getPosition().getY() - b.getPosition().getY());
        return dx < threshold && dy < threshold;
    }

    private boolean isOverlapping(Obstacle o1, Obstacle o2, double minGap) {
        return o1.getPosition().x - minGap < o2.getPosition().x + o2.getWidth() &&
                o1.getPosition().x + o1.getWidth() + minGap > o2.getPosition().x &&
                o1.getPosition().y - minGap < o2.getPosition().y + o2.getHeight() &&
                o1.getPosition().y + o1.getHeight() + minGap > o2.getPosition().y;
    }

    private boolean isMapTraversable() {
        boolean[][] grid = createNavigationGrid();

        int startX = 0;
        int startY = 0;

        while (startX < grid.length && grid[startX][startY]) startX++;
        while (startY < grid[0].length && grid[startX][startY]) startY++;

        if (startX >= grid.length || startY >= grid[0].length) {
            return false;
        }

        boolean[][] visited = new boolean[grid.length][grid[0].length];
        floodFill(grid, visited, startX, startY);

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (!grid[i][j] && !visited[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean[][] createNavigationGrid() {
        int gridCols = (int) Math.ceil(Constants.GAME_WIDTH / GRID_SIZE);
        int gridRows = (int) Math.ceil(Constants.GAME_HEIGHT / GRID_SIZE);
        boolean[][] grid = new boolean[gridCols][gridRows];

        for (Obstacle obstacle : obstacles) {
            int startX = (int) (obstacle.getPosition().x / GRID_SIZE);
            int startY = (int) (obstacle.getPosition().y / GRID_SIZE);
            int endX = (int) Math.ceil((obstacle.getPosition().x + obstacle.getWidth()) / GRID_SIZE);
            int endY = (int) Math.ceil((obstacle.getPosition().y + obstacle.getHeight()) / GRID_SIZE);

            for (int x = startX; x < endX && x < gridCols; x++) {
                for (int y = startY; y < endY && y < gridRows; y++) {
                    grid[x][y] = true;
                }
            }
        }

        return grid;
    }

    private void floodFill(boolean[][] grid, boolean[][] visited, int x, int y) {
        if (x < 0 || x >= grid.length || y < 0 || y >= grid[0].length ||
                grid[x][y] || visited[x][y]) {
            return;
        }

        visited[x][y] = true;

        floodFill(grid, visited, x + 1, y);
        floodFill(grid, visited, x - 1, y);
        floodFill(grid, visited, x, y + 1);
        floodFill(grid, visited, x, y - 1);
    }

    public void update() {
        playerInputs.forEach((id, input) -> {
            Player player = players.get(id);
            if (player != null) {
                Vector2D oldPosition = new Vector2D(player.getPosition());
                player.update(input);

                for (Obstacle obstacle : obstacles) {
                    if (obstacle.collidesWith(player.getPosition(), Constants.PLAYER_SIZE, Constants.PLAYER_SIZE)) {
                        player.setPosition(oldPosition);
                        break;
                    }
                }
            }
        });

        List<Projectile> projectilesToRemove = new ArrayList<>();

        for (Projectile projectile : projectiles) {
            projectile.update();

            if (projectile.isOutOfBounds()) {
                projectilesToRemove.add(projectile);
                continue;
            }

            boolean hitObstacle = obstacles.stream()
                    .anyMatch(obstacle -> obstacle.collidesWith(
                            projectile.getPosition(),
                            Constants.PROJECTILE_SIZE,
                            Constants.PROJECTILE_SIZE
                    ));

            if (hitObstacle) {
                projectilesToRemove.add(projectile);
                continue;
            }

            for (Player player : players.values()) {
                if (player.getId() != projectile.getPlayer().getId() &&
                        player.isAlive() &&
                        projectile.collidesWith(player)) {
                    player.damage(10);
                    projectilesToRemove.add(projectile);
                    break;
                }
            }
        }

        projectiles.removeAll(projectilesToRemove);
    }

    public void addPlayer(int id) {
        Vector2D spawnPoint = findBestSpawnPoint();
        Player newPlayer = new Player(id, players);
        newPlayer.setPosition(spawnPoint);
        players.put(id, newPlayer);
        playerInputs.put(id, new Player.Input());
    }

    private Vector2D findBestSpawnPoint() {
        Vector2D bestSpawnPoint = spawnPoints.get(0);
        double maxMinDistance = 0;

        for (Vector2D spawnPoint : spawnPoints) {
            double minDistance = Double.MAX_VALUE;

            for (Player player : players.values()) {
                double distance = calculateDistance(spawnPoint, player.getPosition());
                minDistance = Math.min(minDistance, distance);
            }

            if (minDistance > maxMinDistance) {
                maxMinDistance = minDistance;
                bestSpawnPoint = spawnPoint;
            }
        }

        double randomOffsetX = ThreadLocalRandom.current().nextDouble(-20, 20);
        double randomOffsetY = ThreadLocalRandom.current().nextDouble(-20, 20);
        return new Vector2D(
                bestSpawnPoint.x + randomOffsetX,
                bestSpawnPoint.y + randomOffsetY
        );
    }

    private double calculateDistance(Vector2D p1, Vector2D p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void removePlayer(int id) {
        players.remove(id);
        playerInputs.remove(id);
    }

    public void updatePlayerInput(int playerId, Player.Input input) {
        playerInputs.put(playerId, input);
        Player player = players.get(playerId);

        if (input.shooting && player != null && player.isAlive()) {
            addProjectile(player);
        }
    }

    private void addProjectile(Player player) {
        Vector2D position = new Vector2D(
                player.getPosition().x + Constants.PLAYER_SIZE / 2,
                player.getPosition().y + Constants.PLAYER_SIZE / 2
        );
        Vector2D velocity = new Vector2D(0, 0);
        Player.Input playerInput = playerInputs.get(player.getId());

        if (playerInput.isMoving() || player.getLastDirection() != null) {
            if (playerInput.right || player.getLastDirection() == Player.Direction.RIGHT) {
                velocity.x = Constants.PROJECTILE_SPEED;
            } else if (playerInput.left || player.getLastDirection() == Player.Direction.LEFT) {
                velocity.x = -Constants.PROJECTILE_SPEED;
            } else if (playerInput.up || player.getLastDirection() == Player.Direction.UP) {
                velocity.y = -Constants.PROJECTILE_SPEED;
            } else if (playerInput.down || player.getLastDirection() == Player.Direction.DOWN) {
                velocity.y = Constants.PROJECTILE_SPEED;
            }

            projectiles.add(new Projectile(player, position, velocity));
        }
    }

    public Map<Integer, Player> getPlayers() {
        return new HashMap<>(players);
    }

    public List<Projectile> getProjectiles() {
        return new ArrayList<>(projectiles);
    }

    public List<Obstacle> getObstacles() {
        return new ArrayList<>(obstacles);
    }

    @Override
    public String toString() {
        return "GameState{" +
                "players=" + players +
                ", projectiles=" + projectiles +
                ", playerInputs=" + playerInputs +
                '}';
    }
}