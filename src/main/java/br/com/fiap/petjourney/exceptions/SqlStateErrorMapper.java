package br.com.fiap.petjourney.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Map;

public final class SqlStateErrorMapper {

    private static final Map<String, ErrorMapping> ERRORS = Map.ofEntries(
            entry("PJ001", HttpStatus.BAD_REQUEST, "Nome e CNPJ da clinica sao obrigatorios"),
            entry("PJ002", HttpStatus.BAD_REQUEST, "Nome, clinica e CPF do tutor sao obrigatorios"),
            entry("PJ003", HttpStatus.CONFLICT, "CPF ja cadastrado para outro tutor"),
            entry("PJ004", HttpStatus.NOT_FOUND, "Clinica informada nao existe"),
            entry("PJ005", HttpStatus.BAD_REQUEST, "Nome, clinica e CRMV do veterinario sao obrigatorios"),
            entry("PJ006", HttpStatus.CONFLICT, "CRMV ja cadastrado para outro veterinario"),
            entry("PJ007", HttpStatus.NOT_FOUND, "Clinica informada nao existe"),
            entry("PJ008", HttpStatus.BAD_REQUEST, "Nome, tutor e especie do pet sao obrigatorios"),
            entry("PJ009", HttpStatus.BAD_REQUEST, "Especie invalida"),
            entry("PJ010", HttpStatus.BAD_REQUEST, "Sexo invalido. Use MACHO ou FEMEA"),
            entry("PJ011", HttpStatus.NOT_FOUND, "Tutor informado nao existe"),
            entry("PJ012", HttpStatus.BAD_REQUEST, "Veterinario, inicio e fim da disponibilidade sao obrigatorios"),
            entry("PJ013", HttpStatus.BAD_REQUEST, "O fim da disponibilidade deve ser depois do inicio"),
            entry("PJ014", HttpStatus.CONFLICT, "Veterinario ja possui disponibilidade neste horario"),
            entry("PJ015", HttpStatus.NOT_FOUND, "Veterinario informado nao existe"),
            entry("PJ016", HttpStatus.BAD_REQUEST, "Status de agendamento invalido"),
            entry("PJ017", HttpStatus.CONFLICT, "Horario nao esta disponivel para este veterinario"),
            entry("PJ018", HttpStatus.CONFLICT, "Veterinario ja possui agendamento neste horario"),
            entry("PJ019", HttpStatus.NOT_FOUND, "Clinica, pet ou veterinario informado nao existe"),
            entry("PJ020", HttpStatus.BAD_REQUEST, "Dados obrigatorios do prontuario nao informados"),
            entry("PJ021", HttpStatus.CONFLICT, "Este agendamento ja possui prontuario"),
            entry("PJ022", HttpStatus.NOT_FOUND, "Agendamento, pet ou veterinario informado nao existe"),
            entry("PJ023", HttpStatus.BAD_REQUEST, "Dados obrigatorios do medicamento nao informados"),
            entry("PJ024", HttpStatus.BAD_REQUEST, "Status de medicamento invalido"),
            entry("PJ025", HttpStatus.BAD_REQUEST, "A data fim do tratamento nao pode ser anterior a data inicio"),
            entry("PJ026", HttpStatus.NOT_FOUND, "Prontuario, pet ou veterinario informado nao existe"),
            entry("PJ027", HttpStatus.BAD_REQUEST, "Username, senha e perfil sao obrigatorios"),
            entry("PJ028", HttpStatus.BAD_REQUEST, "Perfil de usuario invalido"),
            entry("PJ029", HttpStatus.CONFLICT, "Usuario ou vinculo de conta ja cadastrado"),
            entry("PJ030", HttpStatus.NOT_FOUND, "Clinica, tutor ou veterinario informado nao existe"),
            entry("PJ031", HttpStatus.BAD_REQUEST, "Vinculo incoerente com o perfil informado"),
            entry("PJ054", HttpStatus.NOT_FOUND, "Clinica informada nao existe"),
            entry("PJ055", HttpStatus.CONFLICT, "Clinica informada esta inativa"),
            entry("PJ056", HttpStatus.BAD_REQUEST, "Clinica e obrigatoria para o relatorio"),
            entry("PJ057", HttpStatus.BAD_REQUEST, "Especie invalida para filtro do relatorio"),
            entry("PJ060", HttpStatus.UNPROCESSABLE_ENTITY, "O pet informado nao pertence a clinica do agendamento"),
            entry("PJ061", HttpStatus.UNPROCESSABLE_ENTITY, "O veterinario informado nao pertence a clinica do agendamento"),
            entry("PJ062", HttpStatus.UNPROCESSABLE_ENTITY, "Pet ou veterinario informado nao existe"),
            entry("PJ063", HttpStatus.UNPROCESSABLE_ENTITY, "Pet e veterinario do prontuario pertencem a clinicas diferentes"),
            entry("PJ064", HttpStatus.UNPROCESSABLE_ENTITY, "Pet ou veterinario do prontuario diferem do agendamento informado"),
            entry("PJ065", HttpStatus.UNPROCESSABLE_ENTITY, "Pet e veterinario do medicamento pertencem a clinicas diferentes"),
            entry("PJ066", HttpStatus.UNPROCESSABLE_ENTITY, "Pet ou veterinario do medicamento diferem do prontuario informado"),
            entry("PJ071", HttpStatus.CONFLICT, "Nao e possivel cadastrar tutor em clinica inativa"),
            entry("PJ072", HttpStatus.CONFLICT, "Nao e possivel cadastrar veterinario em clinica inativa"),
            entry("PJ074", HttpStatus.CONFLICT, "Nao e possivel cadastrar pet para tutor inativo"),
            entry("PJ075", HttpStatus.CONFLICT, "Nao e possivel cadastrar pet: clinica do tutor esta inativa"),
            entry("PJ077", HttpStatus.CONFLICT, "Nao e possivel cadastrar disponibilidade para veterinario inativo"),
            entry("PJ078", HttpStatus.CONFLICT, "Nao e possivel cadastrar disponibilidade: clinica do veterinario esta inativa"),
            entry("PJ080", HttpStatus.CONFLICT, "Nao e possivel agendar: clinica inativa"),
            entry("PJ081", HttpStatus.CONFLICT, "Nao e possivel agendar: pet inativo"),
            entry("PJ082", HttpStatus.CONFLICT, "Nao e possivel agendar: tutor do pet esta inativo"),
            entry("PJ083", HttpStatus.CONFLICT, "Nao e possivel agendar: veterinario inativo"),
            entry("PJ084", HttpStatus.CONFLICT, "Nao e possivel agendar: clinica do veterinario esta inativa"),
            entry("PJ085", HttpStatus.CONFLICT, "Nao e possivel criar prontuario para pet inativo"),
            entry("PJ086", HttpStatus.CONFLICT, "Nao e possivel criar prontuario para veterinario inativo"),
            entry("PJ087", HttpStatus.CONFLICT, "Nao e possivel prescrever medicamento para pet inativo"),
            entry("PJ088", HttpStatus.CONFLICT, "Nao e possivel prescrever medicamento com veterinario inativo"),
            entry("PJ090", HttpStatus.NOT_FOUND, "Tutor informado nao existe"),
            entry("PJ091", HttpStatus.NOT_FOUND, "Veterinario informado nao existe"),
            entry("PJ092", HttpStatus.NOT_FOUND, "Pet informado nao existe"),
            entry("PJ093", HttpStatus.NOT_FOUND, "Clinica informada nao existe"),
            entry("PJ094", HttpStatus.CONFLICT, "Nao e possivel criar conta vinculada a clinica inativa"),
            entry("PJ095", HttpStatus.CONFLICT, "Nao e possivel criar conta para tutor inativo"),
            entry("PJ096", HttpStatus.CONFLICT, "Nao e possivel criar conta para veterinario inativo"),
            entry("23502", HttpStatus.BAD_REQUEST, "Campo obrigatorio nao informado"),
            entry("23503", HttpStatus.CONFLICT, "Existem registros vinculados"),
            entry("23505", HttpStatus.CONFLICT, "Registro duplicado"),
            entry("23514", HttpStatus.BAD_REQUEST, "Valor invalido para as regras de integridade")
    );

    private SqlStateErrorMapper() {
    }

    public static ErrorMapping find(String sqlState) {
        return sqlState == null ? null : ERRORS.get(sqlState);
    }

    private static Map.Entry<String, ErrorMapping> entry(String sqlState, HttpStatus status, String message) {
        return Map.entry(sqlState, new ErrorMapping(status, message));
    }

    public record ErrorMapping(HttpStatus status, String message) {
    }
}
