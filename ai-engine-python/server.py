import grpc
import random
from concurrent import futures
import game_bridge_pb2
import game_bridge_pb2_grpc

class AIEngineServicer(game_bridge_pb2_grpc.AIEngineServicer):
    def SendState(self, request, context):
        player_x = request.player_x
        boss_x = request.player_y 
        action = request.action
        
        # 1. Respect the Dead
        if action == "Dead":
            return game_bridge_pb2.BotScript(generated_command="IDLE")
            
        king_center = player_x + 75.0
        boss_center = boss_x + 190.0
        center_distance = king_center - boss_center
        abs_distance = abs(center_distance)
        
        command = "IDLE"
        attack_range = 200.0 # Tighter range so he has to get close!

        # 2. Evade Parries
        if action == "Parrying":
            if center_distance < 0:
                command = "MOVE_RIGHT"
            else:
                command = "MOVE_LEFT"
                
        # 3. Dynamic Combat (Smart Attacks)
        elif abs_distance <= attack_range:
            move_roll = random.random()
            if move_roll < 0.5:
                command = "ATTACK"
            else:
                command = "SECONDARY_ATTACK"
                
        # 4. Pursuit & Tactical Jumping
        else:
            if center_distance < 0:
                command = "MOVE_LEFT"
            else:
                command = "MOVE_RIGHT"
                
            # 5% chance to jump while chasing to keep the player guessing
            if random.random() < 0.05:
                command += "_JUMP"

        return game_bridge_pb2.BotScript(generated_command=command)

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    game_bridge_pb2_grpc.add_AIEngineServicer_to_server(AIEngineServicer(), server)
    server.add_insecure_port('[::]:50051')
    print("Python AI Engine online. Awaiting combat data...")
    server.start()
    server.wait_for_termination()

if __name__ == '__main__':
    serve()