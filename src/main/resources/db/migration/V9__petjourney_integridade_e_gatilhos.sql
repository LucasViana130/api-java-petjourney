-- ============================================================
-- PETJOURNEY - SPRINT 4 - V9: INTEGRIDADE, INDICES E GATILHOS
--
-- ATENCAO: renumere conforme flyway_schema_history (sugestao: V9).
--
-- ANTES DE APLICAR em uma base com dados: rode db/diagnostico.sql
-- em uma COPIA da base. Esta migracao nao exclui nem corrige dados;
-- se existir valor nulo, duplicado ou invalido, ela ABORTA com a
-- indicacao da tabela/coluna e nada e alterado (transacao unica).
--
-- A migracao e idempotente: restricao ou indice que ja exista
-- (pelo nome, ou unico equivalente nas mesmas colunas) e preservado.
-- ============================================================

-- ------------------------------------------------------------
-- Funcoes auxiliares temporarias (existem so durante esta execucao)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION pg_temp.ensure_not_null(p_table text, p_column text)
RETURNS void LANGUAGE plpgsql AS $f$
DECLARE
    v_nulos bigint;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_attribute
                WHERE attrelid = ('public.' || p_table)::regclass
                  AND attname = p_column AND attnotnull) THEN
        RETURN;
    END IF;

    EXECUTE format('SELECT count(*) FROM public.%I WHERE %I IS NULL', p_table, p_column) INTO v_nulos;
    IF v_nulos > 0 THEN
        RAISE EXCEPTION 'public.%.% possui % linha(s) nula(s). Rode db/diagnostico.sql e corrija os dados antes de aplicar a V9.',
            p_table, p_column, v_nulos;
    END IF;

    EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I SET NOT NULL', p_table, p_column);
END
$f$;

CREATE OR REPLACE FUNCTION pg_temp.ensure_check(p_table text, p_name text, p_expr text)
RETURNS void LANGUAGE plpgsql AS $f$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
                WHERE conrelid = ('public.' || p_table)::regclass AND conname = p_name) THEN
        RETURN;
    END IF;
    EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I CHECK (%s)', p_table, p_name, p_expr);
END
$f$;

-- Cria indice unico, a menos que ja exista um com o mesmo nome ou, sem
-- predicado, um indice unico nas mesmas colunas (ex.: UNIQUE do Java).
CREATE OR REPLACE FUNCTION pg_temp.ensure_unique(p_table text, p_name text, p_cols text[], p_where text DEFAULT NULL)
RETURNS void LANGUAGE plpgsql AS $f$
DECLARE
    v_rel regclass := ('public.' || p_table)::regclass;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public' AND c.relname = p_name) THEN
        RETURN;
    END IF;

    IF p_where IS NULL AND EXISTS (
        SELECT 1
          FROM pg_index i
         WHERE i.indrelid = v_rel AND i.indisunique AND i.indpred IS NULL AND i.indexprs IS NULL
           AND i.indnatts = array_length(p_cols, 1)
           AND (SELECT array_agg(a.attname::text ORDER BY k.ord)
                  FROM unnest(i.indkey::int2[]) WITH ORDINALITY AS k(attnum, ord)
                  JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = k.attnum) = p_cols
    ) THEN
        RETURN;
    END IF;

    EXECUTE format('CREATE UNIQUE INDEX %I ON public.%I (%s)%s',
                   p_name, p_table,
                   (SELECT string_agg(quote_ident(c), ', ') FROM unnest(p_cols) AS c),
                   CASE WHEN p_where IS NULL THEN '' ELSE ' WHERE ' || p_where END);
END
$f$;

