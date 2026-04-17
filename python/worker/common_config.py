"""Variáveis de ambiente lidas uma vez (compartilhado pelos workers)."""

import os

API_BASE_URL = os.environ["API_BASE_URL"].rstrip("/")
QUEUE_NAME = os.environ["QUEUE_NAME"].strip()
if not QUEUE_NAME:
    raise RuntimeError("QUEUE_NAME não pode ser vazio")
POLL_INTERVAL_SECONDS = float(os.environ.get("POLL_INTERVAL_SECONDS", "5"))
MAX_RETRY_ATTEMPTS = int(os.environ.get("MAX_RETRY_ATTEMPTS", "5"))
OLLAMA_URL = os.environ.get("OLLAMA_URL", "http://127.0.0.1:11434").rstrip("/")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "llama3.2")
HIGH_PRIORITY_MIN = int(os.environ.get("HIGH_PRIORITY_MIN", "8"))
