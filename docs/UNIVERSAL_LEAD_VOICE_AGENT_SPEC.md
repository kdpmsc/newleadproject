# Universal Lead Voice Agent: Product and Implementation Specification

## 1. Purpose

Turn LeadProject from a real-estate-specific calling MVP into a configurable, multi-tenant product that lets organizations in different industries create, test, publish, and run outbound lead conversations. Each organization should define what it sells, what it needs to learn, how the agent should sound, which statements it may make, when it must transfer or end a call, and how qualified outcomes reach its team.

The agent is not a fixed questionnaire and must not act as an unconstrained salesperson. It should carry a natural conversation, remember what the lead has already said, collect only useful missing information, clarify uncertainty, and operate only within approved facts and policies.

## 2. Current Baseline and Gaps

The current implementation already provides useful foundations:

- `VoiceController` has Twilio webhook endpoints and an AI voice turn loop.
- `AiVoiceAgentService` invokes the configured LLM provider.
- `LeadCallService` persists a text transcript on `LeadCall`.
- `Playbook` and `PlaybookVersion` provide an initial configuration/version concept.
- `Campaign` can refer to a playbook version.
- Twilio and Plivo are represented behind `VoiceCallService`.

The product is not yet domain-neutral or real-time:

- The voice prompt and greeting defaults are UAE real-estate-specific in `application.yml`.
- The voice agent does not load a campaign's `PlaybookVersion`; it uses global `AiProperties` prompts.
- Playbook configuration is unvalidated arbitrary JSON, with no editor, preview, publish workflow, or runtime contract.
- A call's conversation is a single text field. Turns, extracted facts, confidence, and outcome are not first-class records.
- The AI receives flattened transcript text rather than role-separated messages or a compact state summary.
- Twilio `<Gather>` waits for a completed utterance, then the server waits for a complete LLM response. This is turn-based voice, not streaming speech or interruptible real-time dialogue.
- Organization ownership is absent. Users are configured in memory, campaign `ownerId` is hard-coded, and provider configuration is global.
- Call policy, consent, jurisdiction, retention, and per-tenant suppression need explicit product behavior.

## 3. Product Goals

1. Let a non-developer configure a lead-calling agent for a supported industry/domain without changing code or environment variables.
2. Make active campaigns use an immutable, approved agent version.
3. Make the agent acknowledge and use known information, ask one relevant question at a time, clarify uncertain answers, handle natural corrections, and stop or escalate appropriately.
4. Support a reliable turn-based voice mode as the first delivery milestone and a genuinely streaming, interruptible mode as a separate milestone.
5. Keep lead data, credentials, call records, and configuration isolated between customer organizations.
6. Make call behavior auditable, testable, measurable, and safe to change through versioned configuration.
7. Provide explicit provider adapters so a voice carrier, model, speech service, or domain profile can be changed without rewriting conversation policy.

## 4. Non-Goals for the First Release

- Autonomous pricing, quoting, booking, eligibility decisions, or commitments not backed by approved data and an explicit tool.
- Replacing a CRM, contact-center platform, or full sales engagement suite.
- Supporting every carrier and every language at launch. Define extension points; launch with a deliberately tested provider/language matrix.
- Fine-tuning a foundation model as a prerequisite. Begin with controlled prompts, structured state, retrieval from approved tenant content, and evaluations.
- Letting tenant-authored prompts bypass platform safety, consent, privacy, or call termination rules.

## 5. Product Roles and Primary Workflows

### Roles

- **Organization owner:** manages organization settings, members, provider connections, retention, and billing-level controls.
- **Agent designer:** creates agent profiles, qualification fields, knowledge, conversation policies, test scenarios, and drafts.
- **Campaign manager:** imports or selects leads, chooses a published agent version, configures calling rules, previews, and launches campaigns.
- **Compliance reviewer:** approves policies and versions, reviews disclosure/consent rules, suppression, audit history, and call samples.
- **Sales user:** receives outcomes, summaries, callbacks, and qualified leads; may request human transfer.
- **Platform operator:** manages supported providers, operational limits, platform-wide safety controls, and incidents without accessing tenant content except through audited support access.

