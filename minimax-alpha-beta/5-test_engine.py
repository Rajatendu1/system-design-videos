"""Run: python3 test_engine.py   (takes a few seconds)"""
from engine import *
from play import prove

def test_counts():
    s1, s2 = {"nodes": 0}, {"nodes": 0, "cuts": 0}
    minimax("." * 9, "X", s1); alphabeta("." * 9, "X", -2, 2, s2)
    assert (s1["nodes"], s2["nodes"]) == (549946, 18297)

def test_agree_everywhere():
    seen = set()
    def walk(b, p):
        if b in seen: return
        seen.add(b)
        if winner(b) or not moves(b): return
        assert minimax(b, p, {"nodes": 0}) == alphabeta(b, p, -2, 2, {"nodes": 0, "cuts": 0})
        for m in moves(b): walk(b[:m] + p + b[m+1:], "O" if p == "X" else "X")
    walk("." * 9, "X")
    assert len(seen) == 5478

def test_never_loses():
    assert prove("X")["engine_losses"] == 0
    assert prove("O")["engine_losses"] == 0

if __name__ == "__main__":
    test_counts(); test_agree_everywhere(); test_never_loses(); print("all tests passed")
