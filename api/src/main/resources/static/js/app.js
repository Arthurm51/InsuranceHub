"use strict";

/* =========================================================
   InsureHub — front-end (SPA sem build, rotas por hash)
   ========================================================= */

const API = "/api/v1";

const PRODUCTS = {
  VIDA: { label: "Vida", slug: "vida", icon: "ico-vida", claimTypes: ["MORTE", "INVALIDEZ"] },
  AUTO: { label: "Auto", slug: "auto", icon: "ico-auto", claimTypes: ["COLISAO", "ROUBO_FURTO", "DANOS_TERCEIROS"] },
  RESIDENCIAL: { label: "Residencial", slug: "residencial", icon: "ico-residencial", claimTypes: ["INCENDIO", "DANOS_ELETRICOS", "ROUBO_FURTO"] },
  PREVIDENCIA: { label: "Previdência", slug: "previdencia", icon: "ico-previdencia", claimTypes: [] },
  VIAGEM: { label: "Viagem", slug: "viagem", icon: "ico-viagem", claimTypes: ["DESPESAS_MEDICAS", "EXTRAVIO_BAGAGEM", "CANCELAMENTO_VIAGEM"] },
};
const ORDER = ["VIDA", "AUTO", "RESIDENCIAL", "PREVIDENCIA", "VIAGEM"];
const BY_SLUG = Object.fromEntries(ORDER.map((k) => [PRODUCTS[k].slug, k]));

const PLANS = {
  ESSENCIAL: { label: "Essencial", note: "Cobertura principal pelo menor preço. Franquia Auto de 6%." },
  COMPLETO: { label: "Completo", note: "Preço 25% maior, franquia Auto de 4% e taxa de 1% na previdência." },
  PREMIUM: { label: "Premium", note: "Preço 50% maior, franquia Auto de 2% e taxa de 0,5% na previdência." },
};

const AMOUNT_FIELD = {
  AUTO: { label: "Valor do veículo (R$)", hint: "De R$ 10.000 a R$ 500.000" },
  RESIDENCIAL: { label: "Valor da cobertura (R$)", hint: "De R$ 50.000 a R$ 2.000.000" },
  VIDA: { label: "Capital segurado (R$)", hint: "De R$ 20.000 a R$ 1.000.000. É o valor pago aos beneficiários." },
  VIAGEM: { label: "Cobertura médica (R$)", hint: "De R$ 30.000 a R$ 1.000.000" },
  PREVIDENCIA: { label: "Contribuição mensal (R$)", hint: "De R$ 100 a R$ 50.000" },
};

const UFS = "AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO".split(" ");

const state = {
  token: sessionStorage.getItem("ih_token"),
  role: sessionStorage.getItem("ih_role"),
  name: sessionStorage.getItem("ih_name"),
  me: null,
  contract: null,
};