### Main customer workflow

1. Create an organization and invite users with roles.
2. Create an agent from a generic starter template or an industry template.
3. Configure organization identity, offer, audience, goals, facts, qualification fields, call behavior, language/voice, compliance rules, and handoff destination.
4. Test in a text simulator and run voice test calls with synthetic/test leads.
5. Resolve validation errors and review a generated preview of the greeting, sample scenarios, and expected data captured.
6. Submit the version for approval; an authorized reviewer publishes it.
7. Create a campaign, select a published immutable version, import/choose leads, set schedule and retry controls, preview recipients, and launch.
8. Review live campaign status and call outcomes; inspect transcripts and structured facts subject to role permissions.
9. Create a new draft version for changes. Existing campaigns stay pinned to their selected version unless explicitly migrated.

## 6. Configuration Contract: Agent Profile

An agent version is structured configuration, not a free-form prompt. Validate it on save and publish, apply platform-owned system rules at runtime, and keep a read-only snapshot on each campaign.

### Required profile sections

- **Identity:** display name, company/brand name, industry label, product/service category, locale, timezone, and supported languages.
- **Purpose:** campaign objective, target audience, what a successful call means, permitted call outcomes, and whether the purpose is qualification, appointment setting, renewal, research, or another configured workflow.
- **Offer facts:** approved descriptions, eligibility limits, service areas, prices/ranges if approved, availability sources, disclaimers, and explicit unknowns. Each fact has a source, status, and optional expiry.
- **Lead fields:** configurable typed fields with stable keys, human labels, descriptions, required/optional flags, validation, sensitivity classification, and allowed values where applicable. Examples: intent, need, location, budget, timing, current provider, property type, policy type, preferred appointment time.
- **Conversation policy:** tone, formality, response length, one-question-at-a-time preference, allowed clarifications, objection handling, re-engagement, silence handling, interruption behavior, fallback behavior, and closing rules.
- **Question strategy:** goals and conditional dependencies, not a rigid script. Define why each field matters, acceptable evidence, priority, skip rules, and whether a field may be inferred or must be explicitly confirmed.
- **Knowledge:** approved FAQ/knowledge entries, structured product/service records, retrieval scope, source owner, review date, and behavior when no supported answer is found.
- **Call handling:** greeting, identity/disclosure statements, callback/transfer targets, voicemail behavior, maximum call duration, end-call rules, and no-answer behavior.
- **Voice and recognition:** provider voice ID, speaking style where supported, language/locale, pronunciation dictionary, domain vocabulary hints, speech timeout/endpointing configuration, and fallback locale.
- **Compliance policy:** permitted jurisdictions, local calling windows, consent requirements, disclosure text, recording policy and consent flow, opt-out phrases, prohibited claims, suppression scope, retention, and escalation conditions.
- **Evaluation suite:** required sample conversations and rubric expectations that must pass before publish.

### Configuration principles

- Offer generic primitives; industry templates are starting values, not hard-coded runtime branches.
- Use typed field definitions and JSON Schema (or equivalent validation) for configuration and model output.
- Separate stable field keys from displayed wording so campaigns and reports survive copy edits.
- Keep prompts generated from the profile and platform policy; do not concatenate untrusted fields without clear boundaries.
- Treat tenant documents and caller speech as untrusted input. They cannot change system rules or tool permissions.
- Allow tenant-defined fields and outcomes, but require explicit type, validation, privacy classification, and retention behavior.

## 7. Conversation Runtime Requirements

### State model

Maintain a structured `ConversationState` per call with:

- profile/version and campaign IDs;
- current stage and call status;
- confirmed facts, tentative facts, rejected/corrected facts, and unanswered required fields;
- for each fact: value, type, confidence, provenance (lead turn, CRM, approved knowledge, or explicit tool), timestamp, and confirmation state;
- last agent action, last caller turn, current question goal, retry counts, consent/opt-out status, and human handoff status;
- a concise rolling summary plus a bounded window of recent role-separated turns.

