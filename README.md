# Fila de Classificação de Reportes

**Disciplina:** Sistemas Distribuídos e Infraestrutura em Nuvem  
**Grupo:** Mateus Alves Costa, Carlos Eduardo Pisa Meireles e Guilherme de Amorim Gomes


## O que é o sistema?

O sistema é uma fila distribuída de classificação de reportes operacionais. Quando um usuário abre um reporte informando título, descrição, tipo de ocorrência, setor e categoria, o sistema classifica automaticamente a prioridade desse reporte usando IA (Groq/LLaMA), sem necessidade de análise humana prévia. Com base na prioridade classificada, o reporte é encaminhado para a fila correta e processado pelo worker responsável.


## Stack Tecnológica

| Camada | Tecnologia |
|---|---|
| API | Java 21, Spring Boot 3.5 |
| Banco de Dados | PostgreSQL 16 |
| Workers | Python 3.12 |
| IA | Groq API (llama-3.1-8b-instant) |
| Infraestrutura | Docker, Docker Compose |

### Dependências Java

| Dependência | Função |
|---|---|
| Spring Web | Exposição dos endpoints REST |
| Spring Data JPA | Abstração do acesso ao banco de dados |
| Spring Validation | Validação dos DTOs de entrada |
| Spring Actuator | Health check da API |
| PostgreSQL Driver | Conexão com o banco |
| Lombok | Redução de boilerplate |

### Dependências Python

| Dependência | Função |
|---|---|
| httpx | Chamadas HTTP à API e ao Groq |


## Visão Geral da Arquitetura

```
Usuário (Postman)
      │
      ▼
┌─────────────┐
│  API Java   │  Spring Boot 3 + PostgreSQL
│  (porta 8080)│
└──────┬──────┘
       │
  ┌────┴─────────────────────┐
  │          Filas           │
  │                          │
  │  reports                 │  ← entrada de reportes
  │  reports-standard        │  ← prioridade baixa/média (1-6)
  │  reports-high            │  ← prioridade alta (7-10)
  └────┬──────────┬──────────┘
       │          │
┌──────┴───┐ ┌───┴──────────┐ ┌─────────────────┐
│classifier│ │   standard   │ │   high_alert     │
│ worker   │ │   worker     │ │   worker         │
│(x2)      │ │(x2)          │ │(x2)              │
└──────────┘ └──────────────┘ └─────────────────┘
```

O sistema é composto por:

- **API Java (Spring Boot):** coração do sistema. Recebe reportes, gerencia as filas no banco de dados e expõe endpoints REST para os workers.
- **PostgreSQL:** onde as filas vivem. Cada linha da tabela `tasks` é um reporte na fila.
- **Worker Classifier:** consome a fila `reports`, classifica o reporte com IA (Groq) e redireciona para `reports-standard` ou `reports-high`.
- **Worker Standard:** consome a fila `reports-standard` e finaliza reportes de prioridade normal (1-6).
- **Worker High Alert:** consome a fila `reports-high`, dispara um webhook de alerta e finaliza reportes de prioridade alta (7-10).

A comunicação entre todos os componentes é exclusivamente via HTTP REST. Nenhuma biblioteca de mensageria (RabbitMQ, Kafka, etc.) é utilizada.

### Sobre a escolha do Groq

Inicialmente o sistema foi desenvolvido utilizando o modelo LLaMA rodando localmente via Ollama. Porém, o tempo de inicialização do container do Ollama era muito elevado — chegando a vários minutos a cada rebuild — o que tornava o ciclo de desenvolvimento inviável. Por isso, migramos para a API do Groq, que utiliza o mesmo modelo LLaMA (llama-3.1-8b-instant) de forma remota e gratuita, eliminando o tempo de espera e mantendo a qualidade da classificação.


## Estrutura de Pastas

