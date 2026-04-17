#!/bin/sh
set -e
ollama serve &
OLLAMA_PID=$!
i=0
while ! ollama list >/dev/null 2>&1; do
  i=$((i + 1))
  if [ "$i" -gt 60 ]; then
    echo "Ollama não arrancou a tempo" >&2
    exit 1
  fi
  sleep 1
done
ollama pull "${OLLAMA_MODEL:-llama3.2}"
wait "$OLLAMA_PID"