/* ---------- utilitários ---------- */
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
const view = () => $("#view");
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const brl = (v) => (v == null ? "—" : Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" }));
const date = (iso) => (iso ? new Date(iso.length === 10 ? iso + "T12:00:00" : iso).toLocaleDateString("pt-BR") : "—");
const dateTime = (iso) => (iso ? new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" }) : "—");
const plural = (n, one, many) => `${n} ${n === 1 ? one : many}`;
const icon = (id, cls = "") => `<svg class="${cls}" aria-hidden="true"><use href="#${id}"/></svg>`;
const statusTag = (s) => `<span class="status ${esc(s)}" data-testid="status-badge">${esc(s)}</span>`;
const todayIso = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`; };
const reducedMotion = () => window.matchMedia("(prefers-reduced-motion: reduce)").matches;

function toast(msg, isError = false) {
  const t = $("#toast");
  t.textContent = msg;
  t.className = "toast" + (isError ? " error" : "");
  t.hidden = false;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => (t.hidden = true), 4500);
}

function errorList(err) {
  if (!err) return "";
  const items = err.fields?.length ? err.fields.map((f) => f.message) : [err.message || "Não foi possível concluir. Tente de novo."];
  return items.map((m) => `<li>${esc(m)}</li>`).join("");
}

async function api(path, { method = "GET", body } = {}) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const res = await fetch(API + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const data = res.status === 204 ? null : await res.json().catch(() => null);
  if (res.status === 401 && state.token) {
    logout();
    throw { message: "Sua sessão expirou. Entre novamente." };
  }
  if (!res.ok) throw data || { message: `Erro ${res.status}` };
  return data;
}

function formValues(form) {
  const obj = {};
  new FormData(form).forEach((v, k) => (obj[k] = typeof v === "string" ? v.trim() : v));
  return obj;
}

const numOrNull = (v) => (v === "" || v == null ? null : Number(v));

/* ---------- selos e marcas ---------- */
function seals(holding) {
  const ih = `<span class="seal seal-ih">${icon("mark-insurehub")}InsureHub</span>`;
  const op = `<span class="seal seal-opin">OPIN</span>`;
  const map = { INSUREHUB: ih, OPIN: op, INSUREHUB_OPIN: ih + op };
  if (!map[holding]) return `<span class="seal-none" data-testid="product-seal" data-holding="NONE">Não contratado</span>`;
  return `<span class="seals" data-testid="product-seal" data-holding="${holding}">${map[holding]}</span>`;
}

function institutionLogo(source) {
  return source === "OPIN"
    ? `<span class="logo logo-opin" data-testid="policy-logo" data-logo="OPIN" aria-label="Open Insurance">OPIN</span>`
    : `<span class="logo logo-ih" data-testid="policy-logo" data-logo="INSUREHUB" aria-label="InsureHub">${icon("mark-insurehub")}</span>`;
}

/* ---------- pentágono de proteção ---------- */
function pentagon(products, protectedCount) {
  const c = 180, r = 132;
  const pt = (deg, rad) => [c + rad * Math.cos((deg * Math.PI) / 180), c + rad * Math.sin((deg * Math.PI) / 180)];
  const wedges = products.map((p, i) => {
    const a0 = -90 + 72 * i, a1 = a0 + 72, mid = a0 + 36;
    const [x0, y0] = pt(a0, r), [x1, y1] = pt(a1, r), [lx, ly] = pt(mid, r * 0.81 + 44);
    const on = p.holding !== "NONE";
    const anchor = Math.abs(Math.cos((mid * Math.PI) / 180)) < 0.2 ? "middle" : Math.cos((mid * Math.PI) / 180) > 0 ? "start" : "end";
    return `
      <path class="wedge" data-on="${on}" data-testid="pentagon-wedge" data-line="${p.line}"
            d="M${c} ${c} L${x0.toFixed(1)} ${y0.toFixed(1)} L${x1.toFixed(1)} ${y1.toFixed(1)} Z"/>
      <text class="wedge-label ${on ? "on" : ""}" x="${lx.toFixed(1)}" y="${ly.toFixed(1)}" text-anchor="${anchor}" dominant-baseline="middle">${esc(p.label)}</text>`;
  }).join("");
  return `
    <svg class="pentagon" viewBox="-44 0 448 360" role="img" data-testid="protection-chart"
         aria-label="${protectedCount} de ${products.length} produtos protegidos">
      ${wedges}
      <circle class="hub" cx="${c}" cy="${c}" r="58"/>
      <text class="count" x="${c}" y="${c - 6}" text-anchor="middle" dominant-baseline="middle" data-testid="protected-count">${protectedCount}</text>
      <text class="of" x="${c}" y="${c + 30}" text-anchor="middle">de ${products.length}</text>
    </svg>`;
}

/** O único momento animado da Home: as fatias protegidas acendem em sequência. */
function lightUpWedges() {
  const on = $$('.pentagon .wedge[data-on="true"]');
  if (reducedMotion()) return on.forEach((w) => w.classList.add("on"));
  on.forEach((w, i) => setTimeout(() => w.classList.add("on"), 180 + i * 140));
}

/* =========================================================
   Autenticação
   ========================================================= */
function renderAuth() {
  view().innerHTML = `
    <section class="auth">
      <div class="auth-pitch">
        <svg viewBox="0 0 32 32" aria-hidden="true" style="color:var(--cobalt)"><use href="#mark-insurehub"/></svg>
        <h1>Seus seguros, todos num lugar só.</h1>
        <p>Veja o que você tem na InsureHub e em outras seguradoras, descubra o que ainda falta proteger e contrate em poucos minutos.</p>
      </div>
      <div class="panel">
        <div class="switch" role="tablist">
          <button role="tab" aria-selected="true" data-auth="login" data-testid="tab-login">Entrar</button>
          <button role="tab" aria-selected="false" data-auth="register" data-testid="tab-register">Criar conta</button>
        </div>
        <form id="login-form" class="stack" data-testid="login-form" novalidate>
          <label class="field"><span>E-mail</span><input name="email" type="email" autocomplete="username" data-testid="login-email" required></label>
          <label class="field"><span>Senha</span><input name="password" type="password" autocomplete="current-password" data-testid="login-password" required></label>
          <button class="btn" type="submit" data-testid="login-submit">Entrar</button>
        </form>
        <form id="register-form" class="stack" data-testid="register-form" novalidate hidden>
          <label class="field"><span>Nome completo</span><input name="name" autocomplete="name" data-testid="register-name" required></label>
          <label class="field"><span>CPF</span><input name="cpf" inputmode="numeric" maxlength="11" placeholder="Só números" data-testid="register-cpf" required></label>
          <label class="field"><span>E-mail</span><input name="email" type="email" autocomplete="email" data-testid="register-email" required></label>
          <label class="field"><span>Senha</span><input name="password" type="password" autocomplete="new-password" data-testid="register-password" required><small>De 8 a 64 caracteres, com letra e número.</small></label>
          <label class="field"><span>Data de nascimento</span><input name="birthDate" type="date" data-testid="register-birthdate" required></label>
          <button class="btn" type="submit" data-testid="register-submit">Criar conta</button>
        </form>
        <ul class="errors" id="auth-errors" data-testid="form-errors"></ul>
      </div>
    </section>`;

  $$("[data-auth]").forEach((tab) =>
    tab.addEventListener("click", () => {
      $$("[data-auth]").forEach((t) => t.setAttribute("aria-selected", String(t === tab)));
      $("#login-form").hidden = tab.dataset.auth !== "login";
      $("#register-form").hidden = tab.dataset.auth !== "register";
      $("#auth-errors").innerHTML = "";
    })
  );

  $("#login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      const r = await api("/auth/login", { method: "POST", body: formValues(e.target) });
      Object.assign(state, { token: r.accessToken, role: r.role, name: r.name, me: null });
      sessionStorage.setItem("ih_token", r.accessToken);
      sessionStorage.setItem("ih_role", r.role);
      sessionStorage.setItem("ih_name", r.name);
      location.hash = r.role === "ADMIN" ? "#/analista" : "#/inicio";
      route();
    } catch (err) {
      $("#auth-errors").innerHTML = errorList(err);
    }
  });

  $("#register-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await api("/auth/register", { method: "POST", body: formValues(e.target) });
      toast("Conta criada. Entre com seu e-mail e senha.");
      e.target.reset();
      $('[data-auth="login"]').click();
    } catch (err) {
      $("#auth-errors").innerHTML = errorList(err);
    }
  });
}

function logout() {
  Object.assign(state, { token: null, role: null, name: null, me: null, contract: null });
  sessionStorage.clear();
  location.hash = "";
  route();
}

async function me() {
  if (!state.me) state.me = await api("/me");
  return state.me;
}

/* =========================================================
   Home
   ========================================================= */
async function renderHome() {
  const data = await api("/portfolio");
  const missing = data.products.filter((p) => p.holding === "NONE").map((p) => p.label);
  const n = data.protectedCount, total = data.totalProducts;

  const headline = n === total ? `Você tem os ${total} produtos protegidos`
    : n === 0 ? "Nenhum produto protegido ainda"
    : `Você tem ${n} de ${total} produtos protegidos`;
  const lead = n === 0
    ? "Contrate um seguro ou traga os dados de outras seguradoras para começar."
    : missing.length ? `Ainda sem proteção: ${missing.join(", ")}.` : "Tudo coberto. Toque em um produto para ver suas apólices.";

  let opinNote = "";
  if (!data.opin.consent) {
    opinNote = `<p class="opin-note" data-testid="opin-note">Tem seguro em outra seguradora? <a href="#/compartilhamento">Autorize o compartilhamento</a> para ver tudo aqui.</p>`;
  } else if (!data.opin.available) {
    opinNote = `<p class="opin-note warn" data-testid="opin-note">As outras seguradoras não responderam agora. Mostrando só o que você tem na InsureHub.</p>`;
  } else if (data.opin.stale) {
    opinNote = `<p class="opin-note warn" data-testid="opin-note">As outras seguradoras não responderam agora. Mostrando os dados salvos em ${dateTime(data.opin.syncedAt)}.</p>`;
  }

  const rows = data.products.map((p) => {
    const meta = PRODUCTS[p.line];
    const sub = {
      INSUREHUB: plural(p.insureHubCount, "apólice na InsureHub", "apólices na InsureHub"),
      OPIN: plural(p.opinCount, "apólice em outra seguradora", "apólices em outras seguradoras"),
      INSUREHUB_OPIN: `${p.insureHubCount} na InsureHub e ${p.opinCount} em ${p.opinCount === 1 ? "outra seguradora" : "outras seguradoras"}`,
      NONE: "Você ainda não tem este produto",
    }[p.holding];
    return `
      <a class="product-row ${p.holding === "NONE" ? "none" : ""}" href="#/produto/${meta.slug}"
         data-testid="product-item" data-line="${p.line}" data-holding="${p.holding}">
        <span class="glyph">${icon(meta.icon)}</span>
        <span><span class="name">${esc(p.label)}</span><br><span class="sub">${sub}</span></span>
        ${seals(p.holding)}
        ${icon("ico-chevron", "chev")}
      </a>`;
  }).join("");

  view().innerHTML = `
    <section class="overview">
      ${pentagon(data.products, n)}
      <div class="overview-text">
        <h1 data-testid="home-headline">${headline}</h1>
        <p class="lead" data-testid="home-missing">${esc(lead)}</p>
        ${opinNote}
      </div>
    </section>
    <nav class="products" aria-label="Produtos" data-testid="product-list">${rows}</nav>`;
  lightUpWedges();
}

/* =========================================================
   Tela do produto
   ========================================================= */
function detailFacts(line, d = {}) {
  const people = (list, fn) => `<ul>${(list || []).map((x) => `<li>${fn(x)}</li>`).join("") || "<li>—</li>"}</ul>`;
  const beneficiaries = (list) => people(list, (b) => `${esc(b.name)}${b.relationship ? ` (${esc(b.relationship)})` : ""}${b.percentage != null ? `, ${b.percentage}%` : ""}`);
  const f = (label, value, testid, wide = false) =>
    `<div class="${wide ? "wide" : ""}"><dt>${label}</dt><dd data-testid="detail-${testid}">${value}</dd></div>`;
  switch (line) {
    case "VIDA":
      return f("Valor do ganho (capital segurado)", brl(d.insuredCapital), "insured-capital") +
        f("Beneficiários", beneficiaries(d.beneficiaries), "beneficiaries", true);
    case "PREVIDENCIA":
      return f("Contribuição mensal", brl(d.monthlyContribution), "monthly-contribution") +
        f("Total acumulado", brl(d.accumulatedBalance), "accumulated-balance") +
        f("Tipo do plano", esc(d.planType ?? "—"), "plan-type") +
        f("Beneficiários", beneficiaries(d.beneficiaries), "beneficiaries", true);
    case "AUTO":
      return f("Placa", esc(d.plate ?? "—"), "plate") +
        f("Franquia", brl(d.deductible), "deductible") +
        f("Condutores", people(d.drivers, (x) => esc(x.name)), "drivers", true);
    case "VIAGEM":
      return f("Destino", esc(d.destination ?? "—"), "destination") +
        f("Dias de viagem", d.tripDays != null ? plural(d.tripDays, "dia", "dias") : "—", "trip-days") +
        f("Viajantes", people(d.travelers, (x) => esc(x.name)), "travelers", true);
    case "RESIDENCIAL": {
      const a = d.address || {};
      const cep = a.zipCode ? `${a.zipCode.slice(0, 5)}-${a.zipCode.slice(5)}` : "";
      const addr = a.street ? `${esc(a.street)}, ${esc(a.number)}${a.complement ? `, ${esc(a.complement)}` : ""}<br>${esc(a.district)}, ${esc(a.city)}/${esc(a.state)}, CEP ${cep}` : "—";
      return f("Endereço", addr, "address", true) + f("Valor total da cobertura", brl(d.coverageAmount), "coverage-amount");
    }
    default:
      return "";
  }
}

function policyCard(item, line, idx) {
  const id = `policy-${idx}`;
  return `
    <article class="policy" data-open="false" data-testid="policy-card" data-source="${item.source}" data-policy-number="${esc(item.policyNumber)}">
      <button class="policy-toggle" aria-expanded="false" aria-controls="${id}" data-testid="policy-toggle">
        ${institutionLogo(item.source)}
        <span class="policy-main">
          <span class="policy-institution" data-testid="policy-institution">${esc(item.institution)}</span><br>
          <span class="policy-plan" data-testid="policy-plan">${esc(item.planName ?? "Plano não informado")}</span>
        </span>
        <span class="policy-number" data-testid="policy-number">Apólice ${esc(item.policyNumber)}</span>
        ${icon("ico-chevron", "chev")}
      </button>
      <div class="policy-body" id="${id}" role="region">
        <div>
          <dl class="facts" data-testid="policy-details">
            <div><dt>Vigência</dt><dd data-testid="detail-validity">${date(item.startDate)} a ${date(item.endDate)}</dd></div>
            <div><dt>Segurado</dt><dd data-testid="detail-insured-name">${esc(item.insuredName ?? "—")}</dd></div>
            ${detailFacts(line, item.details)}
          </dl>
        </div>
      </div>
    </article>`;
}

async function renderProduct(slug) {
  const line = BY_SLUG[slug];
  if (!line) return renderNotFound();
  const meta = PRODUCTS[line];
  const first = await api(`/portfolio/${line}`);
  const holding = first.items.some((i) => i.source === "INSUREHUB") && first.items.some((i) => i.source === "OPIN") ? "INSUREHUB_OPIN"
    : first.items.some((i) => i.source === "INSUREHUB") ? "INSUREHUB"
    : first.items.length ? "OPIN" : "NONE";

  view().innerHTML = `
    <a class="back" href="#/inicio" data-testid="back-home">${icon("ico-back")}Início</a>
    <header class="product-head">
      <span class="glyph">${icon(meta.icon)}</span>
      <h1 data-testid="product-title">${esc(meta.label)}</h1>
      ${seals(holding)}
    </header>
    ${first.total ? `
      <div class="search">
        ${icon("ico-search")}
        <input type="search" id="product-search" data-testid="product-search" maxlength="60"
               placeholder="Buscar por plano, seguradora ou número da apólice" aria-label="Buscar apólices">
      </div>
      <p class="result-count" id="result-count" data-testid="result-count" aria-live="polite"></p>` : ""}
    <div class="policies" id="policies" data-testid="policy-list"></div>`;

  const paint = (data, q) => {
    const list = $("#policies");
    if (!data.total) {
      list.innerHTML = `
        <div class="empty" data-testid="product-empty">
          <p>Você ainda não tem ${esc(meta.label)}, nem na InsureHub nem em outra seguradora.</p>
          <a class="btn" href="#/contratar?produto=${line}" data-testid="product-empty-cta">Cotar ${esc(meta.label)}</a>
        </div>`;
      return;
    }
    $("#result-count").textContent = q
      ? `${plural(data.items.length, "apólice encontrada", "apólices encontradas")} de ${data.total}`
      : plural(data.total, "apólice", "apólices");
    list.innerHTML = data.items.length
      ? data.items.map((it, i) => policyCard(it, line, i)).join("")
      : `<div class="empty" data-testid="search-empty">Nenhuma apólice encontrada para “${esc(q)}”. Busque pelo nome do plano, pela seguradora ou pelo número da apólice.</div>`;
    $$(".policy-toggle", list).forEach((btn) =>
      btn.addEventListener("click", () => {
        const card = btn.closest(".policy");
        const open = card.dataset.open !== "true";
        card.dataset.open = String(open);
        btn.setAttribute("aria-expanded", String(open));
      })
    );
  };

  paint(first, "");
  const input = $("#product-search");
  if (input) {
    let timer, seq = 0;
    input.addEventListener("input", () => {
      clearTimeout(timer);
      timer = setTimeout(async () => {
        const q = input.value.trim();
        const mine = ++seq;
        try {
          const data = await api(`/portfolio/${line}${q ? `?q=${encodeURIComponent(q)}` : ""}`);
          if (mine === seq) paint(data, q);
        } catch (err) {
          toast(err.message, true);
        }
      }, 250);
    });
  }
}

/* =========================================================
   Contratar (cotação → dados → apólice)
   ========================================================= */
function stepsBar(step) {
  const names = ["Produto e plano", "Preço", "Seus dados"];
  return `<ol class="steps" data-testid="contract-steps">${names.map((n, i) => {
    const s = i + 1;
    return `<li ${s === step ? 'aria-current="step"' : ""} class="${s < step ? "done" : ""}"><span class="n">${s}</span>${n}</li>`;
  }).join("")}</ol>`;
}

async function renderContract(params) {
  const c = state.contract || (state.contract = { step: 1, line: params.get("produto") || "AUTO", plan: "ESSENCIAL", quote: null });
  if (params.get("produto") && c.step === 1) c.line = params.get("produto");
  if (c.step === 1) return contractStep1();
  if (c.step === 2) return contractStep2();
  return contractStep3();
}

async function contractStep1() {
  const c = state.contract;
  const quotes = await api("/quotes");
  const pending = quotes.filter((q) => q.status === "PENDING");
  view().innerHTML = `
    <header class="page-head"><div><h1>Contratar</h1><p>Escolha o produto e o plano para ver o preço na hora.</p></div></header>
    ${stepsBar(1)}
    <form id="quote-form" data-testid="quote-form" novalidate>
      <p class="section-title">Produto</p>
      <div class="choice-row" role="radiogroup">
        ${ORDER.map((k) => `
          <label class="choice"><input type="radio" name="line" value="${k}" ${c.line === k ? "checked" : ""} data-testid="quote-line-${k}">
            <span>${icon(PRODUCTS[k].icon)}<b>${PRODUCTS[k].label}</b></span></label>`).join("")}
      </div>
      <p class="section-title">Plano</p>
      <div class="choice-row plans" role="radiogroup">
        ${Object.entries(PLANS).map(([k, p]) => `
          <label class="choice"><input type="radio" name="plan" value="${k}" ${c.plan === k ? "checked" : ""} data-testid="quote-plan-${k}">
            <span><b>${p.label}</b><small>${p.note}</small></span></label>`).join("")}
      </div>
      <div class="form-grid" style="margin-top:24px" id="amount-fields"></div>
      <ul class="errors" id="quote-errors" data-testid="quote-errors"></ul>
      <div class="actions"><button class="btn" type="submit" data-testid="quote-submit">Ver preço</button></div>
    </form>
    ${pending.length ? `
      <section class="block">
        <h2>Cotações em aberto</h2>
        <div class="table-wrap"><table data-testid="quotes-table">
          <thead><tr><th>Plano</th><th class="num">Valor</th><th>Origem</th><th>Válida até</th><th></th></tr></thead>
          <tbody>${pending.map((q) => `
            <tr data-testid="quote-row" data-quote-id="${q.id}">
              <td>${esc(q.planName)}</td>
              <td class="num">${brl(q.monthlyPremium)}${q.line === "VIAGEM" ? "" : "/mês"}</td>
              <td>${q.origin === "PORTABILITY" ? "Portabilidade" : "Cotação"}${Number(q.discount) > 0 ? `, ${Math.round(q.discount * 100)}% de desconto` : ""}</td>
              <td>${date(q.expiresAt)}</td>
              <td class="num"><button class="btn btn-small btn-secondary" data-resume="${q.id}" data-testid="quote-resume">Continuar</button></td>
            </tr>`).join("")}</tbody>
        </table></div>
      </section>` : ""}`;

  const form = $("#quote-form");
  const paintAmount = () => {
    const line = form.line.value;
    const f = AMOUNT_FIELD[line];
    const name = line === "PREVIDENCIA" ? "monthlyContribution" : "insuredAmount";
    $("#amount-fields").innerHTML = `
      <label class="field"><span>${f.label}</span>
        <input name="${name}" type="number" step="0.01" min="0" inputmode="decimal" data-testid="quote-amount"><small>${f.hint}</small></label>
      ${line === "VIAGEM" ? `<label class="field"><span>Dias de viagem</span>
        <input name="tripDays" type="number" min="1" max="90" step="1" data-testid="quote-trip-days"><small>De 1 a 90 dias</small></label>` : ""}`;
  };
  paintAmount();
  $$('input[name="line"]', form).forEach((r) => r.addEventListener("change", paintAmount));

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const v = formValues(form);
    const body = { line: v.line, plan: v.plan, insuredAmount: numOrNull(v.insuredAmount), monthlyContribution: numOrNull(v.monthlyContribution), tripDays: numOrNull(v.tripDays) };
    try {
      const quote = await api("/quotes", { method: "POST", body });
      Object.assign(state.contract, { line: v.line, plan: v.plan, quote, step: 2 });
      contractStep2();
    } catch (err) {
      $("#quote-errors").innerHTML = errorList(err);
    }
  });

  $$("[data-resume]").forEach((b) =>
    b.addEventListener("click", () => {
      const q = pending.find((x) => String(x.id) === b.dataset.resume);
      Object.assign(state.contract, { line: q.line, plan: q.plan, quote: q, step: 3 });
      contractStep3();
    })
  );
}

function contractStep2() {
  const { quote: q } = state.contract;
  const price = q.line === "VIAGEM" ? `<strong data-testid="quote-price">${brl(q.annualPremium)}</strong><span>pagamento único</span>`
    : q.line === "PREVIDENCIA" ? `<strong data-testid="quote-price">${brl(q.monthlyPremium)}</strong><span>de contribuição por mês</span>`
    : `<strong data-testid="quote-price">${brl(q.monthlyPremium)}</strong><span>por mês, ${brl(q.annualPremium)} por ano</span>`;
  view().innerHTML = `
    <header class="page-head"><div><h1>${esc(q.planName)}</h1></div></header>
    ${stepsBar(2)}
    <section class="panel" data-testid="quote-result">
      <div class="price">${price}</div>
      <div class="quote-facts">
        ${q.insuredAmount != null ? `<span>${AMOUNT_FIELD[q.line].label.replace(" (R$)", "")}: <b>${brl(q.insuredAmount)}</b></span>` : ""}
        ${q.tripDays ? `<span>Viagem de <b>${plural(q.tripDays, "dia", "dias")}</b></span>` : ""}
        <span>Idade considerada: <b data-testid="quote-age">${q.customerAge} anos</b></span>
        <span>Preço válido até <b data-testid="quote-expires">${date(q.expiresAt)}</b></span>
      </div>
      <div class="actions">
        <button class="btn" data-testid="quote-continue" id="go-details">Continuar para os dados</button>
        <button class="btn btn-secondary" id="redo" data-testid="quote-redo">Mudar produto ou plano</button>
      </div>
    </section>`;
  $("#go-details").addEventListener("click", () => { state.contract.step = 3; contractStep3(); });
  $("#redo").addEventListener("click", () => { state.contract.step = 1; contractStep1(); });
}

const REPEAT = {
  drivers: { max: 3, cols: "cols-2", add: "Adicionar condutor", fields: [["name", "Nome do condutor", "text"], ["cpf", "CPF", "text"]] },
  beneficiaries: { max: 5, cols: "cols-3", add: "Adicionar beneficiário", fields: [["name", "Nome", "text"], ["relationship", "Parentesco", "text"], ["percentage", "%", "number"]] },
  travelers: { max: 6, cols: "cols-2", add: "Adicionar viajante", fields: [["name", "Nome do viajante", "text"], ["birthDate", "Nascimento", "date"]] },
};

function repeatItem(kind, values = {}) {
  const cfg = REPEAT[kind];
  return `
    <div class="repeat-item ${cfg.cols}" data-kind="${kind}" data-testid="${kind}-item">
      ${cfg.fields.map(([n, label, type]) => `
        <label class="field"><span>${label}</span>
          <input name="${n}" type="${type}" ${type === "number" ? 'min="1" max="100" step="1"' : ""} value="${esc(values[n] ?? "")}" data-testid="${kind}-${n}"></label>`).join("")}
      <button type="button" class="btn-quiet" data-remove data-testid="${kind}-remove" aria-label="Remover">Remover</button>
    </div>`;
}

function repeatBlock(kind, title, initial) {
  return `
    <p class="section-title">${title}</p>
    <div class="repeat" data-repeat="${kind}">
      <div class="repeat-list">${initial.map((v) => repeatItem(kind, v)).join("")}</div>
      <div class="repeat-foot">
        <button type="button" class="btn-quiet" data-add="${kind}" data-testid="${kind}-add">${REPEAT[kind].add}</button>
        ${kind === "beneficiaries" ? `<span class="sum" data-testid="beneficiaries-sum"></span>` : ""}
      </div>
    </div>`;
}

function wireRepeats(root) {
  const refresh = (box) => {
    const kind = box.dataset.repeat;
    const items = $$(".repeat-item", box);
    $(`[data-add="${kind}"]`, box).hidden = items.length >= REPEAT[kind].max;
    items.forEach((it) => ($("[data-remove]", it).hidden = items.length === 1));
    const sum = $(".sum", box);
    if (sum) {
      const total = items.reduce((acc, it) => acc + (Number($('[name="percentage"]', it).value) || 0), 0);
      sum.textContent = `Soma: ${total}%`;
      sum.className = `sum ${total === 100 ? "ok" : "bad"}`;
    }
  };
  $$("[data-repeat]", root).forEach((box) => {
    box.addEventListener("click", (e) => {
      if (e.target.matches("[data-add]")) {
        $(".repeat-list", box).insertAdjacentHTML("beforeend", repeatItem(box.dataset.repeat));
      } else if (e.target.matches("[data-remove]")) {
        e.target.closest(".repeat-item").remove();
      } else return;
      refresh(box);
    });
    box.addEventListener("input", () => refresh(box));
    refresh(box);
  });
}

function collectRepeat(root, kind) {
  return $$(`[data-repeat="${kind}"] .repeat-item`, root).map((it) => {
    const o = {};
    REPEAT[kind].fields.forEach(([n, , type]) => {
      const v = $(`[name="${n}"]`, it).value.trim();
      o[n] = type === "number" ? numOrNull(v) : v || null;
    });
    return o;
  });
}

async function contractStep3() {
  const { quote: q } = state.contract;
  const user = await me();
  const holder = { name: user.name, cpf: user.cpf, birthDate: user.birthDate };
  const isTrip = q.line === "VIAGEM";
  const specific = {
    AUTO: `
      <div class="form-grid"><label class="field"><span>Placa do veículo</span>
        <input name="plate" maxlength="8" placeholder="ABC1D23" data-testid="details-plate"><small>Padrão antigo ou Mercosul</small></label></div>
      ${repeatBlock("drivers", "Condutores", [holder])}`,
    VIDA: repeatBlock("beneficiaries", "Beneficiários", [{}]),
    PREVIDENCIA: `
      <p class="section-title">Tipo do plano</p>
      <div class="choice-row plans" role="radiogroup">
        <label class="choice"><input type="radio" name="planType" value="PGBL" data-testid="details-plan-type-PGBL"><span><b>PGBL</b><small>Para quem declara IR no modelo completo. Abate até 12% da renda tributável.</small></span></label>
        <label class="choice"><input type="radio" name="planType" value="VGBL" checked data-testid="details-plan-type-VGBL"><span><b>VGBL</b><small>Para quem declara no modelo simplificado. IR só sobre o rendimento.</small></span></label>
      </div>
      ${repeatBlock("beneficiaries", "Beneficiários", [{}])}`,
    VIAGEM: `
      <div class="form-grid"><label class="field wide"><span>Destino</span>
        <input name="destination" maxlength="80" placeholder="Cidade, país" data-testid="details-destination"></label></div>
      ${repeatBlock("travelers", "Viajantes", [holder])}`,
    RESIDENCIAL: `
      <p class="section-title">Endereço do imóvel</p>
      <div class="form-grid">
        <label class="field"><span>CEP</span><input name="zipCode" inputmode="numeric" maxlength="8" placeholder="Só números" data-testid="details-zip"></label>
        <label class="field"><span>Número</span><input name="number" maxlength="10" data-testid="details-number"></label>
        <label class="field wide"><span>Logradouro</span><input name="street" maxlength="120" data-testid="details-street"></label>
        <label class="field"><span>Complemento <small>(opcional)</small></span><input name="complement" maxlength="60" data-testid="details-complement"></label>
        <label class="field"><span>Bairro</span><input name="district" maxlength="80" data-testid="details-district"></label>
        <label class="field"><span>Cidade</span><input name="city" maxlength="80" data-testid="details-city"></label>
        <label class="field"><span>UF</span><select name="state" data-testid="details-state"><option value="">Selecione</option>${UFS.map((u) => `<option>${u}</option>`).join("")}</select></label>
      </div>`,
  }[q.line];

  view().innerHTML = `
    <header class="page-head"><div><h1>${esc(q.planName)}</h1><p>Falta pouco. Confira os dados que vão constar na apólice.</p></div></header>
    ${stepsBar(3)}
    <form id="details-form" class="panel" data-testid="details-form" novalidate>
      <div class="form-grid">
        <label class="field"><span>${isTrip ? "Data de embarque" : "Início da vigência"}</span>
          <input type="date" name="startDate" min="${todayIso()}" value="${todayIso()}" data-testid="issue-start-date">
          <small>${isTrip ? "Até 180 dias a partir de hoje" : "Até 30 dias a partir de hoje"}</small></label>
        <div class="field"><span>Segurado</span><p style="padding:10px 0;font-weight:600" data-testid="issue-insured-name">${esc(user.name)}</p></div>
      </div>
      ${specific}
      <ul class="errors" id="issue-errors" data-testid="issue-errors"></ul>
      <div class="actions">
        <button class="btn" type="submit" data-testid="issue-policy">Contratar ${esc(q.planName)}</button>
        <button class="btn btn-secondary" type="button" id="back-price" data-testid="issue-back">Voltar</button>
      </div>
    </form>`;

  const form = $("#details-form");
  wireRepeats(form);
  $("#back-price").addEventListener("click", () => { state.contract.step = 2; contractStep2(); });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const v = formValues(form);
    const details = {
      AUTO: () => ({ plate: v.plate, drivers: collectRepeat(form, "drivers") }),
      VIDA: () => ({ beneficiaries: collectRepeat(form, "beneficiaries") }),
      PREVIDENCIA: () => ({ planType: v.planType, beneficiaries: collectRepeat(form, "beneficiaries") }),
      VIAGEM: () => ({ destination: v.destination, travelers: collectRepeat(form, "travelers") }),
      RESIDENCIAL: () => ({ address: { zipCode: v.zipCode, street: v.street, number: v.number, complement: v.complement || null, district: v.district, city: v.city, state: v.state } }),
    }[q.line]();
    try {
      const p = await api("/policies", { method: "POST", body: { quoteId: q.id, startDate: v.startDate || null, details } });
      state.contract = null;
      toast(`Apólice ${p.number} contratada.`);
      location.hash = `#/produto/${PRODUCTS[p.line].slug}`;
    } catch (err) {
      $("#issue-errors").innerHTML = errorList(err);
      $("#issue-errors").scrollIntoView({ block: "nearest", behavior: reducedMotion() ? "auto" : "smooth" });
    }
  });
}

