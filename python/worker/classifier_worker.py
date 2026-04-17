"""
Worker: classificação com IA.
GET /tasks/next/<fila>/unclassified → Ollama → PATCH pending com priority.
"""

import time

import httpx

import common_config as cfg
from common_api import patch_task
from llm_classify import description_from_payload, prioritize_with_ollama


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
            print(f"[classifier] tarefa {task_id} attempts={attempts}", flush=True)

            try:
                desc = description_from_payload(payload)
                scored = prioritize_with_ollama(desc)
                pr = int(scored["priority"])
                patch_task(
                    client,
                    task_id,
                    {
                        "status": "pending",
                        "priority": pr,
                        "result": {**scored, "phase": "classified_queued"},
                    },
                )
                print(f"[classifier] reenfileirada prioridade={pr} {task_id}", flush=True)
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
