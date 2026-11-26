# Regras de Negócio — InsureHub

Fonte da verdade do comportamento esperado do sistema. Cada regra tem um ID (`RN-xx`) para uso na matriz de rastreabilidade.

Todas as respostas de erro seguem o formato:

```json
{
  "timestamp": "2026-11-25T21:00:00Z",
  "status": 422,
  "code": "QUOTE_EXPIRED",
  "message": "Cotação expirada",
  "path": "/api/v1/policies",
  "fields": [{ "field": "cpf", "message": "CPF inválido" }]
}
```

`fields` só aparece em erros de validação (`code = VALIDATION_ERROR`, HTTP 400).

---

## RN-01 — Cadastro de cliente

| Campo | Regra |
|---|---|
| `name` | obrigatório, 3 a 120 caracteres |
| `cpf` | obrigatório, 11 dígitos (só números), dígitos verificadores válidos, único |
| `email` | obrigatório, formato válido, único (case-insensitive, salvo em minúsculas) |
| `password` | 8 a 64 caracteres, ao menos 1 letra e 1 número |
| `birthDate` | obrigatório, cliente deve ter **18 anos completos** na data do cadastro |

- CPF já cadastrado → `409 CPF_ALREADY_REGISTERED`
- E-mail já cadastrado → `409 EMAIL_ALREADY_REGISTERED`
- Menor de 18 → `422 UNDERAGE_CUSTOMER`
- Senha é armazenada com hash BCrypt (nunca em texto puro).
- Todo cliente cadastrado recebe perfil `CUSTOMER`. O perfil `ADMIN` só existe via carga inicial.

## RN-02 — Autenticação

- Login por e-mail + senha. Sucesso retorna JWT (HS256) válido por **60 minutos**, com `role` no token.
- Credenciais inválidas → `401 INVALID_CREDENTIALS` (mesma mensagem para e-mail inexistente e senha errada).
- **5 tentativas erradas consecutivas** bloqueiam a conta por **15 minutos** → `423 ACCOUNT_LOCKED` (inclusive com senha certa durante o bloqueio).
- Login com sucesso zera o contador de tentativas.
- Endpoints protegidos sem token ou com token inválido/expirado → `401 UNAUTHORIZED`.
- Endpoint de admin acessado por `CUSTOMER` → `403 FORBIDDEN`.

## RN-03 — Cotação

Linhas de produto: `AUTO`, `RESIDENCIAL`, `VIDA`.

Produtos: `AUTO`, `RESIDENCIAL`, `VIDA`, `PREVIDENCIA`, `VIAGEM`.

| Produto | Valor cotado (R$) | Taxa base | Idade aceita | Fator de idade |
|---|---|---|---|---|
| AUTO | importância 10.000,00 a 500.000,00 | 4,0% ao ano | 18 a 80 | 18–25: +30% · 26–60: 0% · 61–80: +15% |
| RESIDENCIAL | importância 50.000,00 a 2.000.000,00 | 0,5% ao ano | 18 ou mais | sem fator |
| VIDA | importância 20.000,00 a 1.000.000,00 | 1,2% ao ano | 18 a 70 | 18–49: 0% · 50–70: +40% |
| PREVIDENCIA | **contribuição mensal** 100,00 a 50.000,00 | sem taxa | 18 a 75 | sem fator |
| VIAGEM | importância (cobertura médica) 30.000,00 a 1.000.000,00 | 0,04% **por dia** | 18 a 85 | 18–59: 0% · 60–85: +50% |

Planos (obrigatório em toda cotação):

| Plano | Multiplicador do prêmio | Franquia Auto (% da importância) | Taxa de administração Previdência (a.a.) |
|---|---|---|---|
| `ESSENCIAL` | 1,00 | 6% | 1,5% |
| `COMPLETO` | 1,25 | 4% | 1,0% |
| `PREMIUM` | 1,50 | 2% | 0,5% |

Nome do plano exibido: `{Produto} {Plano}` (ex.: `Auto Completo`).

Campos condicionais (faltando ou sobrando → `400 VALIDATION_ERROR` no campo):

