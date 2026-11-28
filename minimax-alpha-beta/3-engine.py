"""Tic-tac-toe: plain minimax vs alpha-beta, with node counters."""
LINES = [(0,1,2),(3,4,5),(6,7,8),(0,3,6),(1,4,7),(2,5,8),(0,4,8),(2,4,6)]

def winner(b):
    for a, c, d in LINES:
        if b[a] != "." and b[a] == b[c] == b[d]:
            return b[a]
    return None

def moves(b):
    return [i for i, v in enumerate(b) if v == "."]

def score(b):
    w = winner(b)
    return 1 if w == "X" else -1 if w == "O" else 0

def minimax(b, player, stats):
    stats["nodes"] += 1
    if winner(b) or not moves(b):
        return score(b), None
    best, best_m = (-2, None) if player == "X" else (2, None)
    for m in moves(b):
        child = b[:m] + player + b[m+1:]
        v, _ = minimax(child, "O" if player == "X" else "X", stats)
        if (player == "X" and v > best) or (player == "O" and v < best):
            best, best_m = v, m
    return best, best_m

def alphabeta(b, player, alpha, beta, stats):
    stats["nodes"] += 1
    if winner(b) or not moves(b):
        return score(b), None
    best_m = None
    if player == "X":
        best = -2
        for m in moves(b):
            v, _ = alphabeta(b[:m] + "X" + b[m+1:], "O", alpha, beta, stats)
            if v > best: best, best_m = v, m
            alpha = max(alpha, best)
            if alpha >= beta:
                stats["cuts"] += 1
                break
    else:
        best = 2
        for m in moves(b):
            v, _ = alphabeta(b[:m] + "O" + b[m+1:], "X", alpha, beta, stats)
            if v < best: best, best_m = v, m
            beta = min(beta, best)
            if alpha >= beta:
                stats["cuts"] += 1
                break
    return best, best_m

if __name__ == "__main__":
    import json
    tests = {"empty": "." * 9,
             "X center, O corner": "O...X....",
             "midgame": "XO..X...O"}
    out = {}
    for name, b in tests.items():
        s1 = {"nodes": 0}; s2 = {"nodes": 0, "cuts": 0}
        n = sum(1 for c in b if c != ".")
        p = "X" if n % 2 == 0 else "O"
        v1, m1 = minimax(b, p, s1)
        v2, m2 = alphabeta(b, p, -2, 2, s2)
        out[name] = dict(board=b, to_move=p, minimax=dict(value=v1, move=m1, nodes=s1["nodes"]),
                         alphabeta=dict(value=v2, move=m2, nodes=s2["nodes"], cuts=s2["cuts"]))
        print(name, out[name])
    json.dump(out, open("results.json", "w"), indent=1)
