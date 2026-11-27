# Plano — InsureHub

Roteiro do projeto de QA, organizado como um projeto real em sprints. Cada etapa termina em um PR revisado.

**Decisões:**
- Monorepo: sistema e testes no mesmo repositório.
- API e banco: Java 21 + JUnit 5 + REST Assured.
- E2E: TypeScript + Playwright.
- Performance: k6.
- CI: GitHub Actions.
- Relatórios: Allure.

---

## Fase 0 — Sistema-alvo (Claude) ✅

- [x] Regras de negócio RN-01 a RN-10 e histórias US-01 a US-12
- [x] API Spring Boot 3 / Java 21 com JWT, Flyway e PostgreSQL
- [x] Mock da OPIN no WireMock (sucesso, 503, timeout, vazio)
- [x] Cache de contingência OPIN (LIVE / CACHE / CACHE_FALLBACK)
- [x] Front mínimo com `data-testid` para E2E
- [x] docker-compose (api + postgres + opin-mock)
- [x] Verificação manual ponta a ponta de todas as regras

## Fase 0.1 — Carteira e novo layout (Claude) ✅

- [x] Previdência e Viagem vendidos pela InsureHub; 3 planos (Essencial, Completo, Premium)
- [x] Dados do produto na contratação (placa/condutores, beneficiários, endereço, viajantes, PGBL/VGBL)
- [x] Home com pentágono "X de 5" e selos InsureHub / OPIN / InsureHub | OPIN (RN-11)
- [x] Tela do produto com accordion e busca por plano, instituição ou número (RN-12)
- [x] Migration V2, mock OPIN com os 5 produtos, usuário demo com carteira preenchida
- [x] Redesign de todas as telas (Contratar em 3 passos, Sinistros, Outras seguradoras, Analista)

---

## Sprint 0 — Fundação

### Etapa 1. Repositório e forma de trabalhar
- [ ] `git init`, repositório no GitHub, branch `main` protegida (só entra por PR)
- [ ] Fluxo: branch por tarefa (`feat/`, `test/`, `docs/`, `ci/`), Conventional Commits, um PR por entrega
- [ ] Quadro no GitHub Projects com uma issue por etapa deste roteiro
- [ ] Templates de issue (bug, tarefa) e de PR
- [ ] Estrutura de pastas: `docs/qa/`, `tests/api/`, `tests/e2e/`, `tests/performance/`, `.github/workflows/`
- **Pronto quando:** repositório publicado e backlog montado no quadro.

## Sprint 1 — Análise e planejamento

### Etapa 2. Revisão dos requisitos (teste estático, cap. 3 do CTFL)
- [ ] Revisar RN-01 a RN-12 e US-01 a US-15: ambiguidades, lacunas, contradições, critérios não testáveis
- [ ] Abrir cada achado como issue "dúvida para o PO" (Claude responde como PO)
- **Entregável:** `docs/qa/revisao-de-requisitos.md`

### Etapa 3. Análise de risco
- [ ] Probabilidade × impacto (1 a 5) por regra; matriz de risco
- [ ] Prioridade de teste derivada do risco
- **Entregável:** `docs/qa/analise-de-risco.md`

### Etapa 4. Estratégia de teste
- [ ] Pirâmide e justificativa; níveis e tipos de teste
- [ ] Ferramentas e motivo de cada uma
- [ ] Ambientes (local, CI) e estratégia de dados de teste (cada teste cria a própria massa)
- [ ] O que fica fora do escopo
- **Entregável:** `docs/qa/estrategia-de-teste.md`

### Etapa 5. Plano de teste
- [ ] Escopo por sprint, critérios de entrada e saída, Definition of Done
- [ ] Entregáveis, riscos do projeto, estimativa (três pontos)
- **Entregável:** `docs/qa/plano-de-teste.md`

## Sprint 2 — Modelagem

### Etapa 6. Modelagem de casos de teste (cap. 4 do CTFL)
- [ ] Partição de equivalência e valor limite: idade, faixas de valor, dias de viagem, tamanhos de campo, busca com 60 caracteres
- [ ] Tabela de decisão: campos condicionais por produto (RN-03), selos da Home (RN-11), portabilidade (RN-09)
- [ ] Transição de estado: sinistro, consentimento, cotação, bloqueio de login
- [ ] Baseada em experiência: acesso a recurso de outro cliente, token adulterado, JSON com campos extras
- [ ] Casos em Gherkin com IDs `CT-RNxx-nnn`
- **Entregável:** `docs/qa/casos-de-teste/` (um arquivo por regra)

