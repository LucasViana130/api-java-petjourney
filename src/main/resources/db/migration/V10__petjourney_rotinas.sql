-- ============================================================
-- PETJOURNEY - SPRINT 4 - V10: ROTINAS (PROCEDURES E FUNCOES)
--
-- ATENCAO: renumere conforme flyway_schema_history (sugestao: V10).
-- Requer V8 e V9 aplicadas antes.
--
-- Equivalencia com a entrega Oracle: o package vira o schema
-- pkg_petjourney; erros ORA-20xxx viram SQLSTATE 'PJ' + 3 digitos
-- (ORA-20018 -> PJ018) com a MESMA mensagem.
--
-- Diferencas inevitaveis do PostgreSQL:
--  * nao existe AUTONOMOUS_TRANSACTION: a rotina lanca o erro e o
--    Java grava o log chamando prc_log_erro em transacao separada;
--  * ids gerados e textos de relatorio voltam em parametros INOUT
--    (CALL ... devolve uma linha de resultado);
--  * DBMS_OUTPUT nao existe: os relatorios devolvem um documento JSON em INOUT.
-- ============================================================

-- ------------------------------------------------------------
-- LOG DE ERRO (chamar em transacao propria apos o ROLLBACK)
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_log_erro(
    p_routine        text,
    p_sqlstate       text,
    p_message        text,
    p_correlation_id text DEFAULT NULL
) LANGUAGE plpgsql AS $f$
BEGIN
    INSERT INTO public.tb_log_erro (routine_name, db_username, occurred_at, sqlstate, message, correlation_id)
    VALUES (COALESCE(p_routine, '?'), current_user::text, clock_timestamp(),
            left(p_sqlstate, 5), left(p_message, 4000), p_correlation_id);
END
$f$;

-- Uso interno por funcoes que retornam NULL em erro: nunca falha o chamador
-- (por exemplo em transacao somente leitura).
CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_log_erro_seguro(
    p_routine text, p_sqlstate text, p_message text
) LANGUAGE plpgsql AS $f$
BEGIN
    BEGIN
        CALL pkg_petjourney.prc_log_erro(p_routine, p_sqlstate, p_message);
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
END
$f$;

-- ------------------------------------------------------------
-- CADASTROS
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_inserir_clinica(
    p_name    text,
    p_cnpj    text,
    p_phone   text,
    p_email   text,
    p_address text,
    INOUT p_id bigint
) LANGUAGE plpgsql AS $f$
BEGIN
    IF p_name IS NULL OR btrim(p_name) = '' OR p_cnpj IS NULL OR btrim(p_cnpj) = '' THEN
        RAISE EXCEPTION 'Nome e CNPJ da clinica sao obrigatorios.' USING ERRCODE = 'PJ001';
    END IF;

    INSERT INTO public.tb_clinica (name, cnpj, phone, email, address, active)
    VALUES (btrim(p_name), p_cnpj, p_phone, p_email, p_address, true)
    RETURNING id INTO p_id;
END
$f$;

CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_inserir_tutor(
    p_name      text,
    p_clinic_id bigint,
    p_cpf       text,
    p_phone     text,
    p_email     text,
    INOUT p_id  bigint
) LANGUAGE plpgsql AS $f$
DECLARE
    v_ativa boolean;
BEGIN
    IF p_name IS NULL OR btrim(p_name) = '' OR p_clinic_id IS NULL OR p_cpf IS NULL THEN
        RAISE EXCEPTION 'Nome, clinica e CPF do tutor sao obrigatorios.' USING ERRCODE = 'PJ002';
    END IF;

    SELECT active INTO v_ativa FROM public.tb_clinica WHERE id = p_clinic_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Clinica informada nao existe.' USING ERRCODE = 'PJ004';
    END IF;
    IF NOT v_ativa THEN
        RAISE EXCEPTION 'Nao e possivel cadastrar tutor em clinica inativa.' USING ERRCODE = 'PJ071';
    END IF;

    INSERT INTO public.tb_tutor (clinic_id, name, cpf, phone, email, active)
    VALUES (p_clinic_id, btrim(p_name), p_cpf, p_phone, p_email, true)
    RETURNING id INTO p_id;
EXCEPTION
    WHEN unique_violation THEN
        RAISE EXCEPTION 'CPF ja cadastrado para outro tutor.' USING ERRCODE = 'PJ003';
    WHEN foreign_key_violation THEN
        RAISE EXCEPTION 'Clinica informada nao existe.' USING ERRCODE = 'PJ004';
END
$f$;

CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_inserir_veterinario(
    p_name      text,
    p_clinic_id bigint,
    p_crmv      text,
    p_phone     text,
    p_email     text,
    p_specialty text,
    INOUT p_id  bigint
) LANGUAGE plpgsql AS $f$
DECLARE
    v_ativa boolean;
BEGIN
    IF p_name IS NULL OR btrim(p_name) = '' OR p_clinic_id IS NULL
       OR p_crmv IS NULL OR btrim(p_crmv) = '' THEN
        RAISE EXCEPTION 'Nome, clinica e CRMV do veterinario sao obrigatorios.' USING ERRCODE = 'PJ005';
    END IF;

    SELECT active INTO v_ativa FROM public.tb_clinica WHERE id = p_clinic_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Clinica informada nao existe.' USING ERRCODE = 'PJ007';
    END IF;
    IF NOT v_ativa THEN
        RAISE EXCEPTION 'Nao e possivel cadastrar veterinario em clinica inativa.' USING ERRCODE = 'PJ072';
    END IF;

    INSERT INTO public.tb_veterinario (clinic_id, name, crmv, phone, email, specialty, active)
    VALUES (p_clinic_id, btrim(p_name), p_crmv, p_phone, p_email, p_specialty, true)
    RETURNING id INTO p_id;
