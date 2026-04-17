"""Somente o classificador usa: Ollama + prompt + piso de severidade."""

from __future__ import annotations

import json
import re
from typing import Any

import httpx

import common_config as cfg


def extract_json(text: str) -> dict[str, Any]:
    text = text.strip()
    m = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", text, re.IGNORECASE)
    if m:
        text = m.group(1).strip()
    return json.loads(text)


def description_from_payload(payload: dict[str, Any]) -> str:
    d = payload.get("description")
    if isinstance(d, str) and d.strip():
        return d.strip()
    return json.dumps(payload, ensure_ascii=False)[:8000]


def prioritize_with_ollama(description: str) -> dict[str, Any]:
    from classification_prompt import SYSTEM_MSG, apply_severity_floor, user_prompt

    body = {
        "model": cfg.OLLAMA_MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_MSG},
            {"role": "user", "content": user_prompt(description)},
        ],
        "stream": False,
        "options": {"temperature": 0.15},
    }
    with httpx.Client(timeout=httpx.Timeout(120.0)) as client:
        r = client.post(f"{cfg.OLLAMA_URL}/api/chat", json=body)
        if r.status_code == 404:
            raise RuntimeError(
                "Ollama retornou 404 em /api/chat (modelo ausente ou pull incompleto). "
                f"Modelo: {cfg.OLLAMA_MODEL!r}. Resposta: {r.text[:500]}"
            )
        r.raise_for_status()
        msg = r.json()["message"]["content"]
    parsed = extract_json(msg)
    priority = int(parsed["priority"])
    label = str(parsed["priorityLabel"])
    if not (1 <= priority <= 10):
        raise ValueError(f"prioridade fora de 1-10: {priority}")
    priority, label, floored = apply_severity_floor(description, priority, label)
    out: dict[str, Any] = {
        "description": description,
        "priority": priority,
        "priority_label": label,
    }
    if floored:
        out["severity_floor_applied"] = True
    return out