-- ------------------------------------------------------------
-- 1) Obrigatoriedade (apos saneamento verificado)
-- ------------------------------------------------------------
SELECT pg_temp.ensure_not_null('tb_clinica', 'cnpj');
SELECT pg_temp.ensure_not_null('tb_tutor', 'clinic_id');
SELECT pg_temp.ensure_not_null('tb_veterinario', 'clinic_id');
SELECT pg_temp.ensure_not_null('tb_pet', 'tutor_id');
SELECT pg_temp.ensure_not_null('tb_pet', 'species');
SELECT pg_temp.ensure_not_null('tb_agendamento', 'clinic_id');
SELECT pg_temp.ensure_not_null('tb_agendamento', 'pet_id');
SELECT pg_temp.ensure_not_null('tb_agendamento', 'veterinarian_id');
SELECT pg_temp.ensure_not_null('tb_agendamento', 'status');
SELECT pg_temp.ensure_not_null('tb_prontuario', 'pet_id');
SELECT pg_temp.ensure_not_null('tb_prontuario', 'veterinarian_id');
SELECT pg_temp.ensure_not_null('tb_prontuario', 'main_complaint');
SELECT pg_temp.ensure_not_null('tb_medicamento', 'pet_id');
SELECT pg_temp.ensure_not_null('tb_medicamento', 'veterinarian_id');

-- ------------------------------------------------------------
-- 2) CHECKs de valores
-- ------------------------------------------------------------
SELECT pg_temp.ensure_check('tb_pet', 'chk_pet_species',
    $c$species IN ('CACHORRO','GATO','AVE','ROEDOR','REPTIL','OUTRO')$c$);
SELECT pg_temp.ensure_check('tb_pet', 'chk_pet_sex',
    $c$sex IS NULL OR sex IN ('MACHO','FEMEA')$c$);
SELECT pg_temp.ensure_check('tb_pet', 'chk_pet_weight',
    $c$weight IS NULL OR weight > 0$c$);
SELECT pg_temp.ensure_check('tb_agendamento', 'chk_agendamento_status',
    $c$status IN ('PENDENTE','REALIZADO','CANCELADO')$c$);
SELECT pg_temp.ensure_check('tb_medicamento', 'chk_medicamento_status',
    $c$status IS NULL OR status IN ('ATIVO','FINALIZADO','CANCELADO')$c$);
SELECT pg_temp.ensure_check('tb_medicamento', 'chk_medicamento_periodo',
    $c$end_date IS NULL OR start_date IS NULL OR end_date >= start_date$c$);
SELECT pg_temp.ensure_check('tb_disponibilidade_veterinario', 'chk_disponibilidade_periodo',
    $c$end_time > start_time$c$);
SELECT pg_temp.ensure_check('tb_usuario', 'chk_usuario_role',
    $c$role IN ('ADMIN_SISTEMA','ADMIN_CLINICA','VETERINARIO','TUTOR')$c$);
-- Perfil x vinculo: cada perfil tem exatamente o vinculo correspondente.
SELECT pg_temp.ensure_check('tb_usuario', 'chk_usuario_role_vinculo', $c$
       (role = 'ADMIN_SISTEMA' AND clinic_id IS NULL     AND tutor_id IS NULL     AND veterinarian_id IS NULL)
    OR (role = 'ADMIN_CLINICA' AND clinic_id IS NOT NULL AND tutor_id IS NULL     AND veterinarian_id IS NULL)
    OR (role = 'TUTOR'         AND clinic_id IS NULL     AND tutor_id IS NOT NULL AND veterinarian_id IS NULL)
    OR (role = 'VETERINARIO'   AND clinic_id IS NULL     AND tutor_id IS NULL     AND veterinarian_id IS NOT NULL)
$c$);