Never treat an inferred or low-confidence value as confirmed. Preserve user corrections as the current value and record the correction event. Existing `LeadCall.conversation` can be retained temporarily for backward compatibility, but new runtime logic must use persisted turn/state records.

### Per-turn control flow

For every caller turn:

1. Validate provider webhook signature and correlate the provider event idempotently to the call.
2. Normalize the transcript and locale metadata without discarding the raw transcript required for audit.
3. Apply deterministic policy checks first: opt-out, stop, wrong number, emergency/safety cue, explicit human request, call-window violations, and terminal call status.
4. Update state with transcript and candidate fact observations. Do not ask the model to decide whether a hard opt-out should be honored.
5. Construct a model input from platform policy, pinned agent-version data, authorized knowledge, compact state, and recent role-separated turns.
6. Ask the model for a strictly structured response containing spoken text, fact updates, next action, rationale code, handoff/end-call decision, and flags. Do not accept executable instructions or arbitrary URLs from the model.
7. Validate schema, allowed actions, field keys/types, response length, policy constraints, and claims against approved knowledge. Retry once with a constrained repair instruction or use a safe fallback if invalid.
8. Persist the model decision and resulting state before dispatching speech, using an idempotency key to avoid duplicate turns on webhook retries.
9. Deliver speech, collect the next caller turn, and record provider timing/status events.
10. On terminal outcome, persist disposition, structured fields, summary, next action, and campaign counters exactly once.

### Conversational behavior

The agent should:

- Respond to the caller's actual point before moving qualification forward.
- Confirm understood intent and facts naturally, without repeating them as a checklist.
- Ask one short, useful question at a time; prioritize fields needed to determine fit or next action.
- Skip questions already answered with sufficient confidence. Ask confirmation only when ambiguity or policy requires it.
- Ask open questions where natural, then offer concise choices when the caller needs help answering.
- Clarify uncertain entities, amounts, dates, names, locations, or technical terms. Repeat the candidate interpretation and ask a yes/no correction question; never silently normalize uncertain speech.
- Handle corrections, digressions, interruptions, objections, requests to repeat, topic changes, and requests for a person without losing the current goal.
- Answer only from approved organization facts or explicitly authorized tools. Say it does not know and offer an allowed next step when unsupported.
- Avoid promises, invented inventory/prices/coverage/eligibility, sensitive inferences, pressure tactics, and false claims of human identity.
- Respect silence/no-input, DTMF if enabled, opt-out, callback requests, wrong numbers, and human handoff.
- End cleanly with a concise summary/next step when complete, or gracefully close when the caller declines.

### Example target behavior for the observed exchange

Given the transcript:

> Caller: I want to buy an investment property. I'm looking for a villa. I'm looking for an "argon". Do you have another area that could be beneficial?

Expected response should preserve confirmed facts and repair the uncertain location before asking another qualification question, for example:

> “An investment villa, got it. Did you mean Arjan? I can note that as your preferred area. Are you open to comparing it with other areas if they fit your budget and investment goals?”

The exact wording is generated, but the behavior is tested: intent and property type remain captured; uncertain place is not committed; answer acknowledges the question and asks a relevant clarification rather than repeating “which area?”.

## 8. Voice Modes and Provider Strategy

### Mode A: Reliable turn-based voice (initial release)

Keep Twilio `<Gather>` and current `VoiceCallService` path initially, but improve behavior and observability:

- Bind calls to campaign-pinned agent version and tenant voice configuration.
- Add configurable recognition locale, vocabulary hints where supported, endpointing/silence behavior, and repeat/clarify flow.
- Increase and tune model output budget based on evaluation, rather than keeping the current 40-token default for all providers.
- Use provider-specific timeout budgets, warmup, model latency tracking, and user-friendly fallback behavior.
- Persist discrete turns and structured state. Provide fast webhook acknowledgement and avoid duplicate model work for retries.
- Establish latency targets that the chosen model and telephony provider can meet; do not label this mode real-time.

### Mode B: Streaming, interruptible voice (real-time milestone)