```
fila-classificacao-reportes/
├── python/
│   ├── classification_prompt.py   ← prompt e escala de prioridade para o Groq
│   └── worker/
│       ├── classifier_worker.py   ← worker de classificação com IA
│       ├── common_api.py          ← funções HTTP compartilhadas
│       ├── common_config.py       ← variáveis de ambiente compartilhadas
│       ├── high_alert_worker.py   ← worker de alta prioridade com webhook
│       ├── llm_classify.py        ← integração com a API do Groq
│       ├── standard_worker.py     ← worker de prioridade normal
│       ├── Dockerfile             ← imagem dos workers Python
│       └── requirements.txt       ← dependências Python
├── src/main/java/com/unisales/fila_reportes/
│   ├── controller/
│   │   └── TaskController.java    ← endpoints REST
│   ├── dto/
│   │   ├── TaskRequestDTO.java    ← contrato de entrada
│   │   └── TaskUpdateDTO.java     ← contrato de atualização
│   ├── exception/
│   │   └── TaskNotFoundException.java
│   ├── model/
│   │   └── Task.java              ← entidade da tabela tasks
│   ├── projection/
│   │   └── QueueStatsProjection.java ← projeção das estatísticas
│   ├── repository/
│   │   └── TaskRepository.java    ← queries JPA e nativas
│   ├── service/
│   │   └── TaskService.java       ← regras de negócio
│   └── ApiMensageriaApplication.java
├── .env                           ← variáveis de ambiente (não commitado)
├── docker-compose.yml             ← orquestração dos containers
├── Dockerfile                     ← imagem da API Java
├── init.sql                       ← criação da tabela tasks
└── pom.xml                        ← dependências Maven
```


## Modelo de Dados

```sql
CREATE TABLE tasks (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_name  VARCHAR(100) NOT NULL,    -- fila atual da tarefa
    payload     JSONB        NOT NULL,    -- dados do reporte (título, descrição, etc.)
    result      JSONB,                   -- resultado do processamento ou erro
    priority    INT,                     -- prioridade numérica 1-10 (null = não classificado)
    status      VARCHAR(20)  NOT NULL DEFAULT 'pending',
    attempts    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### Ciclo de vida de um reporte

| Status | Descrição |
|---|---|
| `pending` | Aguardando processamento na fila |
| `processing` | Reservado por um worker (FOR UPDATE SKIP LOCKED) |
| `done` | Processado com sucesso |
| `error` | Falhou após atingir o limite de tentativas |


## Como Subir o Sistema

### Pré-requisitos

- Docker e Docker Compose instalados
- Arquivo `.env` na raiz do projeto (veja seção de variáveis de ambiente)

### Subir tudo

```bash
docker compose up --build
```

### Derrubar e limpar volumes

```bash
docker compose down -v
```


## As Três Filas e o Fluxo de um Reporte

```
1. Usuário cria reporte via POST /tasks (queue_name: "reports")
        │
        ▼
2. Classifier Worker pega o reporte da fila "reports"
   - Valida campos obrigatórios (title, description, type, category, sector)
   - Chama a API do Groq para classificar a prioridade
   - Redireciona para a fila correta:
        │
        ├── priority 1-6 → reports-standard
        └── priority 7-10 → reports-high
        │
        ▼
3a. Standard Worker (reports-standard)
    - Valida se o reporte tem priority preenchida
    - Finaliza com status "done"

3b. High Alert Worker (reports-high)
    - Dispara webhook de alerta
    - Finaliza com status "done"
