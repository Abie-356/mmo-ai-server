import grpc
import time
import threading
import random
from concurrent import futures
import game_bridge_pb2
import game_bridge_pb2_grpc

class AIEngineServicer(game_bridge_pb2_grpc.AIEngineServicer):
    def __init__(self):
        self.active_players = {}
        self.player_last_seen = {} 
        self.lock = threading.Lock()
        self.boss_attack_cooldown = 0.0 

        self.current_target_id = None
        
        self.phase = 1
        self.enemies = {}
        self.awakened_mimics = set()
        
        self.game_started = False
        self.lobby_start_time = float('inf')
        
        self.boss = game_bridge_pb2.EntityState(
            id="Boss", character="Necromancer", x=750.0, y=19.0, action="Idle", isFacingLeft=True
        )
        
        self.ai_thread = threading.Thread(target=self.boss_ai_loop, daemon=True)
        self.ai_thread.start()

    def boss_ai_loop(self):
        last_time = time.time()
        while True:
            if not self.game_started:
                time.sleep(0.03)
                last_time = time.time() # Keeps clock perfectly synced while waiting
                continue
                
            time.sleep(0.03) 
            current_time = time.time()
            dt = current_time - last_time
            last_time = current_time
            
            try:
                with self.lock:
                    # 1. CLEANUP AFK PLAYERS (Increased to 15 seconds for Wi-Fi safety)
                    dead_players = [pid for pid, last_seen in self.player_last_seen.items() if current_time - last_seen > 15.0]
                    for pid in dead_players:
                        if pid in self.active_players: del self.active_players[pid]
                        if pid in self.player_last_seen: del self.player_last_seen[pid]

                    # 2. AUTO-RESET IF EMPTY
                    if not self.active_players:
                        print("Lobby empty. Resetting arena for next match...")
                        self.game_started = False 
                        self.current_target_id = None
                        self.boss.x = 750.0
                        self.boss.action = "Idle"
                        self.boss.isFacingLeft = True
                        self.phase = 1 
                        self.enemies.clear()
                        self.awakened_mimics.clear()
                        continue

                    # 3. PHASE SPAWNING & CINEMATIC WAIT STATE
                    if self.boss.action == "Dead":
                        if self.phase > 3:
                            continue

                        if time.time() > self.lobby_start_time:
                            print(f"Spawning Phase {self.phase}!")
                            self.awakened_mimics.clear()
                            self.boss.action = "Idle"
                            self.boss.x = 750.0
                            self.boss_attack_cooldown = 2.0
                            
                            # SPAWN MINIONS
                            if self.phase == 2:
                                self.enemies["Enemy_Bat1"] = game_bridge_pb2.EntityState(id="Enemy_Bat1", character="Bat", x=800.0, y=150, action="Idle", isFacingLeft=True)
                                self.enemies["Enemy_Slime1"] = game_bridge_pb2.EntityState(id="Enemy_Slime1", character="Slime", x=900.0, y=75, action="Idle", isFacingLeft=True)
                            elif self.phase >= 3:
                                self.enemies["Enemy_Bat1"] = game_bridge_pb2.EntityState(id="Enemy_Bat1", character="Bat", x=800.0, y=150.0, action="Idle", isFacingLeft=True)
                                self.enemies["Enemy_Slime1"] = game_bridge_pb2.EntityState(id="Enemy_Slime1", character="Slime", x=600.0, y=75.0, action="Idle", isFacingLeft=True)
                                self.enemies["Enemy_Mimic1"] = game_bridge_pb2.EntityState(id="Enemy_Mimic1", character="Mimic", x=900.0, y=75.0, action="Idle", isFacingLeft=True)
                                self.enemies["Enemy_Rat1"] = game_bridge_pb2.EntityState(id="Enemy_Rat1", character="Rat", x=1000.0, y=110.0, action="Idle", isFacingLeft=True)
                        else:
                            continue # Freeze AI completely while video finishes
                            
                    # Freeze Boss if waiting for a cinematic to finish
                    if time.time() < self.lobby_start_time:
                        self.boss.action = "Idle"
                        continue

                    # 4. BOSS COOLDOWN
                    if self.boss_attack_cooldown > 0:
                        self.boss_attack_cooldown -= dt
                        continue
                        
                    # 5. BOSS TARGET ACQUISITION
                    alive_players = {p.id: p for p in self.active_players.values() if p.action != "Dead"}
                    if not alive_players:
                        self.boss.action = "Idle"
                        self.current_target_id = None
                        continue
                        
                    player_list = list(alive_players.values())
                    if self.current_target_id not in alive_players:
                        new_target = min(player_list, key=lambda p: abs((p.x + (125.0 if p.character == "Sekiro" else 75.0)) - (self.boss.x + 190.0)))
                        self.current_target_id = new_target.id
                    else:
                        current_target = alive_players[self.current_target_id]
                        current_dist = abs((current_target.x + (125.0 if current_target.character == "Sekiro" else 75.0)) - (self.boss.x + 190.0))
                        closest_player = min(player_list, key=lambda p: abs((p.x + (125.0 if p.character == "Sekiro" else 75.0)) - (self.boss.x + 190.0)))
                        closest_dist = abs((closest_player.x + (125.0 if closest_player.character == "Sekiro" else 75.0)) - (self.boss.x + 190.0))
                        if closest_dist < current_dist - 150.0: 
                            self.current_target_id = closest_player.id

                    # 6. BOSS COMBAT LOGIC
                    target_player = alive_players[self.current_target_id]
                    p_center = target_player.x + (125.0 if target_player.character == "Sekiro" else 75.0)
                    b_center = self.boss.x + 190.0
                    dist = abs(p_center - b_center)
                    
                    if dist <= 175.0: 
                        self.boss.action = "Attacking" if random.random() < 0.5 else "SecondaryAttacking"
                        self.boss.isFacingLeft = (p_center < b_center)
                        self.boss_attack_cooldown = 0.8 
                    else:
                        self.boss.action = "Running"
                        boss_speed = 160.0
                        if p_center < b_center:
                            self.boss.x -= boss_speed * dt
                            self.boss.isFacingLeft = True
                        else:
                            self.boss.x += boss_speed * dt
                            self.boss.isFacingLeft = False
                            
                        if random.random() < 0.05:
                            self.boss.action = "Jump"

                    # 7. MINION AI
                    for enemy_id, enemy in self.enemies.items():
                        closest_e_target = min(player_list, key=lambda p: abs(p.x - enemy.x))
                        dist_e = abs(closest_e_target.x - enemy.x)
                        
                        if enemy.character == "Mimic":
                            if enemy_id not in self.awakened_mimics:
                                if dist_e <= 200.0:
                                    self.awakened_mimics.add(enemy_id)
                                else:
                                    enemy.action = "Idle"
                                    continue 
                        
                        if enemy.character == "Rat": e_speed = 150.0
                        elif enemy.character == "Bat": e_speed = 120.0
                        elif enemy.character == "Mimic": e_speed = 140.0
                        else: e_speed = 80.0 
                        
                        if dist_e <= 80.0:
                            enemy.action = "Attacking"
                            enemy.isFacingLeft = (closest_e_target.x < enemy.x)
                        else:
                            enemy.action = "Running"
                            if closest_e_target.x < enemy.x:
                                enemy.x -= e_speed * dt
                                enemy.isFacingLeft = True
                            else:
                                enemy.x += e_speed * dt
                                enemy.isFacingLeft = False

            except Exception as e:
                print(f"CRITICAL AI ERROR: {e}")

    def JoinLobby(self, request_iterator, context):
        with self.lock:
            if not self.game_started:
                print("First player arrived in arena! Starting combat...")
                self.game_started = True
                self.lobby_start_time = time.time() + 2.0 
            
        player_id = None
        try:
            for player_state in request_iterator:
                if player_id is None:
                    player_id = player_state.id
                    print(f"Player {player_id} joined the lobby!")

                with self.lock:
                    if player_state.action == "KILLED_BOSS" and self.boss.action != "Dead":
                        print(f"Boss Defeated by {player_state.id}! Triggering Phase Transition...")
                        self.boss.action = "Dead"
                        self.phase += 1
                        self.lobby_start_time = time.time() + 10.0 
                        self.enemies.clear() 

                    # CRITICAL FIX: Make a deep copy to prevent Protobuf memory corruption!
                    safe_state = game_bridge_pb2.EntityState()
                    safe_state.CopyFrom(player_state)
                    
                    self.active_players[safe_state.id] = safe_state
                    self.player_last_seen[safe_state.id] = time.time() 
                    
                    world_update = game_bridge_pb2.GameWorldUpdate()
                    world_update.players.extend(self.active_players.values())
                    world_update.boss.CopyFrom(self.boss)
                    world_update.enemies.extend(self.enemies.values()) 
                
                yield world_update

        except Exception as e:
            print(f"Stream Error: {e}")
        finally:
            if player_id in self.active_players:
                with self.lock:
                    if player_id in self.active_players: del self.active_players[player_id]
                    if player_id in self.player_last_seen: del self.player_last_seen[player_id]
                print(f"Player {player_id} left.")

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    game_bridge_pb2_grpc.add_AIEngineServicer_to_server(AIEngineServicer(), server)
    server.add_insecure_port('[::]:50051')
    print("Multiplayer AI Lobby Server online on port 50051...")
    server.start()
    server.wait_for_termination()

if __name__ == '__main__':
    serve()