Add a transport capability interface separate from conversation policy. Implement a Twilio ConversationRelay adapter first if its supported features, pricing, regional availability, and account eligibility fit product requirements. Evaluate Media Streams plus selected streaming STT/TTS/model providers as an alternative when lower-level control is required.

For the selected path:

- Maintain an authenticated WebSocket session tied to a provider call, tenant, campaign, and immutable agent version.
- Handle provider setup, partial/final recognition, caller interruption/barge-in, speech-start/end, DTMF, disconnect, and provider error events.
- Stream model output in short speakable clauses when supported; do not wait for a complete paragraph before starting audio.
- Cancel or truncate current speech/model generation on caller interruption, then incorporate the caller's new turn.
- Separate speech recognition and synthesis configuration from the LLM provider; keep voice IDs and locale capability validation provider-specific.
- Apply backpressure, bounded queues, heartbeat/timeouts, reconnect semantics, concurrency limits, per-call cancellation, and idempotent event handling.
- Preserve a text/turn-based fallback when the streaming provider or WebSocket connection fails.
- Measure end-of-speech-to-first-audio, interruption-stop time, dropped events, reconnects, and call completion outcomes.

Do not build a custom audio pipeline before testing the hosted ConversationRelay path against a small, representative quality/latency test set. Make provider selection an explicit product capability, not an assumption that all transports support the same features.

## 9. Target Architecture

Keep Java/Spring Boot and PostgreSQL initially. Split responsibilities into explicit application services and provider interfaces:

- `OrganizationService` / `MembershipService`: tenant identity, role membership, tenant-scoped authorization.
- `AgentProfileService` / `AgentVersionService`: schema validation, draft/publish lifecycle, immutable version snapshots.
- `CampaignService`: version pinning, call-window policy, lead selection, campaign lifecycle, rate limits, retry rules.
- `ConversationOrchestrator`: turn state machine, deterministic policies, model/tool decision validation, outcomes.
- `ConversationModel`: non-streaming and streaming generation capability; provider-specific client implementations.
- `VoiceTransport`: call creation, webhook/stream events, speech delivery, transfer, hangup; implementations for Twilio and later Plivo/other providers.
- `KnowledgeService`: tenant-scoped approved facts/retrieval with source attribution and freshness.
- `CallPolicyService`: consent, local time windows, suppression, disclosure, recording, retention, retry enforcement.
- `CallEventService`: append-only normalized provider and conversation events with idempotency.
- `EvaluationService`: run fixed scenarios against a draft version and store versioned results.
- `CredentialService`: encrypted provider secrets, key rotation, redaction, and access auditing.

Do not make one service dynamically interpret arbitrary JSON throughout the code. Deserialize validated version JSON into typed domain objects at publish/runtime boundaries. Keep provider SDK types out of domain state and API DTOs.

## 10. Data Model and Migration Plan

Introduce tenant ownership and first-class conversations incrementally. Suggested entities/tables:

- `organizations`: name, slug, default locale/timezone, status, retention policy, created/updated timestamps.
- `users` and `organization_memberships`: external/local identity, organization ID, role, status. Replace in-memory-only identities for product deployment.
- `agent_profiles`: organization ID, name, domain/industry label, status, current published version ID.
- `agent_versions`: profile ID, version number, schema version, validated configuration JSON, lifecycle state, created-by/approved-by/published-at, checksum.
- `knowledge_sources` / `knowledge_entries`: organization ID, source, content/type, approval/freshness, metadata, tenant-safe retrieval index.
- `campaigns`: organization ID, pinned agent version ID, status, timezone, allowed days/hours, retry rules, concurrency/rate limits, owner membership ID.
- `campaign_leads`: campaign ID, lead ID, state, attempts, next-attempt time, exclusion reason, idempotency key.
- `call_sessions`: organization/campaign/lead/profile-version IDs, provider, provider call ID, status, outcome/disposition, consent, timestamps, durations, redacted summary.
- `call_turns`: call ID, sequence, role, text, event type, language, source/provider event ID, timestamps, latency, redaction state.
- `conversation_states`: call ID, JSONB typed state, current stage, rolling summary, state version, updated timestamp.
- `fact_observations`: call ID, stable field key, value JSONB, confidence, confirmation state, provenance/turn ID, correction/supersession link.
- `call_events`: call ID, event key, normalized event payload, received/processed timestamps, provider metadata; unique idempotency constraint.
- `provider_connections`: organization ID, provider/account reference, encrypted credentials or secret-manager reference, caller IDs, capabilities, status.
- `suppression_entries`: organization ID, normalized phone, scope/reason/source, created/expiry timestamps; enforce lookup before every outbound attempt.
- `audit_events`: organization ID, actor, action, resource/version, timestamp, safe metadata; never store credentials or full unredacted secrets.