| Campo | AUTO / RESIDENCIAL / VIDA | PREVIDENCIA | VIAGEM |
|---|---|---|---|
| `insuredAmount` | obrigatório | proibido | obrigatório |
| `monthlyContribution` | proibido | obrigatório | proibido |
| `tripDays` (1 a 90) | proibido | proibido | obrigatório |

Cálculo:

```
AUTO, RESIDENCIAL, VIDA
  premioAnual  = importância × taxa × (1 + fatorIdade) × multiplicadorPlano × (1 − desconto)
  premioMensal = premioAnual / 12

VIAGEM (prêmio único)
  premio       = importância × 0,0004 × dias × (1 + fatorIdade) × multiplicadorPlano × (1 − desconto)
  premioAnual  = premioMensal = premio

PREVIDENCIA
  premioMensal = contribuição mensal
  premioAnual  = contribuição × 12      (sem fator, plano ou desconto)

desconto = 10% só em cotações de portabilidade de seguros (RN-09), senão 0
valores arredondados para 2 casas (HALF_EVEN) no final
```

- Idade = anos completos do cliente na data da cotação.
- Valor fora da faixa → `422 INSURED_AMOUNT_OUT_OF_RANGE`
- Idade fora da faixa → `422 AGE_NOT_ELIGIBLE`
- Cotação nasce `PENDING` e vale **7 dias** (`expiresAt = createdAt + 7 dias`).
- Na API, cotação `PENDING` vencida é exibida com status `EXPIRED` (no banco continua `PENDING`).
- Cliente só vê as próprias cotações. Cotação de outro cliente → `404 QUOTE_NOT_FOUND`.

## RN-04 — Emissão de apólice

- Emitida a partir de uma cotação `PENDING`, não expirada, do próprio cliente.
- Cotação expirada → `422 QUOTE_EXPIRED`. Cotação já usada → `409 QUOTE_ALREADY_CONVERTED`.
- `startDate` opcional: padrão hoje; se informado, entre hoje e hoje + 30 dias (**VIAGEM: + 180 dias**, é a data de embarque) → senão `422 INVALID_START_DATE`.
- `endDate`:
  - AUTO, RESIDENCIAL, VIDA: `startDate + 1 ano − 1 dia`;
  - VIAGEM: `startDate + tripDays − 1 dia`;
  - PREVIDENCIA: `startDate + 30 anos − 1 dia`.
- **Uma apólice `ACTIVE` por produto por cliente** → `409 ACTIVE_POLICY_ALREADY_EXISTS`.
- Número: `IH-{AUT|RES|VID|PRV|VIA}-{ano}-{sequencial de 6 dígitos}` (ex.: `IH-AUT-2026-000001`).
- Segurado (`insuredName`) = nome do cliente.
- Ao emitir: apólice `ACTIVE`, cotação passa a `CONVERTED`.

### Dados do produto (`details`, obrigatório)

Validados só depois das regras acima. Erros vêm como `400 VALIDATION_ERROR` com campos `details.*` (ex.: `details.drivers[1].cpf`).

| Produto | Campos | Regras |
|---|---|---|
| AUTO | `plate`, `drivers[] {name, cpf}` | placa `AAA9999` ou Mercosul `AAA9A99` (hífen opcional, gravada em maiúsculas sem hífen); 1 a 3 condutores; nome 3–120; CPF válido e **não repetido** |
| VIDA | `beneficiaries[] {name, relationship, percentage}` | 1 a 5; nome 3–120; parentesco 2–40; percentual 1–100; **soma = 100** |
| PREVIDENCIA | `planType` (`PGBL`/`VGBL`), `beneficiaries[]` | beneficiários com as mesmas regras de VIDA |
| VIAGEM | `destination`, `travelers[] {name, birthDate}` | destino 2–80; 1 a 6 viajantes; nascimento no passado |
| RESIDENCIAL | `address {zipCode, street, number, complement, district, city, state}` | CEP 8 dígitos; UF válida; complemento opcional |

Campos calculados pelo sistema:

- AUTO: `deductible` (franquia) = importância × % do plano, gravado na emissão.
- VIAGEM: `tripDays` copiado da cotação.
- PREVIDENCIA: `adminFee` do plano; na leitura, `monthlyContribution` e `accumulatedBalance` = contribuição × contribuições feitas (meses desde o início, contando o mês inicial).
- VIDA: na leitura, `insuredCapital` (capital segurado, o "valor do ganho") = importância.
- RESIDENCIAL: na leitura, `coverageAmount` = importância.