/* =========================================================
   Sinistros
   ========================================================= */
async function renderClaims() {
  const [policies, claims] = await Promise.all([api("/policies?status=ACTIVE"), api("/claims")]);
  const eligible = policies.filter((p) => PRODUCTS[p.line].claimTypes.length);
  view().innerHTML = `
    <header class="page-head"><div><h1>Sinistros</h1><p>Comunique um sinistro das suas apólices InsureHub e acompanhe a análise.</p></div></header>
    <section class="block">
      <h2>Abrir sinistro</h2>
      ${eligible.length ? `
      <form id="claim-form" class="panel" data-testid="claim-form" novalidate>
        <div class="form-grid">
          <label class="field"><span>Apólice</span><select name="policyId" data-testid="claim-policy">
            ${eligible.map((p) => `<option value="${p.id}" data-line="${p.line}">${esc(p.planName)}, ${esc(p.number)}</option>`).join("")}
          </select></label>
          <label class="field"><span>O que aconteceu</span><select name="type" data-testid="claim-type"></select></label>
          <label class="field"><span>Data da ocorrência</span><input name="occurrenceDate" type="date" max="${todayIso()}" data-testid="claim-date"></label>
          <label class="field"><span>Valor do prejuízo (R$)</span><input name="amount" type="number" step="0.01" data-testid="claim-amount"></label>
          <label class="field wide"><span>Descrição</span><textarea name="description" rows="3" data-testid="claim-description"></textarea><small>De 20 a 1000 caracteres.</small></label>
        </div>
        <ul class="errors" id="claim-errors" data-testid="claim-errors"></ul>
        <div class="actions"><button class="btn" type="submit" data-testid="claim-submit">Abrir sinistro</button></div>
      </form>` : `<div class="empty" data-testid="claim-no-policy">Você não tem apólice InsureHub ativa que aceite sinistro. Previdência não tem sinistro.</div>`}
    </section>
    <section class="block">
      <h2>Meus sinistros</h2>
      ${claims.length ? `<div class="table-wrap"><table data-testid="claims-table">
        <thead><tr><th>Protocolo</th><th>Tipo</th><th>Ocorrência</th><th class="num">Valor</th><th class="num">Aprovado</th><th>Status</th><th></th></tr></thead>
        <tbody>${claims.map((c) => `
          <tr data-testid="claim-row" data-claim-id="${c.id}">
            <td data-testid="claim-protocol">${esc(c.protocol)}</td><td>${esc(c.type)}</td><td>${date(c.occurrenceDate)}</td>
            <td class="num">${brl(c.amount)}</td><td class="num">${brl(c.approvedAmount)}</td><td>${statusTag(c.status)}</td>
            <td class="num">${c.status === "OPEN" ? `<button class="btn btn-small btn-secondary" data-cancel="${c.id}" data-testid="claim-cancel">Cancelar</button>` : ""}</td>
          </tr>`).join("")}</tbody></table></div>` : `<div class="empty">Nenhum sinistro aberto.</div>`}
    </section>`;

  const form = $("#claim-form");
  if (form) {
    const fillTypes = () => {
      const line = form.policyId.selectedOptions[0]?.dataset.line;
      form.type.innerHTML = (PRODUCTS[line]?.claimTypes || []).map((t) => `<option>${t}</option>`).join("");
    };
    form.policyId.addEventListener("change", fillTypes);
    fillTypes();
    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      const v = formValues(form);
      try {
        const c = await api("/claims", { method: "POST", body: { policyId: numOrNull(v.policyId), type: v.type, occurrenceDate: v.occurrenceDate || null, amount: numOrNull(v.amount), description: v.description } });
        toast(`Sinistro ${c.protocol} aberto.`);
        renderClaims();
      } catch (err) {
        $("#claim-errors").innerHTML = errorList(err);
      }
    });
  }
  $$("[data-cancel]").forEach((b) =>
    b.addEventListener("click", async () => {
      try {
        await api(`/claims/${b.dataset.cancel}/cancel`, { method: "POST" });
        toast("Sinistro cancelado.");
        renderClaims();
      } catch (err) {
        toast(err.message, true);
      }
    })
  );
}