EXCEPTION
    WHEN unique_violation THEN
        RAISE EXCEPTION 'CRMV ja cadastrado para outro veterinario.' USING ERRCODE = 'PJ006';
    WHEN foreign_key_violation THEN
        RAISE EXCEPTION 'Clinica informada nao existe.' USING ERRCODE = 'PJ007';
END
$f$;

CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_inserir_pet(
    p_name       text,
    p_tutor_id   bigint,
    p_species    text,
    p_breed      text,
    p_sex        text,
    p_birth_date date,
    p_weight     numeric,
    INOUT p_id   bigint
) LANGUAGE plpgsql AS $f$
DECLARE
    v_tutor_ativo   boolean;
    v_clinica_ativa boolean;
BEGIN
    IF p_name IS NULL OR btrim(p_name) = '' OR p_tutor_id IS NULL OR p_species IS NULL THEN
        RAISE EXCEPTION 'Nome, tutor e especie do pet sao obrigatorios.' USING ERRCODE = 'PJ008';
    END IF;

    IF p_species NOT IN ('CACHORRO','GATO','AVE','ROEDOR','REPTIL','OUTRO') THEN
        RAISE EXCEPTION 'Especie invalida.' USING ERRCODE = 'PJ009';
    END IF;

    IF p_sex IS NOT NULL AND p_sex NOT IN ('MACHO','FEMEA') THEN
        RAISE EXCEPTION 'Sexo invalido. Use MACHO ou FEMEA.' USING ERRCODE = 'PJ010';
    END IF;

    SELECT t.active, c.active INTO v_tutor_ativo, v_clinica_ativa
      FROM public.tb_tutor t
      JOIN public.tb_clinica c ON c.id = t.clinic_id
     WHERE t.id = p_tutor_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Tutor informado nao existe.' USING ERRCODE = 'PJ011';
    END IF;
    IF NOT v_tutor_ativo THEN
        RAISE EXCEPTION 'Nao e possivel cadastrar pet para tutor inativo.' USING ERRCODE = 'PJ074';
    END IF;
    IF NOT v_clinica_ativa THEN
        RAISE EXCEPTION 'Nao e possivel cadastrar pet: clinica do tutor esta inativa.' USING ERRCODE = 'PJ075';
    END IF;

    INSERT INTO public.tb_pet (tutor_id, name, species, breed, sex, birth_date, weight, active)
    VALUES (p_tutor_id, btrim(p_name), p_species, p_breed, p_sex, p_birth_date, p_weight, true)
    RETURNING id INTO p_id;
EXCEPTION
    WHEN foreign_key_violation THEN
        RAISE EXCEPTION 'Tutor informado nao existe.' USING ERRCODE = 'PJ011';
END
$f$;

CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_inserir_disponibilidade(
    p_veterinarian_id bigint,
    p_start_time      timestamp,
    p_end_time        timestamp,
    INOUT p_id        bigint
) LANGUAGE plpgsql AS $f$
DECLARE
    v_vet_ativo     boolean;
    v_clinica_ativa boolean;
BEGIN
    IF p_veterinarian_id IS NULL OR p_start_time IS NULL OR p_end_time IS NULL THEN
        RAISE EXCEPTION 'Veterinario, inicio e fim da disponibilidade sao obrigatorios.' USING ERRCODE = 'PJ012';
    END IF;

    IF p_end_time <= p_start_time THEN
        RAISE EXCEPTION 'O fim da disponibilidade deve ser depois do inicio.' USING ERRCODE = 'PJ013';
    END IF;

    SELECT v.active, c.active INTO v_vet_ativo, v_clinica_ativa
      FROM public.tb_veterinario v
      JOIN public.tb_clinica c ON c.id = v.clinic_id
     WHERE v.id = p_veterinarian_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Veterinario informado nao existe.' USING ERRCODE = 'PJ015';
    END IF;
    IF NOT v_vet_ativo THEN
        RAISE EXCEPTION 'Nao e possivel cadastrar disponibilidade para veterinario inativo.' USING ERRCODE = 'PJ077';
    END IF;
    IF NOT v_clinica_ativa THEN
        RAISE EXCEPTION 'Nao e possivel cadastrar disponibilidade: clinica do veterinario esta inativa.' USING ERRCODE = 'PJ078';
    END IF;

    INSERT INTO public.tb_disponibilidade_veterinario (veterinarian_id, start_time, end_time)
    VALUES (p_veterinarian_id, p_start_time, p_end_time)
    RETURNING id INTO p_id;
EXCEPTION
    WHEN unique_violation THEN
        RAISE EXCEPTION 'Veterinario ja possui disponibilidade neste horario.' USING ERRCODE = 'PJ014';
    WHEN foreign_key_violation THEN
        RAISE EXCEPTION 'Veterinario informado nao existe.' USING ERRCODE = 'PJ015';
END
$f$;

-- p_status: NULL assume 'PENDENTE'.
CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_inserir_agendamento(
    p_clinic_id       bigint,
    p_pet_id          bigint,
    p_veterinarian_id bigint,
    p_title           text,
    p_description     text,
    p_date_time       timestamp,
    p_status          text,
    INOUT p_id        bigint
) LANGUAGE plpgsql AS $f$
DECLARE
    v_status         text := COALESCE(p_status, 'PENDENTE');
    v_clinica_ativa  boolean;
    v_pet_ativo      boolean;
    v_tutor_ativo    boolean;
    v_vet_ativo      boolean;
    v_clinicavet_ativa boolean;