## RN-05 — Cancelamento e vigência da apólice

- Só o dono cancela, e só apólice `ACTIVE` → senão `409 POLICY_NOT_ACTIVE`.
- Não pode cancelar com sinistro `OPEN` ou `IN_ANALYSIS` → `409 POLICY_HAS_PENDING_CLAIMS`.
- Motivo de cancelamento obrigatório (5 a 255 caracteres).
- Apólices `ACTIVE` com `endDate` anterior a hoje passam a `EXPIRED` (job diário e na inicialização).

## RN-06 — Sinistros

Tipos aceitos por linha:

| Linha | Tipos |
|---|---|
| AUTO | `COLISAO`, `ROUBO_FURTO`, `DANOS_TERCEIROS` |
| RESIDENCIAL | `INCENDIO`, `DANOS_ELETRICOS`, `ROUBO_FURTO` |
| VIDA | `MORTE`, `INVALIDEZ` |
| VIAGEM | `DESPESAS_MEDICAS`, `EXTRAVIO_BAGAGEM`, `CANCELAMENTO_VIAGEM` |
| PREVIDENCIA | nenhum: previdência não tem sinistro (`422 CLAIM_TYPE_NOT_COVERED`) |

Abertura (cliente):

- Apólice do próprio cliente e `ACTIVE` → senão `409 POLICY_NOT_ACTIVE`.
- Tipo incompatível com a linha → `422 CLAIM_TYPE_NOT_COVERED`.
- `occurrenceDate` dentro da vigência, não futura e no máximo **90 dias** atrás → `422 INVALID_OCCURRENCE_DATE`.
- `amount` > 0 e ≤ **saldo de cobertura** (importância − soma aprovada de sinistros `APPROVED`/`PAID`) → `422 AMOUNT_EXCEEDS_COVERAGE`.
- `description` de 20 a 1000 caracteres.
- Apenas **1 sinistro em aberto** (`OPEN`/`IN_ANALYSIS`) por apólice → `409 CLAIM_ALREADY_OPEN`.
- Protocolo: `SIN-{ano}-{sequencial de 6 dígitos}`.

Fluxo de status:

```
OPEN ──(admin: iniciar análise)──> IN_ANALYSIS ──(admin: aprovar)──> APPROVED ──(admin: pagar)──> PAID
  │                                      └────────(admin: recusar)──> REJECTED
  └──(cliente: cancelar)──> CANCELLED
```

- Transição fora do fluxo → `409 INVALID_CLAIM_TRANSITION`.
- Aprovar exige `approvedAmount` > 0, ≤ valor solicitado (`422 APPROVED_AMOUNT_EXCEEDS_CLAIM`) e ≤ saldo de cobertura (`422 AMOUNT_EXCEEDS_COVERAGE`).
- Recusar exige `reason` (10 a 500 caracteres).

## RN-07 — Consentimento Open Insurance (OPIN)

- Cliente concede consentimento para compartilhar dados de congêneres. Validade: **12 meses**.
- Só 1 consentimento `ACTIVE` por cliente → `409 CONSENT_ALREADY_ACTIVE`.
- Revogar → status `REVOKED` e **apaga o cache** de apólices externas do cliente (LGPD).
- Consentimento vencido é tratado como inexistente e passa a `EXPIRED` no primeiro acesso.
- Sem consentimento ativo, `GET /opin/consents/active` e `DELETE /opin/consents/active` → `404 CONSENT_NOT_FOUND`.

## RN-08 — Apólices de congêneres e cache de contingência

- Requer consentimento ativo → `403 CONSENT_REQUIRED`.
- Cache **fresco** (< 15 min) → devolve o cache sem chamar a OPIN (`source = CACHE`).
- Senão chama a OPIN (timeout 3 s). Sucesso → atualiza cache (`source = LIVE`).
- OPIN com erro 5xx ou timeout:
  - existe cache com até **24 h** → devolve cache com `source = CACHE_FALLBACK` e `stale = true`;
  - sem cache utilizável → `503 OPIN_UNAVAILABLE`.