-- ------------------------------------------------------------
-- 3) Unicidade
-- ------------------------------------------------------------
SELECT pg_temp.ensure_unique('tb_tutor', 'uk_tb_tutor_cpf', ARRAY['cpf']);
SELECT pg_temp.ensure_unique('tb_usuario', 'uk_tb_usuario_username', ARRAY['username']);
SELECT pg_temp.ensure_unique('tb_usuario', 'uk_tb_usuario_tutor_id', ARRAY['tutor_id']);
SELECT pg_temp.ensure_unique('tb_usuario', 'uk_tb_usuario_veterinarian_id', ARRAY['veterinarian_id']);
-- Novos (coerentes com a entrega Oracle): CRMV unico e um ADMIN_CLINICA por clinica.
SELECT pg_temp.ensure_unique('tb_veterinario', 'uk_tb_veterinario_crmv', ARRAY['crmv']);
SELECT pg_temp.ensure_unique('tb_usuario', 'uk_tb_usuario_clinic_id', ARRAY['clinic_id']);
SELECT pg_temp.ensure_unique('tb_prontuario', 'uk_tb_prontuario_appointment_id', ARRAY['appointment_id']);
SELECT pg_temp.ensure_unique('tb_disponibilidade_veterinario', 'uk_tb_disponibilidade_vet_inicio',
                             ARRAY['veterinarian_id', 'start_time']);
-- Agenda concorrente: dois agendamentos nao cancelados no mesmo horario do
-- veterinario sao impossiveis. E a garantia final de concorrencia.
SELECT pg_temp.ensure_unique('tb_agendamento', 'uk_tb_agendamento_vet_horario_ativo',
                             ARRAY['veterinarian_id', 'date_time'], $c$status <> 'CANCELADO'$c$);

-- ------------------------------------------------------------
-- 4) Indices de consulta (validar com EXPLAIN em volume real;
--    se o Java ja tiver indice equivalente com outro nome, remova o duplicado)
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_tb_tutor_clinic           ON public.tb_tutor (clinic_id);
CREATE INDEX IF NOT EXISTS idx_tb_pet_tutor              ON public.tb_pet (tutor_id);
CREATE INDEX IF NOT EXISTS idx_tb_veterinario_clinic     ON public.tb_veterinario (clinic_id);
CREATE INDEX IF NOT EXISTS idx_tb_medicamento_medical_record ON public.tb_medicamento (medical_record_id);
CREATE INDEX IF NOT EXISTS idx_tb_prontuario_pet_data    ON public.tb_prontuario (pet_id, registration_date DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_tb_agendamento_pet_data   ON public.tb_agendamento (pet_id, date_time);
CREATE INDEX IF NOT EXISTS idx_tb_agendamento_clinic_data ON public.tb_agendamento (clinic_id, date_time);

-- ------------------------------------------------------------
-- 5) Funcoes de gatilho (no PostgreSQL o gatilho chama uma funcao
--    RETURNS trigger; a logica fica no schema pkg_petjourney)
-- ------------------------------------------------------------

-- 5.1 Isolamento por clinica no agendamento (INSERT e UPDATE).
CREATE OR REPLACE FUNCTION pkg_petjourney.trg_valida_clinica_agendamento()
RETURNS trigger LANGUAGE plpgsql AS $f$
DECLARE
    v_clinic_pet bigint;
    v_clinic_vet bigint;
BEGIN
    SELECT t.clinic_id INTO v_clinic_pet
      FROM public.tb_pet p
      JOIN public.tb_tutor t ON t.id = p.tutor_id
     WHERE p.id = NEW.pet_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ062';
    END IF;

    SELECT clinic_id INTO v_clinic_vet FROM public.tb_veterinario WHERE id = NEW.veterinarian_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ062';
    END IF;

    IF v_clinic_pet IS DISTINCT FROM NEW.clinic_id THEN
        RAISE EXCEPTION 'O pet informado nao pertence a clinica do agendamento.' USING ERRCODE = 'PJ060';
    END IF;

    IF v_clinic_vet IS DISTINCT FROM NEW.clinic_id THEN
        RAISE EXCEPTION 'O veterinario informado nao pertence a clinica do agendamento.' USING ERRCODE = 'PJ061';
    END IF;

    RETURN NEW;
END
$f$;

-- 5.2 Prontuario: pet e veterinario da mesma clinica; com agendamento,
--     pet e veterinario iguais aos do agendamento.
CREATE OR REPLACE FUNCTION pkg_petjourney.trg_valida_vinculos_prontuario()
RETURNS trigger LANGUAGE plpgsql AS $f$
DECLARE
    v_clinic_pet bigint;
    v_clinic_vet bigint;
    v_ag_pet     bigint;
    v_ag_vet     bigint;