BEGIN
    IF v_status NOT IN ('PENDENTE','REALIZADO','CANCELADO') THEN
        RAISE EXCEPTION 'Status invalido. Use PENDENTE, REALIZADO ou CANCELADO.' USING ERRCODE = 'PJ016';
    END IF;

    SELECT active INTO v_clinica_ativa FROM public.tb_clinica WHERE id = p_clinic_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Clinica, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ019';
    END IF;
    IF NOT v_clinica_ativa THEN
        RAISE EXCEPTION 'Nao e possivel agendar: clinica inativa.' USING ERRCODE = 'PJ080';
    END IF;

    SELECT p.active, t.active INTO v_pet_ativo, v_tutor_ativo
      FROM public.tb_pet p
      JOIN public.tb_tutor t ON t.id = p.tutor_id
     WHERE p.id = p_pet_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Clinica, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ019';
    END IF;
    IF NOT v_pet_ativo THEN
        RAISE EXCEPTION 'Nao e possivel agendar: pet inativo.' USING ERRCODE = 'PJ081';
    END IF;
    IF NOT v_tutor_ativo THEN
        RAISE EXCEPTION 'Nao e possivel agendar: tutor do pet esta inativo.' USING ERRCODE = 'PJ082';
    END IF;

    SELECT v.active, c.active INTO v_vet_ativo, v_clinicavet_ativa
      FROM public.tb_veterinario v
      JOIN public.tb_clinica c ON c.id = v.clinic_id
     WHERE v.id = p_veterinarian_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Clinica, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ019';
    END IF;
    IF NOT v_vet_ativo THEN
        RAISE EXCEPTION 'Nao e possivel agendar: veterinario inativo.' USING ERRCODE = 'PJ083';
    END IF;
    IF NOT v_clinicavet_ativa THEN
        RAISE EXCEPTION 'Nao e possivel agendar: clinica do veterinario esta inativa.' USING ERRCODE = 'PJ084';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM public.tb_disponibilidade_veterinario
                    WHERE veterinarian_id = p_veterinarian_id AND start_time = p_date_time) THEN
        RAISE EXCEPTION 'Horario nao esta disponivel para este veterinario.' USING ERRCODE = 'PJ017';
    END IF;

    IF EXISTS (SELECT 1 FROM public.tb_agendamento
                WHERE veterinarian_id = p_veterinarian_id AND date_time = p_date_time
                  AND status <> 'CANCELADO') THEN
        RAISE EXCEPTION 'Veterinario ja possui agendamento neste horario.' USING ERRCODE = 'PJ018';
    END IF;

    INSERT INTO public.tb_agendamento (clinic_id, pet_id, veterinarian_id, title, description, date_time, status)
    VALUES (p_clinic_id, p_pet_id, p_veterinarian_id, p_title, p_description, p_date_time, v_status)
    RETURNING id INTO p_id;
EXCEPTION
    -- corrida: outra transacao gravou o mesmo horario entre o EXISTS e o INSERT
    WHEN unique_violation THEN
        RAISE EXCEPTION 'Veterinario ja possui agendamento neste horario.' USING ERRCODE = 'PJ018';
    WHEN foreign_key_violation THEN
        RAISE EXCEPTION 'Clinica, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ019';
END
$f$;

CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_inserir_prontuario(
    p_appointment_id    bigint,
    p_pet_id            bigint,
    p_veterinarian_id   bigint,
    p_registration_date timestamp,
    p_main_complaint    text,
    p_diagnosis         text,
    p_conduct           text,
    p_observations      text,
    p_clinical_notes    text,
    p_recommendations   text,
    p_prescription_notes text,
    INOUT p_id          bigint
) LANGUAGE plpgsql AS $f$
DECLARE
    v_pet_ativo boolean;
    v_vet_ativo boolean;
BEGIN
    IF p_pet_id IS NULL OR p_veterinarian_id IS NULL OR p_registration_date IS NULL
       OR p_main_complaint IS NULL OR btrim(p_main_complaint) = '' THEN
        RAISE EXCEPTION 'Pet, veterinario, data de registro e queixa principal sao obrigatorios.' USING ERRCODE = 'PJ020';
    END IF;

    SELECT active INTO v_pet_ativo FROM public.tb_pet WHERE id = p_pet_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Agendamento, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ022';
    END IF;
    IF NOT v_pet_ativo THEN
        RAISE EXCEPTION 'Nao e possivel criar prontuario para pet inativo.' USING ERRCODE = 'PJ085';
    END IF;

    SELECT active INTO v_vet_ativo FROM public.tb_veterinario WHERE id = p_veterinarian_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Agendamento, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ022';
    END IF;
    IF NOT v_vet_ativo THEN
        RAISE EXCEPTION 'Nao e possivel criar prontuario para veterinario inativo.' USING ERRCODE = 'PJ086';
    END IF;

    INSERT INTO public.tb_prontuario (appointment_id, pet_id, veterinarian_id, registration_date,
        main_complaint, diagnosis, conduct, observations, clinical_notes, recommendations, prescription_notes)
    VALUES (p_appointment_id, p_pet_id, p_veterinarian_id, p_registration_date,
        p_main_complaint, p_diagnosis, p_conduct, p_observations, p_clinical_notes, p_recommendations, p_prescription_notes)
    RETURNING id INTO p_id;
