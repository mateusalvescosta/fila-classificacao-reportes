"""
Worker: classificação com IA.
GET /tasks/next/<fila>/unclassified → Groq → PATCH pending com priority e queue_name.
"""

import time

import httpx

import common_config as cfg
from common_api import patch_task
from llm_classify import description_from_payload, prioritize_with_groq

REQUIRED_FIELDS = ["title", "description", "type", "category", "sector"]


def validate_payload(payload: dict) -> list[str]:
    missing = []
    for field in REQUIRED_FIELDS:
        value = payload.get(field)
        if not value or not str(value).strip():
            missing.append(field)
    return missing


def target_queue(priority: int) -> str:
    if priority >= cfg.HIGH_PRIORITY_MIN:
        return f"{cfg.QUEUE_NAME}-high"
    return f"{cfg.QUEUE_NAME}-standard"


def main() -> None:
    url = f"{cfg.API_BASE_URL}/tasks/next/{cfg.QUEUE_NAME}/unclassified"
    with httpx.Client(timeout=httpx.Timeout(120.0)) as client:
        while True:
            try:
                r = client.get(url)
            except httpx.HTTPError as e:
                print(f"[classifier] erro no dequeue: {e}", flush=True)
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue

            if r.status_code == 204:
                print(f"[classifier] nenhuma tarefa disponível em {cfg.QUEUE_NAME!r}", flush=True)
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue
            if not r.is_success:
                print(f"[classifier] dequeue HTTP {r.status_code}: {r.text[:400]}", flush=True)
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue

            task = r.json()
            task_id = task["id"]
            attempts = int(task.get("attempts") or 0)
            payload = task.get("payload") or {}
            print(f"[classifier] tarefa {task_id} capturada da fila {cfg.QUEUE_NAME!r} | attempts={attempts}", flush=True)

            missing = validate_payload(payload)
            if missing:
                print(f"[classifier] tarefa {task_id} campos ausentes: {missing} | tentativa {attempts}/{cfg.MAX_RETRY_ATTEMPTS}", flush=True)
                if attempts >= cfg.MAX_RETRY_ATTEMPTS:
                    patch_task(client, task_id, {
                        "status": "error",
                        "result": {
                            "error": "Missing required fields after max retries",
                            "missing_fields": missing,
                            "attempts": attempts,
                        },
                    })
                    print(f"[classifier] tarefa {task_id} marcada como error após {attempts} tentativas", flush=True)
                else:
                    patch_task(client, task_id, {
                        "status": "pending",
                        "result": {
                            "last_error": "Missing required fields",
                            "missing_fields": missing,
                        },
                    })
                continue

            try:
                desc = description_from_payload(payload)
                scored = prioritize_with_groq(desc)
                pr = int(scored["priority"])
                queue = target_queue(pr)
                patch_task(
                    client,
                    task_id,
                    {
                        "status": "pending",
                        "priority": pr,
                        "queue_name": queue,
                        "result": {**scored, "phase": "classified_queued"},
                    },
                )
                print(f"[classifier] tarefa {task_id} classificada com sucesso | priority={pr} | label={scored['priority_label']} | fila={queue!r}", flush=True)
            except Exception as e:
                print(f"[classifier] falha {task_id}: {e}", flush=True)
                if attempts >= cfg.MAX_RETRY_ATTEMPTS:
                    patch_task(
                        client,
                        task_id,
                        {"status": "error", "result": {"error": str(e), "attempts": attempts}},
                    )
                else:
                    patch_task(
                        client,
                        task_id,
                        {"status": "pending", "result": {"last_error": str(e)}},
                    )


if __name__ == "__main__":
    print(
        f"[classifier] fila={cfg.QUEUE_NAME!r} poll={cfg.POLL_INTERVAL_SECONDS}s",
        flush=True,
    )
    main()