Migration constraints:

- Add an initial organization for existing records and associate current records with it in a database migration.
- Add nullable tenant/version foreign keys, backfill, verify counts, then enforce non-null constraints.
- Preserve existing `Playbook`/`PlaybookVersion` records by mapping them to agent profiles/versions or treating them as the initial implementation of those concepts; do not discard arbitrary stored configuration.
- Backfill existing `LeadCall.conversation` into ordered `call_turns` where labels can be parsed. Preserve original transcript if parsing is uncertain.
- Keep legacy `transcript` and conversation fields read-compatible during a defined deprecation window.
- Pin current campaigns to their resolved published playbook version before changing runtime behavior.
- Add indexes for `(organization_id, created_at)`, campaign status, normalized phone suppression lookup, provider call IDs, and turn ordering.
- Use Flyway or the repository's chosen migration tool for every schema change; avoid relying on Hibernate `ddl-auto=update` in production.

## 11. API and UI Scope

### API (versioned under `/api/v1` initially)

- Organizations: create/read/update organization and manage memberships/roles.
- Agent profiles: create, list, retrieve, update draft, clone template, archive.
- Agent versions: validate, simulate, evaluate, submit for approval, approve/reject, publish, list history, compare versions.
- Knowledge: upload/import, add/edit entries, review/approve, test retrieval, view source attribution.
- Campaigns: draft, preview leads/exclusions, validate policy, launch/pause/resume/cancel, inspect attempts, pin/migrate version with explicit confirmation.
- Calls: retrieve session, turns, structured fact state, outcome, consent, audit-safe provider status; apply role-based redaction.
- Provider connections: configure/test/rotate credentials, caller IDs, and capability status without returning stored secret values.
- Voice webhooks/streams: provider-specific externally reachable endpoints with signature validation and idempotency.
- Evaluations: create/run test scenarios, retrieve per-scenario results and aggregate publish gates.

Every organization-scoped endpoint derives tenant from authenticated membership, not a client-supplied organization ID alone. Return 404/403 consistently without disclosing another tenant's resource existence.

### UI

- **Workspace setup:** organization, team, locale/timezone, provider setup and test.
- **Agent builder:** guided sections for purpose, offer/facts, fields, dialogue policy, knowledge, compliance, voice, and handoff; inline validation and autosaved draft.
- **Conversation simulator:** caller and agent turns, displayed extracted facts/confidence, next-field objective, unsupported-answer markers; resettable deterministic scenarios.
- **Voice preview:** controlled test call with explicit test-only calling safeguards; transcript, latency timeline, and interruption events for real-time mode.
- **Version review:** diff of configuration, evaluation results, reviewer, approval, publish, rollback/clone actions.
- **Campaign builder:** selected immutable version, recipient preview/exclusion reasons, schedule/timezone, retry/concurrency, test call, launch confirmation.
- **Call outcome workspace:** transcript by turn, structured field evidence, outcome, callback/handoff, consent/recording status, redacted data controls.
- **Admin controls:** member roles, provider connections, suppression, retention, audit logs.

Keep advanced provider and policy controls in expert settings, but show blocking validation issues before publishing or launching. No invisible fallbacks for credential, calling-window, consent, or version selection errors.

## 12. Security, Privacy, and Compliance Requirements