```

### Escala de Prioridade

| Priority | Label | Descrição | Fila destino |
|---|---|---|---|
| 1-3 | baixa | Rotinas, dúvidas internas, baixo impacto | reports-standard |
| 4-6 | média | Incômodo moderado, prazo normal | reports-standard |
| 7-10 | alta | Impacto forte a crítico em clientes ou negócio | reports-high |


## Como o Groq Classifica

O classifier envia o payload do reporte para a API do Groq com um prompt que instrui o modelo a retornar um JSON com `priority` (1-10) e `priorityLabel` (baixa, média ou alta). O sistema também aplica um piso de severidade — se a descrição contiver palavras como "produção em baixo", "todos os clientes sem acesso" ou "indisponível", a prioridade mínima é elevada para 7, independentemente do que o modelo retornar.


## Como Funciona o Retry

O mecanismo de retry é gerenciado pela coluna `attempts` da tabela `tasks`. A cada vez que um worker reserva uma tarefa, a API incrementa o `attempts`. Se o processamento falhar, o worker devolve a tarefa para `pending` e o ciclo se repete. Quando `attempts >= MAX_RETRY_ATTEMPTS`, a tarefa é marcada como `error`.

**Comportamento especial do Classifier:** quando o classifier classifica com sucesso e redireciona a tarefa para outra fila, a API decrementa o `attempts` de volta para zero — garantindo que o worker da próxima fase começa com o contador zerado e tem suas próprias tentativas independentes.



## Exclusão Mútua
 
A exclusão mútua entre workers da mesma fila é garantida pelo PostgreSQL através do mecanismo `FOR UPDATE SKIP LOCKED`, utilizado nas queries nativas do `TaskRepository`.
 
- **`FOR UPDATE`** — ao selecionar uma linha, o PostgreSQL coloca um lock exclusivo nela, impedindo que outra transação a leia ou modifique até o lock ser liberado.
- **`SKIP LOCKED`** — em vez de esperar o lock ser liberado (o que causaria fila de espera), outros workers simplesmente pulam a linha bloqueada e pegam a próxima disponível.
Sem esse mecanismo, dois workers poderiam ler a mesma tarefa quase simultaneamente — antes que qualquer um tivesse tempo de salvar o status `processing` — resultando em processamento duplicado. Com `FOR UPDATE SKIP LOCKED`, isso é impossível: a linha é bloqueada no exato momento da leitura, dentro de uma transação `@Transactional`, garantindo que apenas um worker processe cada reporte.


## Endpoints Disponíveis

### Criar reporte

```http
POST /tasks
json

{
    "queue_name": "reports",
    "payload": {
        "title": "Sistema indisponível",
        "description": "Todos os clientes sem acesso ao sistema",
        "type": "incidente",
        "category": "infraestrutura",
        "sector": "tecnologia"
    }
}
```

### Listar todos os reportes

```http
GET /tasks
```

### Buscar reporte por ID

```http
GET /tasks/{id}
```

### Estatísticas das filas

```http
GET /tasks/stats
```

### Atualizar status de um reporte

```http
PATCH /tasks/{id}
json

