# Histórias de Usuário — InsureHub

Backlog do produto. Critérios de aceite referenciam as regras em [regras-de-negocio.md](regras-de-negocio.md).

**Produto:** InsureHub é uma carteira digital de seguros. O cliente cota e contrata Vida, Auto, Residencial, Previdência e Viagem, abre sinistros e, via Open Insurance, enxerga num só lugar as apólices que tem em outras seguradoras (congêneres), podendo trazê-las para a InsureHub com desconto (portabilidade).

**Personas:**
- **Cliente** — pessoa física, maior de idade.
- **Analista de sinistros (ADMIN)** — analisa, aprova, recusa e paga sinistros.

---

## Épico 1 — Conta e acesso

### US-01 Cadastro de cliente
**Como** visitante **quero** criar minha conta **para** contratar seguros.
- CA1: cadastro válido retorna 201 com os dados do cliente, sem a senha. (RN-01)
- CA2: CPF inválido, e-mail inválido, senha fraca ou campos faltando retornam 400 com a lista de campos. (RN-01)
- CA3: CPF ou e-mail repetido retorna 409. (RN-01)
- CA4: menor de 18 anos retorna 422. (RN-01)

### US-02 Login
**Como** cliente **quero** entrar com e-mail e senha **para** acessar minha carteira.
- CA1: credenciais válidas retornam token e expiração. (RN-02)
- CA2: credenciais inválidas retornam 401 sem revelar qual campo está errado. (RN-02)
- CA3: 5 erros seguidos bloqueiam a conta por 15 min. (RN-02)

## Épico 2 — Cotação e contratação

### US-03 Cotar seguro
**Como** cliente **quero** simular o preço de um seguro **para** decidir se contrato.
- CA1: cotação válida retorna prêmio anual e mensal conforme a tabela da RN-03.
- CA2: importância ou idade fora da faixa retornam 422. (RN-03)
- CA3: a cotação vale 7 dias. (RN-03)

### US-04 Contratar seguro
**Como** cliente **quero** transformar uma cotação em apólice **para** ficar segurado.
- CA1: emissão gera número no padrão e vigência de 1 ano. (RN-04)
- CA2: cotação expirada ou já usada não emite apólice. (RN-04)
- CA3: não posso ter duas apólices ativas da mesma linha. (RN-04)

### US-05 Consultar e cancelar apólices
**Como** cliente **quero** ver e cancelar minhas apólices.
- CA1: lista só as minhas apólices, com filtro opcional por status. (RN-04)
- CA2: cancelamento exige motivo e é bloqueado se houver sinistro pendente. (RN-05)

## Épico 3 — Sinistros

### US-06 Abrir sinistro
**Como** cliente **quero** comunicar um sinistro **para** receber a indenização.
- CA1: sinistro válido gera protocolo e status `OPEN`. (RN-06)
- CA2: tipo, data, valor e descrição são validados. (RN-06)
- CA3: só um sinistro em aberto por apólice. (RN-06)

### US-07 Analisar sinistro
**Como** analista **quero** analisar, aprovar, recusar e pagar sinistros.
- CA1: o fluxo de status segue o diagrama da RN-06; transição inválida retorna 409.
- CA2: o valor aprovado respeita o saldo de cobertura. (RN-06)
- CA3: cliente não acessa endpoints de analista (403). (RN-02)

### US-08 Cancelar sinistro
**Como** cliente **quero** cancelar um sinistro aberto por engano.
- CA1: só sinistros `OPEN` podem ser cancelados pelo cliente. (RN-06)

## Épico 4 — Open Insurance

### US-09 Consentimento
**Como** cliente **quero** autorizar o compartilhamento dos meus dados de outras seguradoras.
- CA1: consentimento vale 12 meses; só 1 ativo por vez. (RN-07)
- CA2: ao revogar, meus dados externos são apagados. (RN-07)

### US-10 Ver apólices de congêneres
**Como** cliente **quero** ver na InsureHub as apólices que tenho em outras seguradoras.
- CA1: sem consentimento ativo retorna 403. (RN-08)
- CA2: dados recentes vêm do cache, sem nova chamada à OPIN. (RN-08)
- CA3: se a OPIN cair, vejo os últimos dados salvos, sinalizados como desatualizados. (RN-08)
- CA4: OPIN fora do ar e sem cache retorna 503. (RN-08)

### US-11 Portabilidade
**Como** cliente **quero** trazer uma apólice de outra seguradora para a InsureHub com desconto.
- CA1: gera cotação com 10% de desconto para apólices perto do vencimento. (RN-09)
- CA2: cada apólice externa só pode ser portada uma vez. (RN-09)

## Épico 5 — Carteira

### US-13 Ver minha proteção na Home
**Como** cliente **quero** ver quantos dos 5 produtos eu tenho protegidos **para** saber o que falta contratar.
- CA1: o gráfico mostra "X de 5", contando produtos (não apólices). (RN-11)
- CA2: cada produto mostra o selo InsureHub, OPIN ou InsureHub | OPIN conforme a origem. (RN-11)
- CA3: produtos que não tenho aparecem esmaecidos. (RN-11)
- CA4: se a OPIN cair, a Home continua mostrando os produtos da InsureHub. (RN-11)

### US-14 Ver as apólices de um produto
**Como** cliente **quero** abrir um produto e ver cada apólice dele **para** conferir os detalhes.
- CA1: cada apólice aparece num card com instituição, logo (InsureHub ou OPIN), número e nome do plano. (RN-12)
- CA2: ao expandir o card vejo vigência, segurado e os dados específicos do produto. (RN-04, RN-12)
- CA3: consigo buscar por nome do plano, instituição ou número da apólice, sem me preocupar com acentos. (RN-12)

### US-15 Informar os dados do produto ao contratar
**Como** cliente **quero** informar placa, beneficiários, endereço ou viajantes ao contratar **para** que a apólice fique completa.
- CA1: os dados pedidos dependem do produto e são validados. (RN-04)
- CA2: em Vida e Previdência, os percentuais dos beneficiários somam 100%. (RN-04)

## Épico 6 — Rastreabilidade

### US-12 Auditoria
**Como** área de compliance **quero** registro das ações relevantes.
- CA1: os eventos da RN-10 são gravados com usuário, entidade e data.