- Enforce tenant scoping at service/repository boundaries, not only in UI filtering.
- Replace shared default credentials and production in-memory accounts with persisted identity integration; support role checks for organization owner, designer, manager, reviewer, and sales user.
- Validate Twilio/other provider signatures on callbacks; authenticate WebSocket upgrades using provider-supported validation and a short-lived call/session binding.
- Encrypt provider credentials using a managed secret store or envelope encryption. Never return secret material from APIs or log it.
- Redact tokens, credentials, payment data, sensitive personal data, and configured fields from logs and error messages.
- Require configurable retention and deletion for transcripts, audio recordings, derived summaries, and fact data. Delete from indexes/backups according to documented policy.
- Store consent/disclosure evidence and enforce locale/jurisdiction calling rules before dialing. A configured policy must fail closed if required values are missing.
- Apply opt-outs immediately and idempotently across the configured suppression scope, including concurrent campaigns.
- Limit outbound rate/concurrency, retries, per-call duration, spend, and model tokens. Require operator kill switch and campaign pause.
- Keep audit records for profile/version changes, approvals, provider credential changes, call initiation, consent, opt-out, export, deletion, and support access.
- Treat call audio/transcript and uploaded knowledge as personal/customer data. Define data residency and subprocessors before commercial rollout.
- Provide a clear disclosure and recording consent experience; exact wording must be configurable per jurisdiction and reviewed by customer/legal counsel.

This plan specifies technical controls; it is not legal advice. Product launch in a jurisdiction requires jurisdiction-specific compliance review.

## 13. Observability and Operations

Record per-call correlation IDs across campaign, lead, call, provider request, model request, and turn. Capture timings for provider setup, recognition finalization, orchestration, model first-token/complete, synthesis first-audio, caller interruption, and webhook processing.

Dashboards and alerts should cover:

- call attempts, connection rate, completion, opt-out, transfer, callback, qualification, and failure outcomes;
- webhook/stream signature failures, duplicate/retried events, queue saturation, disconnects, and provider error rates;
- model latency, timeout, invalid structured output, unsupported claims, fallback rate, tokens/cost;
- end-of-utterance-to-first-audio p50/p95, interruption-stop latency, and turn abandonment;
- campaign calling-window violations, suppression conflicts, retry-limit violations, and unexpected concurrency;
- per-tenant provider/LLM spend and rate limits without exposing other tenants' data.

Do not log full audio or transcripts by default. Where transcript logging is needed for support/evaluation, enforce access control, redaction, retention, and explicit audit.

## 14. Quality and Evaluation Plan

Build a versioned golden test set before changing prompts. Each scenario includes caller utterances, profile facts, expected state transitions/field values, acceptable response behaviors, disallowed claims, and expected disposition. Include:

- clear answer, multi-field answer, and facts volunteered out of order;
- correction after an incorrect transcription or changed preference;
- uncertain proper noun, amount, date, or technical term;
- request for product/service information outside approved facts;
- objection, interruption, silence, repeated question, topic change, and caller frustration;
- explicit opt-out, wrong person/number, callback request, human request, and voicemail;
- accents, code-switching, noisy audio, short utterances, and ASR substitution;
- prompt injection attempts in caller speech and malicious/unapproved knowledge content;
- transient model/provider failure, slow response, duplicate webhook, reconnect, and call hangup during generation.

Automated evaluation should grade state correctness and policy adherence deterministically where possible, and conversational quality with reviewed rubrics. LLM-as-judge may assist triage but cannot be the sole release gate for compliance. Human reviewers should blind-score a sample of calls for responsiveness, listening, clarity, trust, and correct next action.

## 15. Phased Implementation Plan

### Phase 0: Baseline and product decisions

- Capture current Twilio, Plivo, model, database, and call flows; add reproducible tests for current AI webhook behavior.
- Decide initial supported customer identity/auth approach, first provider, first locales, provider credential model (platform-managed, BYOK, or both), recording default, and initial deployment jurisdictions.
- Define a generic demo profile plus two contrasting profiles (for example, real estate and insurance) to prevent accidental domain assumptions.
- Create golden conversation tests including the observed Arjan clarification case.
- Establish current latency/cost/turn-completion baseline and explicitly separate turn-based vs streaming acceptance.