### Etapa 7. Matriz de rastreabilidade
- [ ] RN ↔ US ↔ caso de teste ↔ teste automatizado (última coluna preenchida conforme a automação avança)
- **Entregável:** `docs/qa/rastreabilidade.md`

### Etapa 8. Teste exploratório
- [ ] 2 ou 3 sessões de 45 min com charter
- [ ] Notas de sessão e bugs abertos com severidade e prioridade
- **Entregável:** `docs/qa/exploratorio/` + issues de bug

## Sprint 3 — Automação de API e banco

### Etapa 9. Projeto `tests/api`
- [ ] Java 21, Maven, JUnit 5, REST Assured, AssertJ, Allure, Datafaker, validação de JSON Schema
- [ ] Camadas: `config`, `clients`, `models`, `factories`, `support` (auth, banco, WireMock), `tests` por domínio
- [ ] Tags: `smoke`, `regression` e ID da regra (`RN-03`)
- **Pronto quando:** um teste de login roda via `mvn test` e gera relatório Allure.

### Etapa 10. Testes de API por ordem de risco
- [ ] Autenticação e acesso (login, bloqueio, 401/403, recurso de outro cliente → 404)
- [ ] Cotação: parametrizados para 5 produtos × 3 planos × faixas de idade
- [ ] Emissão e `details` por produto
- [ ] Sinistros: todas as transições válidas e inválidas
- [ ] OPIN e cache, controlando o WireMock pela API de administração
- [ ] Portabilidade, Carteira/Home, tela do produto e busca, auditoria
- [ ] Contrato das respostas com JSON Schema

### Etapa 11. Testes de banco
- [ ] Helper JDBC para conferir o estado após as operações (apólice, `jsonb`, cotação CONVERTED, `audit_log`, cache apagado ao revogar)
- [ ] Restrições do banco: inserts diretos que violam regras devem ser recusados
- [ ] Migração: banco vazio → V1 → V2
- [ ] Estratégia de limpeza entre testes

### Etapa 12. Ambiente isolado
- [ ] Testcontainers subindo o docker-compose antes da suíte
- **Pronto quando:** `mvn verify` roda do zero numa máquina só com Docker.

## Sprint 4 — E2E, contrato e não funcionais

### Etapa 13. E2E com Playwright (`tests/e2e`)
- [ ] TypeScript, Page Objects, fixtures, login via API com `storageState`, massa via API
- [ ] Fluxos críticos: 4 estados de selo e "X de 5"; accordion e busca; contratação de 2+ produtos; sinistro; consentimento
- [ ] Projeto em viewport de celular e teste de acessibilidade com axe

### Etapa 14. Teste de contrato
- [ ] Respostas reais validadas contra o OpenAPI (`/v3/api-docs`)
- [ ] Opcional: Pact entre API e OPIN

### Etapa 15. Performance com k6 (`tests/performance`)
- [ ] Cenários smoke e carga: login, cotação, `/portfolio`
- [ ] Thresholds (ex.: p95 < 500 ms, erros < 1%) e análise curta dos resultados

### Etapa 16. Segurança básica
- [ ] Token adulterado/expirado, IDOR, injeção na busca
- [ ] Opcional: OWASP ZAP baseline no pipeline

## Sprint 5 — CI/CD e qualidade contínua

### Etapa 17. GitHub Actions
- [ ] `pr.yml`: build, lint, ambiente, smoke de API e E2E; falhou bloqueia o merge
- [ ] `regression.yml`: noturno, suíte completa (API, banco, E2E, k6)
- [ ] `manual.yml`: execução sob demanda por suíte
- [ ] Cache, paralelismo, artefatos (Allure, traces, k6) e proteção da `main` exigindo os checks

### Etapa 18. Relatórios
- [ ] Allure no GitHub Pages com histórico
- [ ] Rastreabilidade gerada a partir das tags
- [ ] Badges no README

### Etapa 19. Higiene do código de teste
- [ ] Spotless / Prettier + ESLint
- [ ] Revisão de PR (Claude como tech lead)
- [ ] Acompanhamento de testes instáveis

## Sprint 6 — Encerramento e publicação

### Etapa 20. Relatório final de teste
- [ ] O que foi testado, cobertura por regra, defeitos por severidade, riscos residuais, métricas, lições aprendidas
- **Entregável:** `docs/qa/relatorio-final.md`

### Etapa 21. Publicação
- [ ] README informando que o sistema-alvo foi gerado e que o QA é autoral
- [ ] README final: arquitetura, como rodar, ADRs, GIF do E2E, links de relatório e docs
- [ ] Repositório público
- [ ] Card no site de portfólio (PT/EN) e atualização do `perfil.md`
- [ ] Linha no CV e post no LinkedIn