/* =========================================================
   Outras seguradoras (OPIN)
   ========================================================= */
async function renderSharing() {
  let consent = null;
  try {
    consent = await api("/opin/consents/active");
  } catch (err) {
    if (err.code !== "CONSENT_NOT_FOUND") throw err;
  }
  view().innerHTML = `
    <header class="page-head"><div><h1>Outras seguradoras</h1>
      <p>Com sua autorização, a InsureHub busca pelo Open Insurance os seguros que você tem em outras seguradoras. Você pode cancelar quando quiser.</p></div></header>
    <section class="panel" data-testid="consent-box">
      ${consent ? `
        <p data-testid="consent-status">Compartilhamento autorizado até <b>${date(consent.expiresAt)}</b>.</p>
        <div class="actions"><button class="btn btn-secondary" id="revoke" data-testid="consent-revoke">Cancelar compartilhamento</button></div>` : `
        <p>O compartilhamento vale por 12 meses. Ao cancelar, os dados trazidos de outras seguradoras são apagados.</p>
        <div class="actions"><button class="btn" id="grant" data-testid="consent-grant">Autorizar compartilhamento</button></div>`}
    </section>
    ${consent ? `<section class="block" id="external"></section>` : ""}`;

  $("#grant")?.addEventListener("click", async () => {
    try { await api("/opin/consents", { method: "POST" }); toast("Compartilhamento autorizado."); renderSharing(); }
    catch (err) { toast(err.message, true); }
  });
  $("#revoke")?.addEventListener("click", async () => {
    try { await api("/opin/consents/active", { method: "DELETE" }); toast("Compartilhamento cancelado e dados apagados."); renderSharing(); }
    catch (err) { toast(err.message, true); }
  });
  if (consent) loadExternal(false);
}

