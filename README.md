# InsureHub

Carteira digital de seguros com integração **Open Insurance (OPIN)**: o cliente vê numa só tela quantos dos 5 produtos (Vida, Auto, Residencial, Previdência e Viagem) tem protegidos, seja na InsureHub ou em outras seguradoras; cota e contrata em 3 planos, abre sinistros e pode trazer apólices de outras seguradoras para a InsureHub (portabilidade).

Este repositório é um **projeto de portfólio de QA**. O sistema-alvo (API, banco, mock da OPIN e front) foi gerado como ambiente de estudo. **A estratégia, o plano e toda a automação de testes são de autoria de Arthur Machado.**

> Status: sistema pronto. Suíte de testes em construção (ver [plano.md](plano.md)).

## Arquitetura

```
┌──────────────┐  HTTP/JSON  ┌────────────────────────┐   JDBC   ┌──────────────┐
│ Front (HTML) │ ──────────▶ │  InsureHub API         │ ───────▶ │ PostgreSQL 16│
│ /index.html  │   JWT       │  Spring Boot 3 / Java21│  Flyway  └──────────────┘
└──────────────┘             │                        │
                             │  cache de contingência │   HTTP   ┌──────────────┐
                             │  (RN-08)               │ ───────▶ │ OPIN mock    │
                             └────────────────────────┘ timeout  │ (WireMock)   │
                                                          3 s    └──────────────┘
```

| Componente | Tecnologia | Porta |
|---|---|---|
| API + front | Spring Boot 3.3, Spring Security (JWT HS256), JPA, Flyway, springdoc | 8080 |
| Banco | PostgreSQL 16 | 5432 |
| Open Insurance simulada | WireMock 3 (`opin-mock/mappings`) | 8089 |

## Como rodar

Pré-requisito: Docker Desktop.

```bash
docker compose up -d --build
```

| O quê | URL |
|---|---|
| Front | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |
| WireMock admin | http://localhost:8089/__admin/mappings |

Zerar o banco (volta à massa inicial): `docker compose down -v && docker compose up -d`.

Rodar só a API fora do Docker (Java 21): suba `postgres` e `opin-mock` com `docker compose up -d postgres opin-mock` e rode `./mvnw spring-boot:run` dentro de `api/`.

## Massa de dados inicial

| Perfil | E-mail | Senha | CPF | Nascimento | Comportamento na OPIN |
|---|---|---|---|---|---|
| ADMIN | admin@insurehub.com | Admin123 | 12345678909 | 1985-01-15 | — |
| CUSTOMER | demo@insurehub.com | Senha123 | 98765432100 | 1988-07-03 | **carteira de demonstração**: Auto contratado na InsureHub + consentimento ativo → Home com 4 de 5 e os 4 estados de selo |
| CUSTOMER | maria@insurehub.com | Senha123 | 52998224725 | 1990-05-10 | 4 apólices padrão (sem consentimento ainda) |
| CUSTOMER | joao@insurehub.com | Senha123 | 39053344705 | 2003-03-15 | congênere fora do ar (503) |
| CUSTOMER | ana@insurehub.com | Senha123 | 71460238001 | 1970-08-20 | resposta lenta (5 s > timeout de 3 s) |

**Apólices padrão da OPIN** (qualquer CPF válido): Auto na Seguradora Alfa (vence em 30 dias, portável), Vida na Delta (vence em 200 dias), Previdência VGBL na Beta e Viagem para Lisboa na Gama. Residencial não vem compartilhado. CPF `11144477735` (livre para cadastro) não tem apólices em congêneres.

**Simular a OPIN fora do ar em tempo de execução** (útil para testar o fallback de cache):

```bash
# derruba a OPIN para todos os CPFs
curl -X POST http://localhost:8089/__admin/mappings \
  -H "Content-Type: application/json" \
  -d '{"priority":1,"request":{"method":"GET","urlPath":"/opin/v1/insurance-policies"},"response":{"status":500}}'

# volta aos mapeamentos originais
curl -X POST http://localhost:8089/__admin/mappings/reset
```

## Endpoints

| Método | Rota | Perfil | Regra |
|---|---|---|---|
| POST | `/api/v1/auth/register` | público | RN-01 |
| POST | `/api/v1/auth/login` | público | RN-02 |
| GET | `/api/v1/me` | autenticado | — |
| GET | `/api/v1/portfolio` | CUSTOMER | RN-11 |
| GET | `/api/v1/portfolio/{produto}` (`?q=`) | CUSTOMER | RN-12 |
| POST / GET | `/api/v1/quotes`, `/api/v1/quotes/{id}` | CUSTOMER | RN-03 |
| POST / GET | `/api/v1/policies`, `/api/v1/policies/{id}` (`?status=`) | CUSTOMER | RN-04 |
| POST | `/api/v1/policies/{id}/cancel` | CUSTOMER | RN-05 |
| POST / GET | `/api/v1/claims`, `/api/v1/claims/{id}` | CUSTOMER | RN-06 |
| POST | `/api/v1/claims/{id}/cancel` | CUSTOMER | RN-06 |
| GET | `/api/v1/admin/claims` (`?status=`), `/api/v1/admin/claims/{id}` | ADMIN | RN-06 |
| POST | `/api/v1/admin/claims/{id}/start-analysis` · `/approve` · `/reject` · `/pay` | ADMIN | RN-06 |
| POST | `/api/v1/opin/consents` | CUSTOMER | RN-07 |
| GET / DELETE | `/api/v1/opin/consents/active` | CUSTOMER | RN-07 |
| GET | `/api/v1/opin/external-policies` (`?refresh=true`) | CUSTOMER | RN-08 |
| POST | `/api/v1/portability` | CUSTOMER | RN-09 |
| GET | `/api/v1/admin/audit` (`?userId=&action=&page=&size=`) | ADMIN | RN-10 |

Contrato completo no Swagger.

## Documentação do produto

- [Regras de negócio (RN-01 a RN-12)](docs/regras-de-negocio.md): fonte da verdade, com códigos de erro.
- [Histórias de usuário (US-01 a US-15)](docs/historias-de-usuario.md): backlog com critérios de aceite.

## Banco de dados

Schema em [`api/src/main/resources/db/migration`](api/src/main/resources/db/migration) (V1 base, V2 previdência/viagem, planos e dados do produto em `jsonb`). Tabelas: `users`, `quotes`, `policies`, `claims`, `consents`, `external_policies_cache`, `audit_log`, mais as sequences `policy_number_seq` e `claim_protocol_seq`.

Regras garantidas também no banco: CPF/e-mail únicos, uma apólice `ACTIVE` por linha por cliente (índice único parcial), um consentimento `ACTIVE` por cliente, uma portabilidade por apólice externa, `CHECK` de status e datas.

Acesso: `localhost:5432`, banco/usuário/senha `insurehub`.

## Estrutura

```
insurehub/
├── api/                 # sistema-alvo (Spring Boot)
├── opin-mock/mappings/  # stubs WireMock da Open Insurance
├── docs/                # regras de negócio e histórias de usuário
├── docker-compose.yml
└── plano.md             # roadmap do trabalho de QA
```