EXCEPTION
    WHEN unique_violation THEN
        RAISE EXCEPTION 'Este agendamento ja possui prontuario.' USING ERRCODE = 'PJ021';
    WHEN foreign_key_violation THEN
        RAISE EXCEPTION 'Agendamento, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ022';
END
$f$;

CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_inserir_medicamento(
    p_medical_record_id bigint,
    p_pet_id            bigint,
    p_veterinarian_id   bigint,
    p_name              text,
    p_dosage            text,
    p_frequency         text,
    p_start_date        date,
    p_end_date          date,
    p_observations      text,
    p_status            text,
    INOUT p_id          bigint
) LANGUAGE plpgsql AS $f$
DECLARE
    v_pet_ativo boolean;
    v_vet_ativo boolean;
BEGIN
    IF p_name IS NULL OR btrim(p_name) = '' OR p_pet_id IS NULL OR p_veterinarian_id IS NULL THEN
        RAISE EXCEPTION 'Nome do medicamento, pet e veterinario sao obrigatorios.' USING ERRCODE = 'PJ023';
    END IF;

    IF p_status IS NOT NULL AND p_status NOT IN ('ATIVO','FINALIZADO','CANCELADO') THEN
        RAISE EXCEPTION 'Status invalido. Use ATIVO, FINALIZADO ou CANCELADO.' USING ERRCODE = 'PJ024';
    END IF;

    IF p_start_date IS NOT NULL AND p_end_date IS NOT NULL AND p_end_date < p_start_date THEN
        RAISE EXCEPTION 'A data fim do tratamento nao pode ser anterior a data inicio.' USING ERRCODE = 'PJ025';
    END IF;

    SELECT active INTO v_pet_ativo FROM public.tb_pet WHERE id = p_pet_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Prontuario, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ026';
    END IF;
    IF NOT v_pet_ativo THEN
        RAISE EXCEPTION 'Nao e possivel prescrever medicamento para pet inativo.' USING ERRCODE = 'PJ087';
    END IF;

    SELECT active INTO v_vet_ativo FROM public.tb_veterinario WHERE id = p_veterinarian_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Prontuario, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ026';
    END IF;
    IF NOT v_vet_ativo THEN
        RAISE EXCEPTION 'Nao e possivel prescrever medicamento com veterinario inativo.' USING ERRCODE = 'PJ088';
    END IF;

    INSERT INTO public.tb_medicamento (medical_record_id, pet_id, veterinarian_id, name, dosage,
        frequency, start_date, end_date, observations, status)
    VALUES (p_medical_record_id, p_pet_id, p_veterinarian_id, btrim(p_name), p_dosage,
        p_frequency, p_start_date, p_end_date, p_observations, p_status)
    RETURNING id INTO p_id;
EXCEPTION
    WHEN foreign_key_violation THEN
        RAISE EXCEPTION 'Prontuario, pet ou veterinario informado nao existe.' USING ERRCODE = 'PJ026';
END
$f$;

-- p_active NULL assume true. p_password deve chegar JA com hash (BCrypt) do Java.
CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_inserir_usuario(
    p_username                text,
    p_password                text,
    p_role                    text,
    p_active                  boolean,
    p_first_access_code       text,
    p_first_access_expires_at timestamp,
    p_first_access_used_at    timestamp,
    p_clinic_id               bigint,
    p_tutor_id                bigint,
    p_veterinarian_id         bigint,
    INOUT p_id                bigint
) LANGUAGE plpgsql AS $f$
DECLARE
    v_ativa  boolean;
    v_ativo  boolean;
BEGIN
    IF p_username IS NULL OR btrim(p_username) = '' OR p_password IS NULL OR p_role IS NULL THEN
        RAISE EXCEPTION 'Username, senha e perfil sao obrigatorios.' USING ERRCODE = 'PJ027';
    END IF;

    IF p_role NOT IN ('ADMIN_SISTEMA','ADMIN_CLINICA','VETERINARIO','TUTOR') THEN
        RAISE EXCEPTION 'Perfil invalido. Use ADMIN_SISTEMA, ADMIN_CLINICA, VETERINARIO ou TUTOR.' USING ERRCODE = 'PJ028';
    END IF;

    IF p_role = 'ADMIN_CLINICA' THEN
        SELECT active INTO v_ativa FROM public.tb_clinica WHERE id = p_clinic_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Clinica, tutor ou veterinario informado nao existe.' USING ERRCODE = 'PJ030';
        END IF;
        IF NOT v_ativa THEN
            RAISE EXCEPTION 'Nao e possivel criar conta vinculada a clinica inativa.' USING ERRCODE = 'PJ094';
        END IF;
    ELSIF p_role = 'TUTOR' THEN
        SELECT t.active, c.active INTO v_ativo, v_ativa
          FROM public.tb_tutor t JOIN public.tb_clinica c ON c.id = t.clinic_id
         WHERE t.id = p_tutor_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Clinica, tutor ou veterinario informado nao existe.' USING ERRCODE = 'PJ030';
        END IF;
        IF NOT v_ativo THEN
            RAISE EXCEPTION 'Nao e possivel criar conta para tutor inativo.' USING ERRCODE = 'PJ095';
        END IF;
        IF NOT v_ativa THEN
            RAISE EXCEPTION 'Nao e possivel criar conta vinculada a clinica inativa.' USING ERRCODE = 'PJ094';
        END IF;
    ELSIF p_role = 'VETERINARIO' THEN
        SELECT v.active, c.active INTO v_ativo, v_ativa
          FROM public.tb_veterinario v JOIN public.tb_clinica c ON c.id = v.clinic_id
         WHERE v.id = p_veterinarian_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Clinica, tutor ou veterinario informado nao existe.' USING ERRCODE = 'PJ030';
        END IF;
        IF NOT v_ativo THEN
            RAISE EXCEPTION 'Nao e possivel criar conta para veterinario inativo.' USING ERRCODE = 'PJ096';
        END IF;
        IF NOT v_ativa THEN
            RAISE EXCEPTION 'Nao e possivel criar conta vinculada a clinica inativa.' USING ERRCODE = 'PJ094';
        END IF;
    END IF;

    INSERT INTO public.tb_usuario (username, password, role, active, first_access_code,
        first_access_expires_at, first_access_used_at, clinic_id, tutor_id, veterinarian_id)
    VALUES (lower(btrim(p_username)), p_password, p_role, COALESCE(p_active, true), p_first_access_code,
        p_first_access_expires_at, p_first_access_used_at, p_clinic_id, p_tutor_id, p_veterinarian_id)
    RETURNING id INTO p_id;