{
    "status": "done"
}
```

### Endpoints de dequeue (usados pelos workers)

```http
GET /tasks/next/reports/unclassified       ← usado pelo classifier
GET /tasks/next/reports-standard           ← usado pelo standard
GET /tasks/next/reports-high               ← usado pelo high_alert
```


## Como Testar os Cenários de Retry

### 1. Classifier — campos ausentes

**Objetivo:** demonstrar que o classifier rejeita reportes incompletos e aplica retry até virar `error`.

**Passo a passo:**

1. Crie um reporte sem o campo `sector`:

```json
POST /tasks
{
    "queue_name": "reports",
    "payload": {
        "title": "Dúvida sobre fatura",
        "description": "Cliente com dúvida sobre o valor da fatura do mês",
        "type": "suporte",
        "category": "financeiro"
    }
}
```

2. Copie o `id` retornado na resposta.
3. Aguarde alguns segundos e consulte o reporte:

```http
GET /tasks/{id}
```

4. Observe nos logs do Docker que o classifier detecta o campo ausente e volta a tarefa para `pending` a cada tentativa:

```
[classifier] tarefa {id} campos ausentes: ['sector'] | tentativa 1/3
[classifier] tarefa {id} campos ausentes: ['sector'] | tentativa 2/3
[classifier] tarefa {id} campos ausentes: ['sector'] | tentativa 3/3
[classifier] tarefa {id} marcada como error após 3 tentativas
```

5. Ao consultar o reporte, o status será `error`.


### 2. High Alert — webhook inválido

**Objetivo:** demonstrar que o high_alert faz retry quando o webhook falha.

**Passo a passo:**

1. No arquivo `.env`, configure uma URL inválida para o webhook:

```env
WEBHOOK_URL=https://url-invalida.com
```

2. Reinicie os containers:

```bash
docker compose down -v
docker compose up --build
```

3. Crie um reporte de alta prioridade:

```json
POST /tasks
{
    "queue_name": "reports",
    "payload": {
        "title": "Produção fora do ar",
        "description": "Todos os clientes sem acesso ao sistema, produção completamente indisponível",
        "type": "incidente",
        "category": "infraestrutura",
        "sector": "tecnologia"
    }
}
```

4. Copie o `id` retornado na resposta.
5. Observe nos logs do Docker que o high_alert tenta disparar o webhook e falha:

```
[high_alert] tarefa {id} capturada da fila 'reports-high' | priority=10 | attempts=1
[high_alert] tarefa {id} falha ao disparar webhook | tentativa 1/3
[high_alert] tarefa {id} capturada da fila 'reports-high' | priority=10 | attempts=2
[high_alert] tarefa {id} falha ao disparar webhook | tentativa 2/3
[high_alert] tarefa {id} capturada da fila 'reports-high' | priority=10 | attempts=3
[high_alert] tarefa {id} falha ao disparar webhook | tentativa 3/3
[high_alert] tarefa {id} marcada como error após 3 tentativas
```

6. Ao consultar o reporte, o status será `error`.


### 3. Standard — reporte sem priority

**Objetivo:** demonstrar que o standard rejeita reportes que chegaram sem classificação.

**Passo a passo:**

1. Crie um reporte diretamente na fila `reports-standard` sem passar pelo classifier:

```json
POST /tasks
{
    "queue_name": "reports-standard",
    "payload": {
        "title": "Teste sem prioridade",
        "description": "Reporte inserido diretamente na fila sem classificação",
        "type": "teste",
        "category": "teste",
        "sector": "teste"
    }
}
```

2. Copie o `id` retornado na resposta.
3. Observe nos logs do Docker que o standard detecta a ausência de priority:

```
[standard] tarefa {id} capturada da fila 'reports-standard' | priority=None | attempts=1
[standard] tarefa {id} falha: Tarefa sem priority na fila reports-standard | tentativa 1/3
[standard] tarefa {id} capturada da fila 'reports-standard' | priority=None | attempts=2
[standard] tarefa {id} falha: Tarefa sem priority na fila reports-standard | tentativa 2/3
[standard] tarefa {id} capturada da fila 'reports-standard' | priority=None | attempts=3
[standard] tarefa {id} falha: Tarefa sem priority na fila reports-standard | tentativa 3/3
[standard] tarefa {id} marcada como error após 3 tentativas
```

4. Ao consultar o reporte, o status será `error`.


## Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto com as seguintes variáveis:

```env
GROQ_API_KEY=sua_chave_aqui
WEBHOOK_URL=https://sua-url-de-webhook.com
POSTGRES_USER=seu_usuario
POSTGRES_PASSWORD=sua_senha
```

| Variável | Descrição | Obrigatório |
|---|---|---|
| `GROQ_API_KEY` | Chave da API do Groq para classificação com IA. Obtenha gratuitamente em https://console.groq.com | Sim |
| `WEBHOOK_URL` | URL para notificação de reportes de alta prioridade. Para testar, use https://pipedream.com para gerar uma URL temporária | Sim |
| `POSTGRES_USER` | Usuário do banco de dados PostgreSQL | Sim |
| `POSTGRES_PASSWORD` | Senha do banco de dados PostgreSQL | Sim |
