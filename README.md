# LeadProject

Spring Boot application for the AI Lead Agent MVP described in the product requirements document.

## Features

- Lead creation and assignment
- Excel import for leads
- Phone-based lead lookup
- Twilio outbound calling integration
- Plivo outbound calling integration
- Real-estate qualification call script
- Call transcript capture and sales brief generation
- Dashboard summary
- Campaign management
- Playbook configuration
- Compliance and suppression handling
- AI qualification endpoint
- Basic admin/security setup

## Swagger UI

```text
http://localhost:8080/swagger-ui.html
```

## API flow for the real-estate business use case

1. Upload leads: `POST /api/v1/leads/import`
2. Generate call plan: `POST /api/v1/leads/call-plan`
3. Start Twilio call: `POST /api/v1/leads/call` with `type` set to `iv` for the fixed questions or `aiagent` for the AI conversation (provider from `AI_PROVIDER`)
4. Save transcript: `POST /api/v1/calls/{callId}/transcript`
5. Qualify lead: `POST /api/v1/leads/{leadId}/qualify`
6. Create sales brief: `POST /api/v1/leads/{leadId}/sales-brief`
7. Assign to salesperson: `POST /api/v1/leads/{leadId}/assign`
8. Check dashboard: `GET /api/v1/dashboard`

## Excel template

Use the file `ui/excel-template.csv` or create an Excel file with these headers:

```csv
Name,Phone,Email,Source
Aisha Rahman,+971555123456,aisha@gmail.com,website_form
```

## Local run with ngrok (Twilio callbacks)

Java is not required on the host. Docker and ngrok are.