BEGIN
    SELECT t.clinic_id INTO v_clinic_pet
      FROM public.tb_pet p JOIN public.tb_tutor t ON t.id = p.tutor_id WHERE p.id = NEW.pet_id;
    SELECT clinic_id INTO v_clinic_vet FROM public.tb_veterinario WHERE id = NEW.veterinarian_id;

    IF v_clinic_pet IS NOT NULL AND v_clinic_vet IS NOT NULL AND v_clinic_pet <> v_clinic_vet THEN
        RAISE EXCEPTION 'Pet e veterinario do prontuario pertencem a clinicas diferentes.' USING ERRCODE = 'PJ063';
    END IF;

    IF NEW.appointment_id IS NOT NULL THEN
        SELECT pet_id, veterinarian_id INTO v_ag_pet, v_ag_vet
          FROM public.tb_agendamento WHERE id = NEW.appointment_id;
        IF FOUND AND (v_ag_pet <> NEW.pet_id OR v_ag_vet <> NEW.veterinarian_id) THEN
            RAISE EXCEPTION 'Pet ou veterinario do prontuario diferem do agendamento informado.' USING ERRCODE = 'PJ064';
        END IF;
    END IF;

    RETURN NEW;
END
$f$;

-- 5.3 Medicamento: pet e veterinario da mesma clinica; com prontuario,
--     pet e veterinario iguais aos do prontuario.
CREATE OR REPLACE FUNCTION pkg_petjourney.trg_valida_vinculos_medicamento()
RETURNS trigger LANGUAGE plpgsql AS $f$
DECLARE
    v_clinic_pet bigint;
    v_clinic_vet bigint;
    v_pr_pet     bigint;
    v_pr_vet     bigint;
BEGIN
    SELECT t.clinic_id INTO v_clinic_pet
      FROM public.tb_pet p JOIN public.tb_tutor t ON t.id = p.tutor_id WHERE p.id = NEW.pet_id;
    SELECT clinic_id INTO v_clinic_vet FROM public.tb_veterinario WHERE id = NEW.veterinarian_id;

    IF v_clinic_pet IS NOT NULL AND v_clinic_vet IS NOT NULL AND v_clinic_pet <> v_clinic_vet THEN
        RAISE EXCEPTION 'Pet e veterinario do medicamento pertencem a clinicas diferentes.' USING ERRCODE = 'PJ065';
    END IF;

    IF NEW.medical_record_id IS NOT NULL THEN
        SELECT pet_id, veterinarian_id INTO v_pr_pet, v_pr_vet
          FROM public.tb_prontuario WHERE id = NEW.medical_record_id;
        IF FOUND AND (v_pr_pet <> NEW.pet_id OR v_pr_vet <> NEW.veterinarian_id) THEN
            RAISE EXCEPTION 'Pet ou veterinario do medicamento diferem do prontuario informado.' USING ERRCODE = 'PJ066';
        END IF;
    END IF;

    RETURN NEW;
END
$f$;

-- 5.4 Auditoria de agendamento, na mesma transacao da alteracao (um
--     ROLLBACK remove tambem a linha de auditoria). Registra o registro
--     completo (inclusive description) em INSERT, UPDATE e DELETE.
--     Os snapshots sao JSON TECNICO (json_build_object), sem ambiguidade para
--     texto com ';', '=' ou quebras de linha; null vira JSON null. Nao confundir
--     com a montagem manual de JSON da parte academica (fn_montar_json_pet).
--     O Java pode informar o usuario da aplicacao com:
--     SELECT set_config('petjourney.app_username', 'x', true);
--     e petjourney.correlation_id; a ausencia dos dois e tolerada.
CREATE OR REPLACE FUNCTION pkg_petjourney.trg_auditoria_agendamento()
RETURNS trigger LANGUAGE plpgsql AS $f$
DECLARE
    v_old text;
    v_new text;