EXCEPTION
    WHEN unique_violation THEN
        RAISE EXCEPTION 'Username ja cadastrado, ou tutor/veterinario/clinica ja possui conta.' USING ERRCODE = 'PJ029';
    WHEN foreign_key_violation THEN
        RAISE EXCEPTION 'Clinica, tutor ou veterinario informado nao existe.' USING ERRCODE = 'PJ030';
    WHEN check_violation THEN
        RAISE EXCEPTION 'Vinculo incoerente com o perfil informado.' USING ERRCODE = 'PJ031';
END
$f$;

-- ------------------------------------------------------------
-- SOFT DELETE
-- ------------------------------------------------------------
-- Desativa o tutor, a conta dele e TODOS os pets dele (como o TutorService.delete do Java).
-- Preserva agendamentos, prontuarios e medicamentos. Repetir a chamada e seguro.
-- Nao faz COMMIT: a transacao de negocio e do Java.
CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_desativar_tutor(p_id bigint)
LANGUAGE plpgsql AS $f$
BEGIN
    UPDATE public.tb_tutor SET active = false WHERE id = p_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Tutor informado nao existe.' USING ERRCODE = 'PJ090';
    END IF;
    UPDATE public.tb_usuario SET active = false WHERE tutor_id = p_id;
    UPDATE public.tb_pet SET active = false WHERE tutor_id = p_id;
END
$f$;

CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_desativar_veterinario(p_id bigint)
LANGUAGE plpgsql AS $f$
BEGIN
    UPDATE public.tb_veterinario SET active = false WHERE id = p_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Veterinario informado nao existe.' USING ERRCODE = 'PJ091';
    END IF;
    UPDATE public.tb_usuario SET active = false WHERE veterinarian_id = p_id;
END
$f$;

CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_desativar_pet(p_id bigint)
LANGUAGE plpgsql AS $f$
BEGIN
    UPDATE public.tb_pet SET active = false WHERE id = p_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Pet informado nao existe.' USING ERRCODE = 'PJ092';
    END IF;
END
$f$;

-- Desativa a clinica e seus dependentes (como o ClinicService.delete do Java):
-- tutores e pets desses tutores, veterinarios, e as contas (admin da clinica,
-- tutores e veterinarios). Preserva agendamentos, prontuarios, medicamentos e
-- disponibilidades (sem DELETE fisico). Repetir a chamada e seguro.
-- Nao faz COMMIT: a transacao de negocio e do Java.
CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_desativar_clinica(p_id bigint)
LANGUAGE plpgsql AS $f$
BEGIN
    UPDATE public.tb_clinica SET active = false WHERE id = p_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Clinica informada nao existe.' USING ERRCODE = 'PJ093';
    END IF;

    UPDATE public.tb_usuario SET active = false
     WHERE clinic_id = p_id
        OR tutor_id IN (SELECT id FROM public.tb_tutor WHERE clinic_id = p_id)
        OR veterinarian_id IN (SELECT id FROM public.tb_veterinario WHERE clinic_id = p_id);

    UPDATE public.tb_pet SET active = false
     WHERE tutor_id IN (SELECT id FROM public.tb_tutor WHERE clinic_id = p_id);
    UPDATE public.tb_tutor SET active = false WHERE clinic_id = p_id;
    UPDATE public.tb_veterinario SET active = false WHERE clinic_id = p_id;
END
$f$;

-- ------------------------------------------------------------
-- FUNCOES
-- ------------------------------------------------------------
-- Escape JSON MANUAL (sem funcoes JSON nativas): barra invertida primeiro, depois
-- aspas e TODOS os controles U+0001..U+001F (\b \t \n \f \r e \u00XX para os demais).
-- U+0000 nao existe em text no PostgreSQL (o proprio servidor o rejeita na entrada).
-- Nao altera acentos nem troca aspas por apostrofos.
CREATE OR REPLACE FUNCTION pkg_petjourney.fn_escapar_json(p_texto text)
RETURNS text LANGUAGE plpgsql IMMUTABLE AS $f$
DECLARE
    v text := p_texto;
    i integer;
BEGIN
    IF v IS NULL THEN
        RETURN NULL;
    END IF;

    v := replace(v, E'\\', E'\\\\');      -- 1) barra invertida (antes de criar novos escapes)
    v := replace(v, '"', E'\\"');            -- 2) aspas duplas

    FOR i IN 1..31 LOOP                       -- 3) controles
        v := replace(v, chr(i), CASE i
                WHEN 8  THEN E'\\b'
                WHEN 9  THEN E'\\t'
                WHEN 10 THEN E'\\n'
                WHEN 12 THEN E'\\f'
                WHEN 13 THEN E'\\r'
                ELSE E'\\u' || lpad(to_hex(i), 4, '0')
            END);
    END LOOP;

    RETURN v;
