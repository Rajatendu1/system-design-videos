# Minimax vs Alpha-Beta: tic-tac-toe engine

Files
- engine.py: minimax and alphabeta, both count nodes visited.
- play.py: best_move, a small play CLI, and a never-loses check.
- test_engine.py: checks both searches agree on every reachable board.

Run

```bash
python3 test_engine.py
python3 play.py proof
python3 play.py
```

`play.py proof` plays the engine against every legal opponent reply. `play.py` alone lets you play O against the engine (enter a square 0-8).

Board: a 9-character string, X, O or "." ; squares 0-8, top-left to bottom-right.
Counts are tree visits, not unique positions. Results hold for this engine with moves tried in order 0 to 8.
Try it: change the move order in engine.py and watch the counters.
