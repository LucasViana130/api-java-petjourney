package br.com.fiap.petjourney.exceptions;

import lombok.Getter;

@Getter
public class DatabaseProcedureException extends RuntimeException {

    private final String sqlState;

    public DatabaseProcedureException(String sqlState, Throwable cause) {
        super("Falha ao executar rotina do banco", cause);
        this.sqlState = sqlState;
    }
}