END
$f$;

-- Monta o JSON de um pet SEM funcoes JSON nativas (mesma ideia da Sprint 3,
-- agora com escape correto de aspas, barras e quebras de linha).
CREATE OR REPLACE FUNCTION pkg_petjourney.fn_montar_json_pet(
    p_pet text, p_tutor text, p_clinica text
) RETURNS text LANGUAGE plpgsql AS $f$
BEGIN
    IF p_pet IS NULL THEN
        CALL pkg_petjourney.prc_log_erro_seguro('fn_montar_json_pet', 'PJ050', 'Nome do pet e obrigatorio para montar o JSON.');
        RETURN '{"erro":"nome do pet obrigatorio"}';
    END IF;

    RETURN '{"pet":"'    || pkg_petjourney.fn_escapar_json(p_pet) || '",' ||
           '"tutor":"'   || pkg_petjourney.fn_escapar_json(COALESCE(p_tutor, 'N/A')) || '",' ||
           '"clinica":"' || pkg_petjourney.fn_escapar_json(COALESCE(p_clinica, 'N/A')) || '"}';
EXCEPTION WHEN OTHERS THEN
    CALL pkg_petjourney.prc_log_erro_seguro('fn_montar_json_pet', SQLSTATE, SQLERRM);
    RETURN '{"erro":"falha ao montar json"}';
END
$f$;

-- Idade do pet em meses completos. NULL se a data for nula (PJ051) ou futura (PJ052).
CREATE OR REPLACE FUNCTION pkg_petjourney.fn_calcular_idade_pet_meses(p_birth_date date)
RETURNS integer LANGUAGE plpgsql AS $f$
DECLARE
    v_idade interval;
BEGIN
    IF p_birth_date IS NULL THEN
        CALL pkg_petjourney.prc_log_erro_seguro('fn_calcular_idade_pet_meses', 'PJ051', 'Data de nascimento nao informada.');
        RETURN NULL;
    END IF;

    IF p_birth_date > current_date THEN
        CALL pkg_petjourney.prc_log_erro_seguro('fn_calcular_idade_pet_meses', 'PJ052', 'Data de nascimento nao pode ser futura.');
        RETURN NULL;
    END IF;

    v_idade := age(current_date, p_birth_date);
    RETURN (extract(year FROM v_idade) * 12 + extract(month FROM v_idade))::integer;
EXCEPTION WHEN OTHERS THEN
    CALL pkg_petjourney.prc_log_erro_seguro('fn_calcular_idade_pet_meses', SQLSTATE, SQLERRM);
    RETURN NULL;
END
$f$;

-- ------------------------------------------------------------
-- RELATORIOS (retorno: UM documento JSON em p_resultado; sem titulos nem NOTICE)
-- Ambos exigem p_clinic_id: NULL NUNCA libera consulta global.
--   PJ054 clinica inexistente | PJ055 clinica inativa | PJ056 clinica obrigatoria
--   PJ057 especie invalida    | clinica valida sem pets => '[]'
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_relatorio_pets_json(
    p_clinic_id bigint,
    INOUT p_resultado text
) LANGUAGE plpgsql AS $f$
DECLARE
    v_ativa boolean;
    v_qtd   integer := 0;
    r       record;
    v_json  text;
BEGIN
    IF p_clinic_id IS NULL THEN
        RAISE EXCEPTION 'Clinica e obrigatoria para o relatorio.' USING ERRCODE = 'PJ056';
    END IF;
    SELECT active INTO v_ativa FROM public.tb_clinica WHERE id = p_clinic_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Clinica informada nao existe.' USING ERRCODE = 'PJ054';
    END IF;
    IF NOT v_ativa THEN
        RAISE EXCEPTION 'Clinica informada esta inativa.' USING ERRCODE = 'PJ055';
    END IF;

    p_resultado := '[';

    FOR r IN
        SELECT p.id AS pet_id, p.name AS pet, t.id AS tutor_id, t.name AS tutor, c.id AS clinic_id, c.name AS clinica
          FROM public.tb_pet p
          JOIN public.tb_tutor t ON t.id = p.tutor_id
          JOIN public.tb_clinica c ON c.id = t.clinic_id
         WHERE t.clinic_id = p_clinic_id
           AND p.active AND t.active AND c.active
         ORDER BY p.id
    LOOP
        -- fn_montar_json_pet (montagem manual) gera {"pet":..,"tutor":..,"clinica":..};
        -- aqui so se acrescentam os ids reais no inicio do objeto.
        v_json := pkg_petjourney.fn_montar_json_pet(r.pet, r.tutor, r.clinica);
        p_resultado := p_resultado || CASE WHEN v_qtd > 0 THEN ',' ELSE '' END
            || '{"petId":' || r.pet_id || ',"tutorId":' || r.tutor_id || ',"clinicId":' || r.clinic_id
            || ',' || substr(v_json, 2);
        v_qtd := v_qtd + 1;
    END LOOP;

    p_resultado := p_resultado || ']';
END
$f$;

