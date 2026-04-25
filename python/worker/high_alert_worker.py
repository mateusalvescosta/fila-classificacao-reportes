"""
Worker: alerta de prioridade alta.
GET /tasks/next/<fila> → dispara webhook → PATCH done.
"""

import time

import httpx

import common_config as cfg
from common_api import patch_task


def fire_webhook(client: httpx.Client, task: dict) -> bool:
    """Dispara o webhook com os dados da tarefa. Retorna True se bem-sucedido."""
    if not cfg.WEBHOOK_URL:
        print("[high_alert] WEBHOOK_URL não configurada, pulando disparo.", flush=True)
        return False

    payload = {
        "event": "high_priority_task",
        "task_id": task.get("id"),
        "queue": task.get("queue_name"),
        "priority": task.get("priority"),
        "payload": task.get("payload"),
        "result": task.get("result"),
    }

    try:
        r = client.post(
            cfg.WEBHOOK_URL,
            json=payload,
            timeout=httpx.Timeout(15.0),
        )
        if r.is_success:
            print(f"[high_alert] webhook disparado com sucesso | status={r.status_code}", flush=True)
            return True
        else:
            print(f"[high_alert] webhook retornou erro | status={r.status_code} body={r.text[:200]}", flush=True)
            return False
    except httpx.HTTPError as e:
        print(f"[high_alert] falha ao disparar webhook: {e}", flush=True)
        return False


def main() -> None:
    url = f"{cfg.API_BASE_URL}/tasks/next/{cfg.QUEUE_NAME}"
    with httpx.Client(timeout=httpx.Timeout(120.0)) as client:
        while True:
            try:
                r = client.get(url)
            except httpx.HTTPError as e:
                print(f"[high_alert] erro no dequeue: {e}", flush=True)
                time.sleep(cfg.POLL_INTERVAL_SECONDS)
                continue

            if r.status_code == 204:
                print(f"[high_alert] nenhuma tarefa de alta prioridade em {cfg.QUEUE_NAME!r}", flush=True)
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
            print(f"[high_alert] tarefa {task_id} capturada da fila {cfg.QUEUE_NAME!r} | priority={pr} | attempts={attempts}", flush=True)
            print(f"[high_alert] tarefa {task_id} — prioridade ALTA detectada, acionando webhook...", flush=True)

            try:
                webhook_ok = fire_webhook(client, task)
                if not webhook_ok:
                    raise Exception("Falha ao disparar webhook")
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
                            "webhook_fired": webhook_ok,
                        },
                    },
                )
                print(f"[high_alert] tarefa {task_id} finalizada com sucesso | status: done | webhook_fired: {webhook_ok}", flush=True)
            except Exception as e:
                print(f"[high_alert] tarefa {task_id} falha ao disparar webhook | tentativa {attempts}/{cfg.MAX_RETRY_ATTEMPTS}", flush=True)
                if attempts >= cfg.MAX_RETRY_ATTEMPTS:
                    patch_task(
                        client,
                        task_id,
                        {"status": "error", "result": {"error": str(e), "attempts": attempts}},
                    )
                    print(f"[high_alert] tarefa {task_id} marcada como error após {attempts} tentativas", flush=True)
                else:
                    patch_task(
                        client,
                        task_id,
                        {"status": "pending", "result": {**prev, "last_error": str(e)}},
                    )


if __name__ == "__main__":
    print(
        f"[high_alert] fila={cfg.QUEUE_NAME!r} poll={cfg.POLL_INTERVAL_SECONDS}s webhook={cfg.WEBHOOK_URL!r}",
        flush=True,
    )
    main()