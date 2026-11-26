-- Previdência e Viagem passam a ser vendidos; cotações ganham plano; apólices ganham dados do produto.

-- ---------- quotes ----------
ALTER TABLE quotes DROP CONSTRAINT quotes_line_check;
ALTER TABLE quotes ADD CONSTRAINT quotes_line_check
    CHECK (line IN ('AUTO', 'RESIDENCIAL', 'VIDA', 'PREVIDENCIA', 'VIAGEM'));

ALTER TABLE quotes ALTER COLUMN insured_amount DROP NOT NULL;
ALTER TABLE quotes ADD COLUMN plan VARCHAR(20) NOT NULL DEFAULT 'ESSENCIAL'
    CHECK (plan IN ('ESSENCIAL', 'COMPLETO', 'PREMIUM'));
ALTER TABLE quotes ALTER COLUMN plan DROP DEFAULT;
ALTER TABLE quotes ADD COLUMN plan_multiplier NUMERIC(4,2) NOT NULL DEFAULT 1.00;
ALTER TABLE quotes ADD COLUMN monthly_contribution NUMERIC(14,2) CHECK (monthly_contribution > 0);
ALTER TABLE quotes ADD COLUMN trip_days INT CHECK (trip_days BETWEEN 1 AND 90);

-- Previdência é cotada por contribuição; os demais produtos, por importância segurada.
ALTER TABLE quotes ADD CONSTRAINT ck_quotes_amount_by_line CHECK (
    (line = 'PREVIDENCIA' AND monthly_contribution IS NOT NULL AND insured_amount IS NULL)
    OR (line <> 'PREVIDENCIA' AND insured_amount IS NOT NULL AND monthly_contribution IS NULL)
);
ALTER TABLE quotes ADD CONSTRAINT ck_quotes_trip_days CHECK (
    (line = 'VIAGEM' AND trip_days IS NOT NULL) OR (line <> 'VIAGEM' AND trip_days IS NULL)
);

-- ---------- policies ----------
ALTER TABLE policies DROP CONSTRAINT policies_line_check;
ALTER TABLE policies ADD CONSTRAINT policies_line_check
    CHECK (line IN ('AUTO', 'RESIDENCIAL', 'VIDA', 'PREVIDENCIA', 'VIAGEM'));

ALTER TABLE policies ALTER COLUMN insured_amount DROP NOT NULL;
ALTER TABLE policies ADD COLUMN plan VARCHAR(20) NOT NULL DEFAULT 'ESSENCIAL'
    CHECK (plan IN ('ESSENCIAL', 'COMPLETO', 'PREMIUM'));
ALTER TABLE policies ALTER COLUMN plan DROP DEFAULT;
ALTER TABLE policies ADD COLUMN monthly_contribution NUMERIC(14,2);
ALTER TABLE policies ADD COLUMN insured_name VARCHAR(120);
UPDATE policies p SET insured_name = u.name FROM users u WHERE u.id = p.customer_id;
ALTER TABLE policies ALTER COLUMN insured_name SET NOT NULL;
ALTER TABLE policies ADD COLUMN details JSONB NOT NULL DEFAULT '{}'::jsonb;

-- Viagem pode ter vigência de 1 dia (início = fim).
ALTER TABLE policies DROP CONSTRAINT ck_policies_dates;
ALTER TABLE policies ADD CONSTRAINT ck_policies_dates CHECK (end_date >= start_date);

-- ---------- external_policies_cache ----------
ALTER TABLE external_policies_cache ADD COLUMN plan_name VARCHAR(120);
ALTER TABLE external_policies_cache ADD COLUMN insured_name VARCHAR(120);
ALTER TABLE external_policies_cache ADD COLUMN details JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE external_policies_cache ALTER COLUMN insured_amount DROP NOT NULL;