BEGIN
    IF TG_OP = 'UPDATE' AND OLD IS NOT DISTINCT FROM NEW THEN
        RETURN NULL;
    END IF;

    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_old := json_build_object(
                    'id', OLD.id, 'clinic_id', OLD.clinic_id, 'pet_id', OLD.pet_id,
                    'veterinarian_id', OLD.veterinarian_id, 'title', OLD.title,
                    'description', OLD.description,
                    'date_time', to_char(OLD.date_time, 'YYYY-MM-DD"T"HH24:MI:SS'),
                    'status', OLD.status)::text;
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        v_new := json_build_object(
                    'id', NEW.id, 'clinic_id', NEW.clinic_id, 'pet_id', NEW.pet_id,
                    'veterinarian_id', NEW.veterinarian_id, 'title', NEW.title,
                    'description', NEW.description,
                    'date_time', to_char(NEW.date_time, 'YYYY-MM-DD"T"HH24:MI:SS'),
                    'status', NEW.status)::text;
    END IF;

    INSERT INTO public.tb_auditoria
        (db_username, operation, occurred_at, appointment_id, old_values, new_values, app_username, correlation_id)
    VALUES
        (current_user::text, TG_OP, clock_timestamp(), COALESCE(NEW.id, OLD.id), v_old, v_new,
         NULLIF(current_setting('petjourney.app_username', true), ''),
         NULLIF(current_setting('petjourney.correlation_id', true), ''));

    RETURN NULL;
END
$f$;

-- ------------------------------------------------------------
-- 6) Gatilhos
-- ------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_tb_agendamento_valida_clinica ON public.tb_agendamento;
CREATE TRIGGER trg_tb_agendamento_valida_clinica
    BEFORE INSERT OR UPDATE OF pet_id, veterinarian_id, clinic_id ON public.tb_agendamento
    FOR EACH ROW EXECUTE FUNCTION pkg_petjourney.trg_valida_clinica_agendamento();

DROP TRIGGER IF EXISTS trg_tb_prontuario_valida_vinculos ON public.tb_prontuario;
CREATE TRIGGER trg_tb_prontuario_valida_vinculos
    BEFORE INSERT OR UPDATE OF pet_id, veterinarian_id, appointment_id ON public.tb_prontuario
    FOR EACH ROW EXECUTE FUNCTION pkg_petjourney.trg_valida_vinculos_prontuario();

DROP TRIGGER IF EXISTS trg_tb_medicamento_valida_vinculos ON public.tb_medicamento;
CREATE TRIGGER trg_tb_medicamento_valida_vinculos
    BEFORE INSERT OR UPDATE OF pet_id, veterinarian_id, medical_record_id ON public.tb_medicamento
    FOR EACH ROW EXECUTE FUNCTION pkg_petjourney.trg_valida_vinculos_medicamento();

DROP TRIGGER IF EXISTS trg_auditoria_agendamento ON public.tb_agendamento;
CREATE TRIGGER trg_auditoria_agendamento
    AFTER INSERT OR UPDATE OR DELETE ON public.tb_agendamento
    FOR EACH ROW EXECUTE FUNCTION pkg_petjourney.trg_auditoria_agendamento();

COMMENT ON FUNCTION pkg_petjourney.trg_valida_clinica_agendamento() IS 'Gatilho: pet e veterinario devem pertencer a clinica do agendamento (PJ060, PJ061, PJ062).';
COMMENT ON FUNCTION pkg_petjourney.trg_valida_vinculos_prontuario() IS 'Gatilho: coerencia de clinica e de pet/veterinario do prontuario com o agendamento (PJ063, PJ064).';
COMMENT ON FUNCTION pkg_petjourney.trg_valida_vinculos_medicamento() IS 'Gatilho: coerencia de clinica e de pet/veterinario do medicamento com o prontuario (PJ065, PJ066).';
COMMENT ON FUNCTION pkg_petjourney.trg_auditoria_agendamento() IS 'Gatilho: auditoria de INSERT, UPDATE e DELETE de tb_agendamento, na mesma transacao.';