-- Item do relatorio de pesos, montado manualmente (sex/species NULL viram null).
CREATE OR REPLACE FUNCTION pkg_petjourney.fn_item_peso_json(
    p_tipo text, p_species text, p_sex text, p_peso numeric
) RETURNS text LANGUAGE sql IMMUTABLE AS $f$
    SELECT '{"tipo":"' || p_tipo || '"'
        || ',"species":' || COALESCE('"' || pkg_petjourney.fn_escapar_json(p_species) || '"', 'null')
        || ',"sex":'     || COALESCE('"' || pkg_petjourney.fn_escapar_json(p_sex) || '"', 'null')
        || ',"pesoKg":'  || round(p_peso, 2)::text || '}'
$f$;

-- A assinatura antiga (sem clinica) permitia relatorio global: removida de proposito
-- (DROP sem CASCADE: se algo depender dela, o erro avisa em vez de apagar em silencio).
DROP PROCEDURE IF EXISTS pkg_petjourney.prc_relatorio_peso_pets(text, text);

-- Somatorio MANUAL por especie/sexo, com subtotal por especie e total geral:
-- le as linhas detalhadas dos pets e acumula no loop (sem SUM, ROLLUP, CUBE ou GROUPING SETS).
-- Item: tipo DETALHE (species+sex), SUBTOTAL (sex null), TOTAL (species e sex null).
-- Peso ausente contribui com zero. Sexo ausente e tratado com IS DISTINCT FROM.
CREATE OR REPLACE PROCEDURE pkg_petjourney.prc_relatorio_peso_pets(
    p_clinic_id bigint,
    p_species   text,
    INOUT p_resultado text
) LANGUAGE plpgsql AS $f$
DECLARE
    v_ativa      boolean;
    r            record;
    v_qtd        integer := 0;
    v_especie    text;
    v_sexo       text;
    v_peso_sexo  numeric := 0;
    v_subtotal   numeric := 0;
    v_total      numeric := 0;
    v_itens      text := '';
BEGIN
    IF p_clinic_id IS NULL THEN
        RAISE EXCEPTION 'Clinica e obrigatoria para o relatorio.' USING ERRCODE = 'PJ056';
    END IF;
    SELECT active INTO v_ativa FROM public.tb_clinica WHERE id = p_clinic_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Clinica informada nao existe.' USING ERRCODE = 'PJ054';
    END IF;
    IF NOT v_ativa THEN
        RAISE EXCEPTION 'Clinica informada esta inativa.' USING ERRCODE = 'PJ055';
    END IF;
    IF p_species IS NOT NULL AND p_species NOT IN ('CACHORRO','GATO','AVE','ROEDOR','REPTIL','OUTRO') THEN
        RAISE EXCEPTION 'Especie invalida para filtro do relatorio.' USING ERRCODE = 'PJ057';
    END IF;

    FOR r IN
        SELECT p.species, p.sex, p.weight
          FROM public.tb_pet p
          JOIN public.tb_tutor t ON t.id = p.tutor_id
          JOIN public.tb_clinica c ON c.id = t.clinic_id
         WHERE t.clinic_id = p_clinic_id
           AND (p_species IS NULL OR p.species = p_species)
           AND p.active AND t.active AND c.active
         ORDER BY p.species, (p.sex IS NULL), p.sex, p.id
    LOOP
        IF v_qtd > 0 AND r.species IS DISTINCT FROM v_especie THEN
            -- mudou a especie: fecha o ultimo detalhe e emite o subtotal
            v_itens := v_itens || pkg_petjourney.fn_item_peso_json('DETALHE', v_especie, v_sexo, v_peso_sexo) || ',';
            v_subtotal := v_subtotal + v_peso_sexo;
            v_itens := v_itens || pkg_petjourney.fn_item_peso_json('SUBTOTAL', v_especie, NULL, v_subtotal) || ',';
            v_peso_sexo := 0;
            v_subtotal := 0;
        ELSIF v_qtd > 0 AND r.sex IS DISTINCT FROM v_sexo THEN
            -- mudou o sexo: fecha o detalhe
            v_itens := v_itens || pkg_petjourney.fn_item_peso_json('DETALHE', v_especie, v_sexo, v_peso_sexo) || ',';
            v_subtotal := v_subtotal + v_peso_sexo;
            v_peso_sexo := 0;
        END IF;

        v_especie := r.species;
        v_sexo := r.sex;
        v_peso_sexo := v_peso_sexo + COALESCE(r.weight, 0);
        v_total := v_total + COALESCE(r.weight, 0);
        v_qtd := v_qtd + 1;
    END LOOP;

    IF v_qtd = 0 THEN
        p_resultado := '[]';
        RETURN;
    END IF;

    v_itens := v_itens || pkg_petjourney.fn_item_peso_json('DETALHE', v_especie, v_sexo, v_peso_sexo) || ',';
    v_subtotal := v_subtotal + v_peso_sexo;
    v_itens := v_itens || pkg_petjourney.fn_item_peso_json('SUBTOTAL', v_especie, NULL, v_subtotal) || ',';
    v_itens := v_itens || pkg_petjourney.fn_item_peso_json('TOTAL', NULL, NULL, v_total);

    p_resultado := '[' || v_itens || ']';
END
$f$;

-- ------------------------------------------------------------
-- EXPORTACAO PARA O MONGODB (mesmas chaves da entrega Oracle)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION pkg_petjourney.fn_exportar_clinicas() RETURNS json
LANGUAGE sql STABLE AS $f$
    SELECT COALESCE(json_agg(json_build_object(
        '_id', c.id, 'nome', c.name, 'cnpj', c.cnpj, 'telefone', c.phone, 'email', c.email,
        'endereco', c.address, 'ativo', c.active,
        'veterinarios', COALESCE((
            SELECT json_agg(json_build_object(
                'id', v.id, 'nome', v.name, 'crmv', v.crmv, 'telefone', v.phone, 'email', v.email,
                'especialidade', v.specialty, 'ativo', v.active) ORDER BY v.id)
              FROM public.tb_veterinario v WHERE v.clinic_id = c.id), '[]'::json)
    ) ORDER BY c.id), '[]'::json)
    FROM public.tb_clinica c