async function loadExternal(refresh) {
  const box = $("#external");
  try {
    const r = await api(`/opin/external-policies${refresh ? "?refresh=true" : ""}`);
    const note = r.stale ? `As outras seguradoras não responderam agora. Dados salvos em ${dateTime(r.syncedAt)}.` : `Atualizado em ${dateTime(r.syncedAt)}.`;
    box.innerHTML = `
      <div class="page-head" style="margin-bottom:16px"><div><h2>Seguros em outras seguradoras</h2>
        <p data-testid="external-source" data-source="${r.source}">${note}</p></div>
        <button class="btn btn-secondary btn-small" id="refresh" data-testid="external-refresh">Atualizar agora</button></div>
      ${r.policies.length ? `<div class="table-wrap"><table data-testid="external-table">
        <thead><tr><th>Seguradora</th><th>Produto</th><th>Plano</th><th>Vencimento</th><th></th></tr></thead>
        <tbody>${r.policies.map((p) => `
          <tr data-testid="external-row" data-external-id="${esc(p.externalId)}">
            <td>${esc(p.insurer)}</td><td>${esc(PRODUCTS[p.line]?.label ?? p.line)}</td><td>${esc(p.planName ?? "—")}</td><td>${date(p.endDate)}</td>
            <td class="num">${p.line !== "VIAGEM" ? `<button class="btn btn-small btn-secondary" data-port="${esc(p.externalId)}" data-testid="portability-request">Trazer para a InsureHub</button>` : ""}</td>
          </tr>`).join("")}</tbody></table></div>` : `<div class="empty">Nenhum seguro encontrado em outras seguradoras.</div>`}`;
    $("#refresh").addEventListener("click", () => loadExternal(true));
    $$("[data-port]", box).forEach((b) =>
      b.addEventListener("click", async () => {
        try {
          const q = await api("/portability", { method: "POST", body: { externalPolicyId: b.dataset.port } });
          toast(`Cotação de portabilidade criada: ${q.planName}, ${brl(q.monthlyPremium)} por mês. Continue em Contratar.`);
        } catch (err) {
          toast(err.message, true);
        }
      })
    );
  } catch (err) {
    box.innerHTML = `<div class="empty" data-testid="external-error">${esc(err.message)}</div>`;
  }
}

