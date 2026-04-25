"""Somente o classificador usa: Groq API + prompt + piso de severidade."""

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
    parts = []
    if payload.get("title"):
        parts.append("Title: " + payload["title"])
    if payload.get("description"):
        parts.append("Description: " + payload["description"])
    if payload.get("type"):
        parts.append("Type: " + payload["type"])
    if payload.get("category"):
        parts.append("Category: " + payload["category"])
    if payload.get("sector"):
        parts.append("Sector: " + payload["sector"])
    if parts:
        return "\n".join(parts)
    return json.dumps(payload, ensure_ascii=False)[:8000]


def prioritize_with_groq(description: str) -> dict[str, Any]:
    from classification_prompt import SYSTEM_MSG, apply_severity_floor, user_prompt

    body = {
        "model": cfg.GROQ_MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_MSG},
            {"role": "user", "content": user_prompt(description)},
        ],
        "temperature": 0.15,
    }
    headers = {
        "Authorization": "Bearer " + cfg.GROQ_API_KEY,
        "Content-Type": "application/json",
    }
    with httpx.Client(timeout=httpx.Timeout(30.0)) as client:
        r = client.post("https://api.groq.com/openai/v1/chat/completions", json=body, headers=headers)
        r.raise_for_status()
        msg = r.json()["choices"][0]["message"]["content"]
    parsed = extract_json(msg)
    priority = int(parsed["priority"])
    label = str(parsed["priorityLabel"])
    if not (1 <= priority <= 10):
        raise ValueError("prioridade fora de 1-10: " + str(priority))
    priority, label, floored = apply_severity_floor(description, priority, label)
    out: dict[str, Any] = {
        "description": description,
        "priority": priority,
        "priority_label": label,
    }
    if floored:
        out["severity_floor_applied"] = True
    return out