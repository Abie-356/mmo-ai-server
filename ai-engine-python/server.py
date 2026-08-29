import grpc
from concurrent import futures
import time

# Import the generated files
import game_bridge_pb2
import game_bridge_pb2_grpc

# This class implements the AIEngine service we defined in the .proto file
class AIEngineServicer(game_bridge_pb2_grpc.AIEngineServicer):
    def SendState(self, request, context):
        # 1. Read the data coming from Java
        print(f"--> Received Game State from Java!")
        print(f"    Player ID: {request.player_id}")
        print(f"    Location: ({request.player_x}, {request.player_y})")
        print(f"    Action: {request.action}")

        # 2. Simulate the AI generating a script (we will add the real LLM later)
        fake_generated_script = '{"command": "moveTo", "targetX": 100, "targetY": 150}'
        
        # 3. Send the script back to Java
        print("<-- Sending BotScript back to Java\n")
        return game_bridge_pb2.BotScript(
            bot_id="npc_goblin_01",
            generated_command=fake_generated_script
        )

def serve():
    # Set up the gRPC server
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    game_bridge_pb2_grpc.add_AIEngineServicer_to_server(AIEngineServicer(), server)
    
    # Listen on port 50051
    server.add_insecure_port('[::]:50051')
    print("Python AI Server is starting...")
    server.start()
    print("Listening on port 50051. Waiting for Java Game Server...")
    
    # Keep the server running
    try:
        while True:
            time.sleep(86400)
    except KeyboardInterrupt:
        server.stop(0)

if __name__ == '__main__':
    serve()