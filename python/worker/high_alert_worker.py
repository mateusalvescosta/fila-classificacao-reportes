"""
Worker: alerta de prioridade alta.
GET /tasks/next/<fila>/high-priority → PATCH done com mensagem "Prioridade alta".
"""

import time

import httpx

import common_config as cfg
from common_api import patch_task


def main() -> None:
    url = f"{cfg.API_BASE_URL}/tasks/next/{cfg.QUEUE_NAME}/high-priority"
    with httpx.Client(timeout=httpx.Timeout(120.0)) as client:
        while True:
            try:
                r = client.get(url, params={"min": cfg.HIGH_PRIORITY_MIN})
            except httpx.HTTPError as e:
                print(f"[high_alert] erro no dequeue: {e}", flush=True)
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue

            if r.status_code == 204:
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue
            if not r.is_success:
                print(f"[high_alert] dequeue HTTP {r.status_code}: {r.text[:400]}", flush=True)
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue

            task = r.json()
            task_id = task["id"]
            attempts = int(task.get("attempts") or 0)
            pr = task.get("priority")
            prev = task.get("result") or {}
            print(f"[high_alert] tarefa {task_id} priority={pr}", flush=True)

            try:
                patch_task(
                    client,
                    task_id,
                    {
                        "status": "done",
                        "result": {
                            **prev,
                            "mensagem": "Prioridade alta",
                            "phase": "high_alert_done",
                            "priority": pr,
                        },
                    },
                )
                print(f"[high_alert] concluída {task_id}", flush=True)
            except Exception as e:
                print(f"[high_alert] falha {task_id}: {e}", flush=True)
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
                        {"status": "pending", "result": {**prev, "last_error": str(e)}},
                    )


if __name__ == "__main__":
    print(
        f"[high_alert] fila={cfg.QUEUE_NAME!r} min={cfg.HIGH_PRIORITY_MIN} "
        f"poll={cfg.POLL_INTERVAL_SECONDS}s",
        flush=True,
    )
    main()