$f$;

CREATE OR REPLACE FUNCTION pkg_petjourney.fn_exportar_tutores() RETURNS json
LANGUAGE sql STABLE AS $f$
    SELECT COALESCE(json_agg(json_build_object(
        '_id', t.id, 'clinicaId', t.clinic_id, 'nome', t.name, 'cpf', t.cpf, 'telefone', t.phone,
        'email', t.email, 'ativo', t.active,
        'pets', COALESCE((
            SELECT json_agg(json_build_object(
                'id', p.id, 'nome', p.name, 'especie', p.species, 'raca', p.breed, 'sexo', p.sex,
                'dataNascimento', to_char(p.birth_date::timestamp, 'YYYY-MM-DD"T"HH24:MI:SS'),
                'pesoKg', p.weight, 'ativo', p.active,
                'medicamentos', COALESCE((
                    SELECT json_agg(json_build_object(
                        'id', m.id, 'prontuarioId', m.medical_record_id, 'veterinarioId', m.veterinarian_id,
                        'nome', m.name, 'dosagem', m.dosage, 'frequencia', m.frequency,
                        'dataInicio', to_char(m.start_date::timestamp, 'YYYY-MM-DD"T"HH24:MI:SS'),
                        'dataFim', to_char(m.end_date::timestamp, 'YYYY-MM-DD"T"HH24:MI:SS'),
                        'observacoes', m.observations, 'status', m.status) ORDER BY m.id)
                      FROM public.tb_medicamento m WHERE m.pet_id = p.id), '[]'::json)
            ) ORDER BY p.id)
              FROM public.tb_pet p WHERE p.tutor_id = t.id), '[]'::json)
    ) ORDER BY t.id), '[]'::json)
    FROM public.tb_tutor t
$f$;

CREATE OR REPLACE FUNCTION pkg_petjourney.fn_exportar_disponibilidades() RETURNS json
LANGUAGE sql STABLE AS $f$
    SELECT COALESCE(json_agg(json_build_object(
        '_id', d.id, 'veterinarioId', d.veterinarian_id,
        'inicio', to_char(d.start_time, 'YYYY-MM-DD"T"HH24:MI:SS'),
        'fim', to_char(d.end_time, 'YYYY-MM-DD"T"HH24:MI:SS')) ORDER BY d.id), '[]'::json)
    FROM public.tb_disponibilidade_veterinario d
$f$;

CREATE OR REPLACE FUNCTION pkg_petjourney.fn_exportar_agendamentos() RETURNS json
LANGUAGE sql STABLE AS $f$
    SELECT COALESCE(json_agg(json_build_object(
        '_id', a.id, 'clinicaId', a.clinic_id, 'petId', a.pet_id, 'petNome', p.name,
        'veterinarioId', a.veterinarian_id, 'veterinarioNome', v.name, 'titulo', a.title,
        'descricao', a.description,
        'dataHora', to_char(a.date_time, 'YYYY-MM-DD"T"HH24:MI:SS'),
        'status', a.status) ORDER BY a.id), '[]'::json)
    FROM public.tb_agendamento a
    JOIN public.tb_pet p ON p.id = a.pet_id
    JOIN public.tb_veterinario v ON v.id = a.veterinarian_id
$f$;

CREATE OR REPLACE FUNCTION pkg_petjourney.fn_exportar_prontuarios() RETURNS json
LANGUAGE sql STABLE AS $f$
    SELECT COALESCE(json_agg(json_build_object(
        '_id', r.id, 'agendamentoId', r.appointment_id, 'petId', r.pet_id,
        'veterinarioId', r.veterinarian_id,
        'dataRegistro', to_char(r.registration_date, 'YYYY-MM-DD"T"HH24:MI:SS'),
        'queixaPrincipal', r.main_complaint, 'diagnostico', r.diagnosis, 'conduta', r.conduct,
        'observacoes', r.observations, 'notasClinicas', r.clinical_notes,
        'recomendacoes', r.recommendations, 'notasPrescricao', r.prescription_notes) ORDER BY r.id), '[]'::json)
    FROM public.tb_prontuario r
$f$;

-- Dataset completo: um objeto JSON com uma chave por colecao.
CREATE OR REPLACE FUNCTION pkg_petjourney.fn_exportar_dataset() RETURNS json
LANGUAGE sql STABLE AS $f$
    SELECT json_build_object(
        'banco', 'petjourney',
        'geradoEm', to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS'),
        'clinicas', pkg_petjourney.fn_exportar_clinicas(),
        'tutores', pkg_petjourney.fn_exportar_tutores(),
        'disponibilidades', pkg_petjourney.fn_exportar_disponibilidades(),
        'agendamentos', pkg_petjourney.fn_exportar_agendamentos(),
        'prontuarios', pkg_petjourney.fn_exportar_prontuarios())
$f$;

COMMENT ON SCHEMA pkg_petjourney IS
    'Rotinas do PetJourney (procedures, funcoes e funcoes de gatilho). Equivale aos packages Oracle da Sprint 4; o PostgreSQL agrupa objetos por schema.';
