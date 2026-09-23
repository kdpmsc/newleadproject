#!/bin/sh
set -e

ollama serve &
OLLAMA_PID=$!

until ollama list >/dev/null 2>&1; do
    sleep 2
done

MODEL="${OLLAMA_MODEL:-llama3.2:3b}"
ollama pull "$MODEL"

ollama run "$MODEL" "Reply with OK only." >/dev/null

wait "$OLLAMA_PID"
