package com.mmo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.mmo.grpc.AIEngineGrpc;
import com.mmo.grpc.GameBridgeProto;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Java Game Server Connection...");

        // 1. Open the gRPC bridge to the Python server on port 50051
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
            .usePlaintext()
            .build();

        // 2. Create the client stub
        AIEngineGrpc.AIEngineBlockingStub stub = AIEngineGrpc.newBlockingStub(channel);

        // 3. Create a mock game state using the explicit proto wrapper
        GameBridgeProto.GameState state = GameBridgeProto.GameState.newBuilder()
            .setPlayerId("Abishek_Warrior")
            .setPlayerX(150.5f)
            .setPlayerY(200.0f)
            .setAction("Attacking the goblin with a sword")
            .build();

        System.out.println("--> Sending GameState to Python AI...");

        // 4. Send the state and wait for the response
        GameBridgeProto.BotScript response = stub.sendState(state);

        // 5. Print the generated script from Python
        System.out.println("<-- Received AI Script for Bot: " + response.getBotId());
        System.out.println("    AI Command: " + response.getGeneratedCommand());

        // 6. Close the bridge
        channel.shutdown();
    }
}