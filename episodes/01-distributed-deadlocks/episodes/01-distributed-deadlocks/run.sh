#!/bin/sh
# Compile, test, and run the episode's simulation with nothing but a JDK.
set -e
cd "$(dirname "$0")"

echo "== Compiling =="
mkdir -p out
javac -d out $(find src -name '*.java')

echo "== Tests =="
java -cp out deadlock.TestRunner

echo "== Simulation =="
java -cp out deadlock.Simulation