/* =========================================================
   Analista
   ========================================================= */
async function renderAdmin() {
  const status = sessionStorage.getItem("ih_admin_filter") || "";
  const list = await api(`/admin/claims${status ? `?status=${status}` : ""}`);
  const actions = (c) => ({
    OPEN: `<button class="btn btn-small" data-act="start-analysis" data-id="${c.id}" data-testid="admin-start">Iniciar análise</button>`,
    IN_ANALYSIS: `<button class="btn btn-small" data-act="approve" data-id="${c.id}" data-amount="${c.amount}" data-testid="admin-approve">Aprovar</button><button class="btn btn-small btn-secondary" data-act="reject" data-id="${c.id}" data-testid="admin-reject">Recusar</button>`,
    APPROVED: `<button class="btn btn-small" data-act="pay" data-id="${c.id}" data-testid="admin-pay">Pagar</button>`,
  }[c.status] || "");
  view().innerHTML = `
    <header class="page-head"><div><h1>Fila de sinistros</h1></div>
      <label class="field" style="min-width:200px"><span>Status</span>
        <select id="admin-status" data-testid="admin-status-filter">
          ${["", "OPEN", "IN_ANALYSIS", "APPROVED", "REJECTED", "PAID", "CANCELLED"].map((s) => `<option value="${s}" ${s === status ? "selected" : ""}>${s || "Todos"}</option>`).join("")}
        </select></label></header>
    ${list.length ? `<div class="table-wrap"><table data-testid="admin-claims-table">
      <thead><tr><th>Protocolo</th><th>Apólice</th><th>Tipo</th><th class="num">Valor</th><th class="num">Aprovado</th><th>Status</th><th class="num">Ações</th></tr></thead>
      <tbody>${list.map((c) => `
        <tr data-testid="admin-claim-row" data-claim-id="${c.id}">
          <td>${esc(c.protocol)}</td><td>${c.policyId}</td><td>${esc(c.type)}</td><td class="num">${brl(c.amount)}</td>
          <td class="num">${brl(c.approvedAmount)}</td><td>${statusTag(c.status)}</td><td class="num">${actions(c)}</td>
        </tr>`).join("")}</tbody></table></div>` : `<div class="empty">Nenhum sinistro ${status ? "com esse status" : "na fila"}.</div>`}`;

  $("#admin-status").addEventListener("change", (e) => { sessionStorage.setItem("ih_admin_filter", e.target.value); renderAdmin(); });
  $$("[data-act]").forEach((b) =>
    b.addEventListener("click", async () => {
      const { act, id } = b.dataset;
      let body;
      if (act === "approve") {
        const v = prompt("Valor aprovado (R$):", b.dataset.amount);
        if (v === null) return;
        body = { approvedAmount: Number(v) };
      } else if (act === "reject") {
        const reason = prompt("Motivo da recusa:");
        if (reason === null) return;
        body = { reason };
      }
      try {
        await api(`/admin/claims/${id}/${act}`, { method: "POST", body });
        toast("Sinistro atualizado.");
        renderAdmin();
      } catch (err) {
        toast(err.message, true);
      }
    })
  );
}

