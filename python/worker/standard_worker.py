"""
Worker: encerra tarefas de prioridade normal.
GET /tasks/next/<fila> → PATCH done com mensagem de prioridade normal.
"""

import time

import httpx

import common_config as cfg
from common_api import patch_task


def main() -> None:
    url = f"{cfg.API_BASE_URL}/tasks/next/{cfg.QUEUE_NAME}"
    with httpx.Client(timeout=httpx.Timeout(120.0)) as client:
        while True:
            try:
                r = client.get(url)
            except httpx.HTTPError as e:
                print(f"[standard] erro no dequeue: {e}", flush=True)
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue

            if r.status_code == 204:
                print(f"[standard] nenhuma tarefa de prioridade normal em {cfg.QUEUE_NAME!r}", flush=True)
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue
            if not r.is_success:
                print(f"[standard] dequeue HTTP {r.status_code}: {r.text[:400]}", flush=True)
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue

            task = r.json()
            task_id = task["id"]
            attempts = int(task.get("attempts") or 0)
            pr = task.get("priority")
            prev = task.get("result") or {}
            print(f"[standard] tarefa {task_id} capturada da fila {cfg.QUEUE_NAME!r} | priority={pr} | attempts={attempts}", flush=True)

            try:
                if pr is None:
                    raise Exception("Tarefa sem priority na fila reports-standard")

                patch_task(
                    client,
                    task_id,
                    {
                        "status": "done",
                        "result": {
                            **prev,
                            "mensagem": "Processado (prioridade normal)",
                            "phase": "standard_done",
                            "priority": pr,
                        },
                    },
                )
                print(f"[standard] tarefa {task_id} finalizada com sucesso | status: done", flush=True)
            except Exception as e:
                print(f"[standard] tarefa {task_id} falha: {e} | tentativa {attempts}/{cfg.MAX_RETRY_ATTEMPTS}", flush=True)
                if attempts >= cfg.MAX_RETRY_ATTEMPTS:
                    patch_task(
                        client,
                        task_id,
                        {"status": "error", "result": {"error": str(e), "attempts": attempts}},
                    )
                    print(f"[standard] tarefa {task_id} marcada como error após {attempts} tentativas", flush=True)
                else:
                    patch_task(
                        client,
                        task_id,
                        {"status": "pending", "result": {**prev, "last_error": str(e)}},
                    )


if __name__ == "__main__":
    print(
        f"[standard] fila={cfg.QUEUE_NAME!r} poll={cfg.POLL_INTERVAL_SECONDS}s",
        flush=True,
    )
    main()