**Exit gate:** documented decisions; baseline tests; no secret values in logs/test fixtures; agreed launch jurisdictions/provider matrix.

### Phase 1: Domain-neutral profile and natural turn-based agent

- Evolve Playbook/PlaybookVersion into typed, schema-versioned agent configuration while preserving stored JSON.
- Add organization ownership and tenant scoping to profiles, campaigns, calls, leads, and suppressions.
- Replace global voice prompt use with version-resolved `ConversationContext` assembled from campaign-pinned profile, policy, approved knowledge, and conversation state.
- Add typed field definitions and structured model output validation; keep a safe, deterministic response on malformed model output.
- Add ordered call turns and state/fact observations with confidence/provenance; maintain legacy transcript compatibility.
- Add dialogue behavior for acknowledgement, avoid-repeat, clarification, correction, answer-then-ask, opt-out, human request, and safe unknown answer.
- Make locale, speech voice, vocabulary hints, turn timeout, token budget, and response length profile/provider settings with validated limits.
- Add simulation endpoint and generic profile builder sufficient to draft, validate, and test a profile.
- Add database migrations and tests for tenant isolation, version pinning, idempotency, call history, and backward compatibility.

**Exit gate:** both contrasting profiles pass golden scenarios; a published version cannot be mutated; existing real-estate flow remains usable; every call is tenant-scoped; no hard-coded real-estate prompt is used unless selected by a profile.

### Phase 2: Product lifecycle and safe campaign operation

- Implement draft/review/approved/published/archived version states, diffs, reviewer audit, rollback by publishing a prior configuration as a new version.
- Add approved knowledge management, freshness/source attribution, retrieval tests, and unknown-answer behavior.
- Add call schedule by timezone, consent/disclosure, suppression, retries, concurrency/rate/spend controls, preview, pause/resume, and kill switch.
- Move authentication to organization memberships with role-based authorization; encrypt provider credentials or integrate with secret manager.
- Add campaign and call outcome dashboards, failure taxonomy, transcript access/redaction, retention/deletion workflows.
- Add a UI path to configure, simulate, publish, test-call, and launch without editing `.env` or JSON by hand.

**Exit gate:** a tenant admin can onboard and operate a sandbox campaign without developer intervention; prohibited recipients cannot be dialed; cross-tenant API tests pass; audited rollback works.

### Phase 3: Real-time voice proof of capability

- Spike ConversationRelay and Media Streams using a small call set with realistic accents/noise, interruptions, endpointing, language, and domain vocabulary.
- Compare time-to-first-response, interruption handling, reliability, operational complexity, provider constraints, and per-minute cost.
- Implement the winning transport behind `VoiceTransport` and add provider capability discovery/config validation.
- Add WebSocket session authentication, event normalization, streaming response delivery, cancellation/barge-in, backpressure, reconnect, and turn persistence.
- Retain the turn-based path as automatic/manual fallback. Make campaign mode explicit and visible.
- Add stress, race/idempotency, disconnect, provider timeout, and security tests.

**Exit gate:** meet real-time targets in representative test calls, including barge-in and recovery, with no lost/doubled turns; fallback remains usable; no security/compliance regression.

### Phase 4: Controlled beta and expansion

- Run internal calls with synthetic/test leads, then opt-in customer beta with limits and human monitoring.
- Review human-rated call quality, qualified lead accuracy, cost, latency, complaints, opt-outs, and provider failures weekly.
- Add languages, carrier adapters, CRM integrations, analytics, and industry templates based on evaluated demand.
- Add staged rollout, per-tenant feature flags, canary model/profile releases, and regression evaluations for every runtime change.

**Exit gate:** agreed customer-specific success thresholds sustained over a representative period, operational runbooks complete, support and incident response staffed.

## 16. Acceptance Criteria and Initial SLO Targets

Targets below are initial engineering targets and must be validated against selected voice/model providers before commitment:

