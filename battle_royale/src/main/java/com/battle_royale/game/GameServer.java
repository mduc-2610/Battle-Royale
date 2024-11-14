package com.battle_royale.game;

import com.battle_royale.network.GamePacket;
import com.battle_royale.network.PacketType;
import com.battle_royale.model.Player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private ServerSocket serverSocket;
    private final int port = 5000;
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final GameState gameState;
    private boolean isRunning = true;

    public GameServer() {
        this.gameState = new GameState();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            new Thread(this::gameLoop).start();

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                int playerId = clients.size() + 1;
                ClientHandler clientHandler = new ClientHandler(clientSocket, playerId);
                clients.put(playerId, clientHandler);
                new Thread(clientHandler).start();
                System.out.println("New player connected: " + playerId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void gameLoop() {
        final int FPS = 60;
        final long frameTime = 1000 / FPS;

        while (isRunning) {
            long startTime = System.currentTimeMillis();

            gameState.update();
            broadcastGameState();

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


    private void broadcastGameState() {
        GamePacket packet = new GamePacket(PacketType.GAME_STATE, gameState);
        Map<Integer, ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new HashMap<>(clients);
        }
//        clients.values().forEach(client -> {
//            client.sendPacket(packet);
//        });

        for (ClientHandler client : clientsSnapshot.values()) {
            client.sendPacket(packet);
        }
    }


    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerId;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private volatile boolean isRunning = true;


        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            try {
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            try {
                while (isRunning) {
                    try {
                        Object packetObj = in.readObject();
                        if (packetObj instanceof GamePacket) {
                            GamePacket packet = (GamePacket) packetObj;
                            handlePacket(packet);
                        }
                    } catch (ClassNotFoundException e) {
                        System.err.println("Invalid packet received from player " + playerId);
                        e.printStackTrace();
                    } catch (IOException e) {
                        System.out.println("Player " + playerId + " disconnected");
                        isRunning = false;
                    }
                }
            } finally {
                cleanup();
            }
        }



        private void cleanup() {
            isRunning = false;
            System.out.println("Player " + playerId + " disconnected");

            clients.remove(playerId);
            gameState.removePlayer(playerId);

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            broadcastGameState();
        }

        private void handlePacket(GamePacket packet) {
            switch (packet.type) {
                case PLAYER_JOIN:
                    sendPacket(new GamePacket(PacketType.PLAYER_ID_ASSIGN, playerId));
                    gameState.addPlayer(playerId);
                    broadcastGameState();
                    break;
                case PLAYER_INPUT:
                    gameState.updatePlayerInput(playerId, (Player.Input) packet.data);
                    break;
                default:
                    break;
            }
        }


        public void sendPacket(GamePacket packet) {
            synchronized (out) {
                try {
                    if (out != null && socket != null && !socket.isClosed()) {
                        out.reset();
                        out.writeObject(packet);
                        out.flush();
                    }
                } catch (IOException e) {
                    System.err.println("Error sending packet to player " + playerId);
                    isRunning = false;
                    cleanup();
                }
            }
        }
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}