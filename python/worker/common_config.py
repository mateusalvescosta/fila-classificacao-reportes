"""Variáveis de ambiente lidas uma vez (compartilhado pelos workers)."""

import os

API_BASE_URL = os.environ["API_BASE_URL"].rstrip("/")
QUEUE_NAME = os.environ["QUEUE_NAME"].strip()
if not QUEUE_NAME:
    raise RuntimeError("QUEUE_NAME não pode ser vazio")
POLL_INTERVAL_SECONDS = float(os.environ.get("POLL_INTERVAL_SECONDS", "5"))
MAX_RETRY_ATTEMPTS = int(os.environ.get("MAX_RETRY_ATTEMPTS", "3"))
HIGH_PRIORITY_MIN = int(os.environ.get("HIGH_PRIORITY_MIN", "8"))
WEBHOOK_URL = os.environ.get("WEBHOOK_URL", "https://4f97db27deb1c0419c48cc0c0029b26c.m.pipedream.net")
GROQ_API_KEY = os.environ.get("GROQ_API_KEY", "")
GROQ_MODEL = os.environ.get("GROQ_MODEL", "llama-3.1-8b-instant")