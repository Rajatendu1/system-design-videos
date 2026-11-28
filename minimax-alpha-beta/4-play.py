"""Play interface + exhaustive never-loses proof for the alpha-beta engine."""
from engine import *
import sys

def best_move(b, player):
    return alphabeta(b, player, -2, 2, {"nodes": 0, "cuts": 0})[1]

def other(p): return "O" if p == "X" else "X"

def prove(engine_side):
    """Engine plays engine_side against EVERY legal opponent move at every turn."""
    games = wins_for_opp = engine_wins = draws = 0
    def rec(b, p):
        nonlocal games, wins_for_opp, engine_wins, draws
        w = winner(b)
        if w or not moves(b):
            games += 1
            if w == engine_side: engine_wins += 1
            elif w: wins_for_opp += 1
            else: draws += 1
            return
        if p == engine_side:
            m = best_move(b, p); rec(b[:m]+p+b[m+1:], other(p))
        else:
            for m in moves(b): rec(b[:m]+p+b[m+1:], other(p))
    rec("."*9, "X")
    return dict(engine=engine_side, games=games, engine_wins=engine_wins, draws=draws, engine_losses=wins_for_opp)

def show(b): return "\n".join(" ".join(b[r*3:r*3+3]) for r in range(3))

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "proof":
        for s in "XO": print(prove(s))
    else:  # human plays O (enter 0-8), engine plays X
        b, p = "."*9, "X"
        while not (winner(b) or not moves(b)):
            if p == "X": m = best_move(b, p)
            else:
                print(show(b)); m = int(input("your square 0-8: "))
                if m not in moves(b): continue
            b = b[:m]+p+b[m+1:]; p = other(p)
        print(show(b)); print("winner:", winner(b) or "draw")