/* =========================================================
   Roteamento
   ========================================================= */
function renderNotFound() {
  view().innerHTML = `<div class="empty" data-testid="not-found">Página não encontrada. <a href="#/inicio">Voltar ao início</a>.</div>`;
}

async function route() {
  const logged = Boolean(state.token);
  $("#session").hidden = !logged;
  $("#mainnav").hidden = !logged || state.role !== "CUSTOMER";
  if (!logged) return renderAuth();
  $("#session-user").textContent = state.name || "";

  const [path, query = ""] = location.hash.replace(/^#\/?/, "").split("?");
  const params = new URLSearchParams(query);
  const [page, arg] = path.split("/");
  const isAdmin = state.role === "ADMIN";
  const target = isAdmin ? "analista" : page || "inicio";

  $$("#mainnav a").forEach((a) => a.toggleAttribute("aria-current", a.dataset.nav === target || (target === "produto" && a.dataset.nav === "inicio")));
  $$("#mainnav a[aria-current]").forEach((a) => a.setAttribute("aria-current", "page"));
  if (target !== "contratar") state.contract = null;

  const pages = isAdmin
    ? { analista: renderAdmin }
    : { inicio: renderHome, produto: () => renderProduct(arg), contratar: () => renderContract(params), sinistros: renderClaims, compartilhamento: renderSharing };
  try {
    await (pages[target] || renderNotFound)();
  } catch (err) {
    view().innerHTML = `<div class="empty" data-testid="page-error">${esc(err.message || "Não foi possível carregar esta página.")} <a href="#/inicio">Tentar de novo</a>.</div>`;
  }
}

$("#logout-button").addEventListener("click", logout);
window.addEventListener("hashchange", () => { route(); view().focus({ preventScroll: true }); window.scrollTo(0, 0); });
route();