- `?refresh=true` ignora o cache fresco e força chamada à OPIN (fallback continua valendo).

## RN-09 — Portabilidade

- A partir de uma apólice de congênere do cache do cliente, gera uma **cotação** (`origin = PORTABILITY`).
- Produtos portáveis: `AUTO`, `RESIDENCIAL`, `VIDA`, `PREVIDENCIA`. Viagem ou produto desconhecido → `422 LINE_NOT_SUPPORTED`.
- **Seguros** (Auto, Residencial, Vida): plano `ESSENCIAL`, mesma importância, **10% de desconto**; a apólice externa precisa vencer nos **próximos 60 dias** (janela de renovação) → `422 OUTSIDE_RENEWAL_WINDOW`.
- **Previdência**: pode ser portada a qualquer momento; plano `PREMIUM` (menor taxa de administração), mesma contribuição mensal (`details.monthlyContribution` da congênere), sem desconto. Sem esse dado → `422 EXTERNAL_DATA_INCOMPLETE`.
- Cada apólice externa só gera uma cotação de portabilidade → `409 ALREADY_PORTED`.
- Requer consentimento ativo (`403 CONSENT_REQUIRED`) e a apólice externa no cache do cliente (`404 EXTERNAL_POLICY_NOT_FOUND`).
- As demais regras de cotação (RN-03) continuam valendo.

## RN-10 — Auditoria

Eventos gravados na tabela `audit_log` (`user_id`, `action`, `entity`, `entity_id`, `details`, `created_at`):

`USER_REGISTERED`, `LOGIN_SUCCESS`, `LOGIN_FAILED`, `ACCOUNT_LOCKED`, `QUOTE_CREATED`, `POLICY_ISSUED`, `POLICY_CANCELLED`, `POLICY_EXPIRED`, `CLAIM_OPENED`, `CLAIM_STATUS_CHANGED`, `CONSENT_GRANTED`, `CONSENT_REVOKED`, `PORTABILITY_QUOTE_CREATED`.

## RN-11 — Carteira (Home)

`GET /api/v1/portfolio`

- Total de produtos é sempre **5** (Vida, Auto, Residencial, Previdência, Viagem, nessa ordem).
- Um produto conta como **protegido** se o cliente tem ao menos uma apólice:
  - InsureHub com status `ACTIVE`; ou
  - compartilhada via OPIN, de um dos 5 produtos, com `endDate` ≥ hoje.
- `protectedCount` = quantidade de produtos protegidos (não de apólices). Ex.: 1 Auto InsureHub + 1 Vida OPIN → **2 de 5**; 1 Auto InsureHub + 1 Auto OPIN → **1 de 5**.
- `holding` de cada produto:

| InsureHub | OPIN | `holding` | Selo na tela |
|---|---|---|---|
| sim | não | `INSUREHUB` | InsureHub |
| não | sim | `OPIN` | OPIN |
| sim | sim | `INSUREHUB_OPIN` | InsureHub \| OPIN |
| não | não | `NONE` | produto esmaecido |

- Dados OPIN seguem a RN-08 (cache/fallback). Sem consentimento → `opin.consent = false` e só a InsureHub conta. OPIN fora e sem cache → `opin.available = false`, só a InsureHub conta, e a Home **não** falha.

## RN-12 — Tela do produto e busca

`GET /api/v1/portfolio/{produto}?q=`

- `{produto}` aceita o código sem diferenciar maiúsculas (`auto`, `AUTO`). Inválido → `404 PRODUCT_NOT_FOUND`.
- Lista as mesmas apólices que contam na RN-11: primeiro as da InsureHub, depois as OPIN por vencimento.
- Cada item: `source` (`INSUREHUB`/`OPIN`), `institution` (InsureHub ou nome da congênere), `policyNumber`, `planName`, `insuredName`, `startDate`, `endDate` e `details` do produto (ver RN-04).
- Busca `q`: contém o texto em **nome do plano, instituição ou número da apólice**; ignora maiúsculas e acentos ("previdencia" encontra "Previdência"); vazia → tudo. Mais de 60 caracteres → `422 QUERY_TOO_LONG`.
- `total` = quantidade sem filtro; `items` = resultado filtrado.
