"""
Prompt e piso de severidade usados pelo worker classificador (Ollama).
Evita classificação absurda (ex.: incidente grave como prioridade 1).
"""

from __future__ import annotations

import re

SYSTEM_MSG = """Você é um classificador de prioridade operacional (português do Brasil).
Siga as regras de negócio: incidentes que derrubam produção ou afetam muitos clientes NUNCA podem ser prioridade baixa.
Responda APENAS um objeto JSON com as chaves exatas "priority" (inteiro 1-10) e "priorityLabel" (uma palavra curta: baixa, média ou alta).""".strip()


def user_prompt(description: str) -> str:
    return f"""Contexto: fila de tarefas para operações ou suporte técnico.

Escala (priority):
- 1-3: rotinas, dúvidas internas, baixo impacto para clientes.
- 4-6: incômodo moderado, prazo normal.
- 7-10: impacto forte a crítico em clientes ou negócio; precisa resposta urgente.

Regras obrigatórias:
- Se houver produção/serviço EM BAIXO, PARADO, INDISPONÍVEL, ou clientes SEM ACESSO / TODOS OS CLIENTES afetados, use pelo menos 8 (em geral 9 ou 10).
- Não use 1-3 para indisponibilidade de produção ou acesso em massa a clientes.

Exemplos:
- "Ajustar typo no relatório interno" -> {{"priority": 2, "priorityLabel": "baixa"}}
- "Pedido de reembolso atrasado (1 cliente)" -> {{"priority": 5, "priorityLabel": "média"}}
- "Produção em baixo, todos os clientes sem acesso" -> {{"priority": 10, "priorityLabel": "alta"}}

Texto a classificar:
{description!r}

Responda só com JSON neste formato, sem markdown nem texto extra:
{{"priority": <número>, "priorityLabel": "<texto>"}}
"""


_INCIDENT_PATTERNS = (
    r"produ[cç][aã]o\s+em\s+baixo",
    r"produ[cç][aã]o\s+(parad|parada|indispon[ií]v|fora|down)",
    r"serv[ií]c(io|os)\s+(principal\s+)?(em\s+baixo|parad|indispon|fora|down)",
    r"todos\s+os\s+clientes",
    r"client(es)?\s+sem\s+acesso",
    r"\bsem\s+acesso\b",
    r"indispon[ií]v(el|íveis)",
    r"parad[ao]\s+total",
    r"perda\s+de\s+(receita|dados)",
    r"viola[cç][aã]o\s+de\s+seguran",
    r"\bcr[ií]tic[oa]\b",
)


def apply_severity_floor(description: str, priority: int, label: str) -> tuple[int, str, bool]:
    """Se a descrição indica incidente grave e o modelo respondeu baixo, sobe o piso."""
    text = description.lower()
    severe = any(re.search(p, text, re.IGNORECASE) for p in _INCIDENT_PATTERNS)
    if severe and priority < 7:
        return 9, "alta", True
    return priority, label, False