- **Configuration:** 100% of published versions pass schema, safety, required-field, and evaluation gates; active campaigns remain pinned to their version.
- **Conversation state:** all caller turns and accepted fact changes are ordered, attributable, idempotent, and recoverable after process restart.
- **Listening:** in golden tests the agent does not re-ask a confirmed field unless it explicitly needs confirmation; ambiguous critical entities trigger clarification, not silent acceptance.
- **Relevance:** the response acknowledges the caller's last message and answers a direct question before advancing qualification, unless safety/compliance handling takes priority.
- **Claims:** every product-specific factual answer is supported by approved profile/knowledge/tool output; unsupported questions produce a safe unknown response.
- **Opt-out:** a clear opt-out stops the active call promptly and creates a suppression event before any subsequent attempt can be scheduled.
- **Turn-based latency:** establish provider-specific p50/p95 baselines; initial target p95 end-of-speech-to-first-audio under 5 seconds for configured supported models, excluding carrier/network delays outside measurement boundary.
- **Real-time latency:** target p95 end-of-speech-to-first-audio under 1.5 seconds and caller interruption-to-agent-audio-stop under 500 ms in controlled supported-provider tests; revise if provider capability evidence requires it.
- **Reliability:** no duplicate lead outcome, turn, or outbound attempt under retried callbacks; recover safely from worker/app restart.
- **Isolation:** automated tests prove a member cannot read or mutate another tenant's profile, leads, campaign, provider connection, transcript, or call recording.
- **Usability:** a trained non-developer can create a draft, run the simulator, correct validation failures, publish, and launch a test campaign using only the UI.

Latency measurement must define the event boundaries and exclude/report provider-side network delay separately; do not claim targets based on model API time alone.

## 17. Key Risks and Decisions to Resolve

- **“Any industry” scope:** avoid claiming universal readiness. The platform is domain-configurable, but each launched industry/jurisdiction needs reviewed templates, facts, and compliance policies.
- **Real-time provider fit:** ConversationRelay may reduce custom audio work, while Media Streams offers more control but materially increases audio, concurrency, and operational complexity. Decide with a spike, not assumption.
- **Model variability:** providers differ in structured output, streaming, cancellation, latency, data handling, and cost. Capability-test at profile publication and provider connection setup.
- **Tenant boundary retrofit:** current data and auth are single-tenant oriented. Treat tenant migration/security as a prerequisite to customer onboarding, not a cosmetic follow-up.
- **Compliance variability:** outbound calling, consent, recording, AI disclosure, opt-out, and retention vary by jurisdiction. Require explicit jurisdiction selection and legal review before campaign launch.
- **Prompt customization risk:** editable prompts alone are not a safe configuration interface. Expose bounded policies and fields; reserve advanced prompt overrides for platform operators or reviewed extensions.
- **Speech recognition quality:** proper nouns and industry vocabulary remain provider-dependent. Use hints and clarification, but measure recognition quality; do not rely on prompt wording to fix ASR errors.
- **Recording and data cost:** default recording should be off unless required and consented; define storage, retention, deletion, and customer access before enabling it broadly.

## 18. Recommended First Implementation Slice

Start with Phase 0 and the first half of Phase 1, not a full streaming rewrite. In one vertical slice:

1. Create a typed, versioned generic agent profile with configurable fields and approved facts.
2. Resolve the profile version from campaign at call creation and pass it into every webhook/turn.
3. Persist role-separated turns and structured fact state.
4. Replace the current global voice prompt with a structured conversation decision contract.
5. Add the Arjan ambiguity, direct-question response, already-known-field, opt-out, and unsupported-fact scenarios as automated tests.
6. Ship a simple simulator and config validation before building a broad visual editor.
7. Keep Twilio `<Gather>` operational while measuring turn latency; run a separate ConversationRelay/Media Streams proof before committing to real-time architecture.

This slice proves domain-neutral configuration and materially more realistic conversation while keeping the existing calling workflow available. It also creates the state/version boundaries needed for a later real-time voice transport.