1. Copy `.env.example` to `.env` and set `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_PHONE_NUMBER`.
2. From the repo root run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-local-ngrok.ps1
```

Add `-WithOllama` only if you need `type: aiagent` calls.

The script starts ngrok on port 8080, then Postgres and the app, and sets `TWILIO_APP_BASE_URL` to the public HTTPS URL. Twilio voice webhooks are unauthenticated at `/api/v1/voice/**`.

If the ngrok URL changes, run the script again so the app is recreated with the new callback base URL.

## Voice provider configuration

Set the following environment variables for the provider you want to use.

### Twilio

```bash
TWILIO_ACCOUNT_SID=ACxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxx
TWILIO_PHONE_NUMBER=+971500000000
APP_PUBLIC_BASE_URL=https://your-subdomain.ngrok-free.dev
TWILIO_APP_BASE_URL=https://your-subdomain.ngrok-free.dev
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2:3b
```

The AI voice mode uses Twilio speech gathering and the configured LLM (`AI_PROVIDER`) one turn at a time. Example request:

```json
{
	"phone": "+971500000000",
	"leadName": "Aisha Rahman",
	"leadId": 42,
	"type": "aiagent"
}
```

Use `type: "iv"` or omit the property to keep the existing predefined-question flow.

For local Ollama chat testing, call `POST /api/v1/ai/chat` with Basic Auth:

```json
{
	"message": "What information should I collect from a Dubai property buyer?",
	"conversation": ""
}
```

The endpoint returns `{ "reply": "..." }` and does not start a Twilio call.

### Docker Compose AI setup

```bash
docker compose up -d --build
```

The Ollama container automatically pulls `${OLLAMA_MODEL:-llama3.2:3b}` on startup and stores it in the `ollama_data` volume. When `AI_PROVIDER=ollama`, start Ollama with `docker compose up -d ollama` (the app no longer hard-depends on it). Inside Compose use `OLLAMA_BASE_URL=http://ollama:11434`; on the host use `http://localhost:11434`.

### Render Ollama setup

For the separate Render Ollama service, set:

```text
OLLAMA_MODEL=llama3.2:3b
OLLAMA_HOST=0.0.0.0:$PORT
```

Use this start command so the model is installed before the service accepts requests:

```bash
sh -c 'ollama serve & pid=$!; until ollama list >/dev/null 2>&1; do sleep 2; done; ollama pull ${OLLAMA_MODEL:-llama3.2:3b}; wait $pid'
```

Attach a persistent disk at `/root/.ollama`; otherwise the model is lost after a Render restart or redeploy.

### Plivo

```bash
PLIVO_AUTH_ID=xxxxxxxx
PLIVO_AUTH_TOKEN=xxxxxxxx
PLIVO_PHONE_NUMBER=+971500000000
PLIVO_APP_BASE_URL=http://localhost:8080
```


## Switch public / ngrok URL (no Java changes)

Set either variable (they alias each other):

```bash
APP_PUBLIC_BASE_URL=https://your-subdomain.ngrok-free.dev
# or
TWILIO_APP_BASE_URL=https://your-subdomain.ngrok-free.dev
```

Or re-run `scripts/run-local-ngrok.ps1`, which sets both from the live ngrok HTTPS URL and recreates the app container.

On a server, set `APP_PUBLIC_BASE_URL=https://your.domain.com` (no localhost in code).

## Switch LLM provider (config / env only)

`app.ai.provider` / `AI_PROVIDER`: `ollama` | `openai` | `openrouter` | `anthropic`

### Ollama (default)

```bash
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434   # or http://ollama:11434 in Compose
OLLAMA_MODEL=llama3.2:3b
OLLAMA_NUM_PREDICT=40
OLLAMA_KEEP_ALIVE=30m
```

### OpenAI

```bash
AI_PROVIDER=openai
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
# optional: OPENAI_BASE_URL=https://api.openai.com
```

### OpenRouter

```bash
AI_PROVIDER=openrouter
OPENROUTER_API_KEY=...
OPENROUTER_MODEL=openai/gpt-4o-mini
# optional: OPENROUTER_BASE_URL=https://openrouter.ai/api
```

### Claude (Anthropic)

```bash
AI_PROVIDER=anthropic
ANTHROPIC_API_KEY=...
ANTHROPIC_MODEL=claude-3-5-sonnet-20241022
```

Shared knobs: `AI_TEMPERATURE`, `AI_MAX_TOKENS`, `AI_CONNECT_TIMEOUT_MS`, `AI_READ_TIMEOUT_MS`.
Voice greetings / IVR / think budget: `VOICE_COMPANY_NAME`, `VOICE_GREETING_TEMPLATE`, `VOICE_IV_Q1`–`VOICE_IV_Q6`, `VOICE_THINK_BUDGET_MS` (see `.env.example` and `application.yml` under `app.voice`).
CORS: `FRONTEND_ORIGIN`, `CORS_ALLOWED_ORIGINS`. Plivo REST host: `PLIVO_API_BASE_URL` (default `https://api.plivo.com`).

## Run locally

```bash
mvn spring-boot:run
```

For Swagger local testing, open `http://localhost:8080/swagger-ui.html` and execute against the generated `http://localhost:8080` server. When Swagger is opened through the HTTPS ngrok URL, Spring uses forwarded headers to advertise the ngrok server URL. Direct ngrok clients must send `ngrok-skip-browser-warning: true`.

## Default users

Passwords are **env-only** (never commit real secrets into `application.yml`). Set them in `.env` (see `.env.example`):

```bash
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=...
APP_SALES_USERNAME=sales
APP_SALES_PASSWORD=...
APP_COMPLIANCE_USERNAME=compliance
APP_COMPLIANCE_PASSWORD=...
```

Docker Compose defaults these to `admin123` / `sales123` / `compliance123` for local demos only — override in `.env` for anything shared.

## Configuration layout

Configurable values live in `src/main/resources/application.yml` (with env overrides):

| Prefix | Purpose |
|--------|---------|
| `app.public-base-url` / `APP_PUBLIC_BASE_URL` | Twilio/Plivo webhook base URL |
| `app.frontend-origin` / `FRONTEND_ORIGIN` | SPA origin for CORS |
| `app.cors.*` / `CORS_ALLOWED_ORIGINS` | CORS origins, patterns, methods, headers |
| `app.ai.*` / `AI_*`, `OLLAMA_*`, `OPENAI_*`, … | LLM provider, models, base URLs, prompts |
| `app.voice.*` / `VOICE_*` | Greetings, IVR Q1–Q6, opt-out keywords/phrases, think budget |
| `security.users` / `APP_*_PASSWORD` | Basic Auth users (passwords from env) |
| `voice.provider` / `VOICE_PROVIDER` | `twilio` or `plivo` |
| `twilio.*` / `TWILIO_*` | Twilio credentials and recording |
| `plivo.*` / `PLIVO_*` | Plivo credentials, `PLIVO_API_BASE_URL`, ring timeout |

IVR question strings (Q1–Q6) are `app.voice.iv-questions` — override with `VOICE_IV_Q1` … `VOICE_IV_Q6`.

## Docker

```bash
mvn clean package -DskipTests
docker-compose up --build
```
