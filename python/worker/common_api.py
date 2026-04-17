"""Chamadas HTTP à API central (PATCH tarefa)."""

from typing import Any

import httpx

import common_config as cfg


def patch_task(client: httpx.Client, task_id: str, body: dict[str, Any]) -> None:
    r = client.patch(f"{cfg.API_BASE_URL}/tasks/{task_id}", json=body)
    r.raise_for_status()
