package com.battle_royale.game;


import com.battle_royale.network.GamePacket;
import com.battle_royale.network.PacketType;
import com.battle_royale.model.Obstacle;
import com.battle_royale.model.Player;
import com.battle_royale.model.Projectile;
import com.battle_royale.utils.Constants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class GameClient extends Application {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameState gameState;
    private Canvas canvas;
    private GraphicsContext gc;
    private boolean isRunning = true;
    private final Object gameStateLock = new Object();
    private Map<KeyCode, Boolean> keyStates = new EnumMap<>(KeyCode.class);
    private Map<Integer, String> playerDeathNotifications = new HashMap<>();
    private static final long INPUT_THROTTLE_MS = 16;
    private static final long SHOOTING_THROTTLE_MS = 250;
    private AtomicLong lastInputTime = new AtomicLong(0);
    private AtomicLong lastShootingTime = new AtomicLong(0);
    private volatile Player.Input currentInput = new Player.Input();
    private volatile boolean inputChanged = false;
    private volatile boolean shootingChanged = false;

    private static final Color JOIN_NOTIFICATION_COLOR = Color.LIGHTGREEN;
    private static final Color DISCONNECT_NOTIFICATION_COLOR = Color.YELLOW;
    private static final Color DEATH_NOTIFICATION_COLOR = Color.RED;
    private static final Color VICTORY_NOTIFICATION_COLOR = Color.GREEN;

    private Map<Integer, NotificationInfo> playerNotifications = new HashMap<>();
    private static final long NOTIFICATION_DISPLAY_TIME = 5000;
    private int currentPlayerId = -1;


    private static class NotificationInfo {
        String message;
        Color color;
        long timestamp;

        NotificationInfo(String message, Color color) {
            this.message = message;
            this.color = color;
            this.timestamp = System.currentTimeMillis();
        }
    }



    @Override
    public void start(Stage primaryStage) {
        connectToServer();

        canvas = new Canvas(Constants.GAME_WIDTH, Constants.GAME_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);

        setupInputHandling(scene);

        primaryStage.setTitle("Battle Royale 2D");
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(this::gameLoop).start();
        new Thread(this::networkLoop).start();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            sendPacket(new GamePacket(PacketType.PLAYER_JOIN, null));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void setupInputHandling(Scene scene) {
        scene.setOnKeyPressed(event -> {
            synchronized (keyStates) {
                if (!keyStates.getOrDefault(event.getCode(), false)) {
                    keyStates.put(event.getCode(), true);
                    updateAndSendInput();
                }
            }
        });

        scene.setOnKeyReleased(event -> {
            synchronized (keyStates) {
                keyStates.put(event.getCode(), false);
                updateAndSendInput();
            }
        });
    }

    private void updateAndSendInput() {
        synchronized (keyStates) {
            Player.Input newInput = new Player.Input();
            newInput.up = keyStates.getOrDefault(KeyCode.W, false);
            newInput.down = keyStates.getOrDefault(KeyCode.S, false);
            newInput.left = keyStates.getOrDefault(KeyCode.A, false);
            newInput.right = keyStates.getOrDefault(KeyCode.D, false);
            newInput.shooting = keyStates.getOrDefault(KeyCode.SPACE, false);

            if (!newInput.equals(currentInput)) {
                currentInput = newInput;
                inputChanged = true;
            }

            if (newInput.shooting && !currentInput.shooting) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastShoot = currentTime - lastShootingTime.get();

                if (timeSinceLastShoot >= SHOOTING_THROTTLE_MS) {
                    shootingChanged = true;
                    lastShootingTime.set(currentTime);
                } else {
                    shootingChanged = false;
                }
            } else {
                shootingChanged = false;
            }
        }
    }

    private void gameLoop() {
        final int FPS = 60;
        final long frameTime = 1000 / FPS;

        while (isRunning) {
            long startTime = System.currentTimeMillis();


            if (inputChanged) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastInput = currentTime - lastInputTime.get();

                if (timeSinceLastInput >= INPUT_THROTTLE_MS) {
                    synchronized (keyStates) {
                        sendPacket(new GamePacket(PacketType.PLAYER_INPUT, currentInput));
                        lastInputTime.set(currentTime);
                        inputChanged = false;
                    }
                }
            }

            if (shootingChanged) {
                sendPacket(new GamePacket(PacketType.PLAYER_SHOOT, null));
            }

            Platform.runLater(() -> {
                render();
                renderHealthBars();
                renderDeathNotifications();
            });

            long endTime = System.currentTimeMillis();
            long sleepTime = frameTime - (endTime - startTime);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void networkLoop() {
        try {
            while (isRunning) {
                GamePacket packet = (GamePacket) in.readObject();
                if (packet != null) {
                    handlePacket(packet);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Network error: " + e.getMessage());
            e.printStackTrace();
            isRunning = false;
            Platform.runLater(() -> {
                System.out.println("Disconnected from server");
            });
        }
    }


    private void handlePacket(GamePacket packet) {
        switch (packet.type) {
            case PLAYER_ID_ASSIGN:
                currentPlayerId = (Integer) packet.data;
                System.out.println("Assigned player ID: " + currentPlayerId);
                break;

            case PacketType.GAME_STATE:
            synchronized (gameStateLock) {
                try {
                    GameState newState = (GameState) packet.data;
                    if (newState != null) {
                        if (gameState != null) {
                            Set<Integer> currentPlayers = newState.getPlayers().keySet();
                            Set<Integer> previousPlayers = gameState.getPlayers().keySet();

                            for (Integer playerId : currentPlayers) {
                                if (!previousPlayers.contains(playerId)) {
                                    String joinMsg = "Player " + playerId + " has joined the game";
                                    addNotification(playerId, joinMsg, JOIN_NOTIFICATION_COLOR);
                                }
                            }

                            for (Integer playerId : previousPlayers) {
                                if (!currentPlayers.contains(playerId)) {
                                    String disconnectMsg = "Player " + playerId + " has left the game";
                                    addNotification(playerId, disconnectMsg, DISCONNECT_NOTIFICATION_COLOR);
                                }
                            }
                        } else {
                            for (Integer playerId : newState.getPlayers().keySet()) {
                                String joinMsg = "Player " + playerId + " has joined the game";
                                addNotification(playerId, joinMsg, JOIN_NOTIFICATION_COLOR);
                            }
                        }

                        gameState = newState;
                        checkPlayerDeaths();
                        sendPacket(new GamePacket(PacketType.STATE_ACK, null));
                    }
                } catch (Exception e) {
                    System.err.println("Error processing game state: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    private void addNotification(int playerId, String message, Color color) {
        playerNotifications.put(playerId, new NotificationInfo(message, color));

        new Thread(() -> {
            try {
                Thread.sleep(NOTIFICATION_DISPLAY_TIME);
                Platform.runLater(() -> playerNotifications.remove(playerId));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void checkPlayerDeaths() {
        Map<Integer, Player> players = gameState.getPlayers();
        for (Player player : players.values()) {
            if (player != null && !player.isAlive()) {
                String deathNotification = "Player " + player.getId() + " has been eliminated";
                playerDeathNotifications.put(player.getId(), deathNotification);
            }
        }

        int alivePlayerCount = 0;
        int winningPlayerId = -1;
        for (Player player : players.values()) {
            if (player != null && player.isAlive()) {
                alivePlayerCount++;
                winningPlayerId = player.getId();
            }
        }

        if (alivePlayerCount == 1 && !playerDeathNotifications.isEmpty()) {
            String victoryNotification = "Player " + winningPlayerId + " has won the game!";
            addNotification(winningPlayerId, victoryNotification, VICTORY_NOTIFICATION_COLOR);
        }
    }

    private void render() {
        if (gameState == null) return;

        synchronized (gameStateLock) {
            gc.setFill(new LinearGradient(
                    0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.BLACK),
                    new Stop(1, Color.DARKSLATEGRAY)
            ));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            Map<Integer, Player> players = gameState.getPlayers();
            if (players != null) {
                for (Player player : players.values()) {
                    if (player != null && player.getPosition() != null) {
                        gc.setFill(player.getObjectColor());
                        gc.setEffect(new DropShadow(10, Color.BLACK));
                        gc.fillOval(player.getPosition().x, player.getPosition().y,
                                Constants.PLAYER_SIZE, Constants.PLAYER_SIZE);

                        gc.setFill(player.getBarrelColor());
                        double barrelLength = 10;
                        double barrelWidth = 4;
                        double centerX = player.getPosition().x + Constants.PLAYER_SIZE / 2;
                        double centerY = player.getPosition().y + Constants.PLAYER_SIZE / 2;

                        switch (player.getLastDirection()) {
                            case UP:
                                gc.fillRect(centerX - barrelWidth/2,
                                        centerY - barrelLength,
                                        barrelWidth,
                                        barrelLength);
                                break;
                            case DOWN:
                                gc.fillRect(centerX - barrelWidth/2,
                                        centerY,
                                        barrelWidth,
                                        barrelLength);
                                break;
                            case LEFT:
                                gc.fillRect(centerX - barrelLength,
                                        centerY - barrelWidth/2,
                                        barrelLength,
                                        barrelWidth);
                                break;
                            case RIGHT:
                                gc.fillRect(centerX,
                                        centerY - barrelWidth/2,
                                        barrelLength,
                                        barrelWidth);
                                break;
                        }
                    }
                }
            }

            gc.setEffect(null);
            for (Projectile projectile : gameState.getProjectiles()) {
                Player player = players.getOrDefault(projectile.getPlayer().getId(), null);
                gc.setFill(player != null ? player.getBarrelColor() : Color.rgb(255, 69, 0, 0.8));
                if (projectile != null && projectile.getPosition() != null) {
                    gc.fillOval(projectile.getPosition().x, projectile.getPosition().y,
                            Constants.PROJECTILE_SIZE, Constants.PROJECTILE_SIZE);
                }
            }
            if (gameState.getObstacles() != null) {
                gc.setFill(Color.CYAN);
                gc.setEffect(new DropShadow(5, Color.CYAN));

                for (Obstacle obstacle : gameState.getObstacles()) {
                    if (obstacle != null && obstacle.getPosition() != null) {
//                        System.out.println("Drawing obstacle at: " +
//                                obstacle.getPosition().x + ", " +
//                                obstacle.getPosition().y +
//                                " with size: " + obstacle.getWidth() + "x" + obstacle.getHeight());

                        gc.fillRect(
                                obstacle.getPosition().x,
                                obstacle.getPosition().y,
                                obstacle.getWidth(),
                                obstacle.getHeight()
                        );
                    }
                }
                gc.setEffect(null);
            }
        }
    }

    private void renderHealthBars() {
        synchronized (gameStateLock) {
            if(gameState != null) {
                Map<Integer, Player> players = gameState.getPlayers();
                for (Player player : players.values()) {
                    if (player != null && player.getPosition() != null) {
                        double healthBarWidth = Constants.PLAYER_SIZE;
                        double healthBarHeight = 5;
                        double healthBarX = player.getPosition().x;
                        double healthBarY = player.getPosition().y - healthBarHeight - 5;

                        gc.setFill(Color.GRAY);
                        gc.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

                        double healthPercentage = player.getHealth() / 100.0;
                        gc.setFill(Color.GREEN);
                        gc.fillRect(healthBarX, healthBarY, healthBarWidth * healthPercentage, healthBarHeight);

                        if (player.getId() == currentPlayerId) {
                            double dotSize = 6;
                            double dotX = healthBarX - 10 ;
                            double dotY = healthBarY + (healthBarHeight / 2) - (dotSize / 2);

                            gc.setFill(Color.BLACK);
                            gc.fillOval(dotX - 1, dotY - 1, dotSize + 2, dotSize + 2);

                            gc.setFill(Color.LIGHTGREEN);
                            gc.fillOval(dotX, dotY, dotSize, dotSize);
                        }
                    }
                }
            }
        }
    }
    private void renderDeathNotifications() {
        synchronized (gameStateLock) {
            double notificationY = 20;
            double maxWidth = Constants.GAME_WIDTH - 300;

            gc.setFont(Font.font(18));

            for (String notification : playerDeathNotifications.values()) {
                gc.setFill(notification.contains("won") ? VICTORY_NOTIFICATION_COLOR : DEATH_NOTIFICATION_COLOR);
                gc.fillText(notification, maxWidth, notificationY);
                notificationY += 25;
            }

            long currentTime = System.currentTimeMillis();
            for (Map.Entry<Integer, NotificationInfo> entry : playerNotifications.entrySet()) {
                NotificationInfo info = entry.getValue();

                long age = currentTime - info.timestamp;
                double opacity = 1.0 - (age / (double) NOTIFICATION_DISPLAY_TIME);
                if (opacity > 0) {
                    Color color = info.color.deriveColor(0, 1, 1, opacity);
                    gc.setFill(color);
                    gc.fillText(info.message, maxWidth, notificationY);
                    notificationY += 25;
                }
            }
        }
    }

    private void sendPacket(GamePacket packet) {
        try {
            if (out != null) {
                synchronized (out) {
                    out.reset();
                    out.writeObject(packet);
                    out.flush();
                }
            }
        } catch (IOException e) {
            handleNetworkError(e);
        }
    }

    private void handleNetworkError(Exception e) {
        System.err.println("Network error: " + e.getMessage());
        e.printStackTrace();
        if (isRunning) {
            isRunning = false;
            Platform.runLater(() -> {
                System.out.println("Disconnected from server");
            });
        }
    }

    @Override
    public void stop() {
        isRunning = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}