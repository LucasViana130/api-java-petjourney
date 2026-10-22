package br.com.fiap.petjourney.repositories;

import br.com.fiap.petjourney.dtos.response.PetReportItemResponse;
import br.com.fiap.petjourney.dtos.response.PetWeightReportItemResponse;
import br.com.fiap.petjourney.exceptions.DatabaseProcedureException;
import br.com.fiap.petjourney.models.enums.PetSpecies;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportProcedureRepository {

    private static final String PETS_REPORT_SQL =
            "CALL pkg_petjourney.prc_relatorio_pets_json(?, ?)";
    private static final String WEIGHT_REPORT_SQL =
            "CALL pkg_petjourney.prc_relatorio_peso_pets(?, ?, ?)";
    private static final TypeReference<List<PetReportItemResponse>> PETS_REPORT_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<PetWeightReportItemResponse>> WEIGHT_REPORT_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<PetReportItemResponse> findPets(Long clinicId) {
        return readJson(callPetsReport(clinicId), PETS_REPORT_TYPE);
    }

    public List<PetWeightReportItemResponse> findPetWeights(Long clinicId, PetSpecies species) {
        return readJson(callWeightReport(clinicId, species), WEIGHT_REPORT_TYPE);
    }

    private String callPetsReport(Long clinicId) {
        return executeCall(PETS_REPORT_SQL, statement -> {
            setLong(statement, 1, clinicId);
            statement.setNull(2, Types.VARCHAR);
        });
    }

    private String callWeightReport(Long clinicId, PetSpecies species) {
        return executeCall(WEIGHT_REPORT_SQL, statement -> {
            setLong(statement, 1, clinicId);
            if (species == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, species.name());
            }
            statement.setNull(3, Types.VARCHAR);
        });
    }

    private String executeCall(String sql, StatementBinder binder) {
        try {
            return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    binder.bind(statement);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Procedure de relatorio nao retornou resultado");
                        }
                        return resultSet.getString(1);
                    }
                }
            });
        } catch (DataAccessException exception) {
            SQLException sqlException = findSqlException(exception);
            if (sqlException != null) {
                throw new DatabaseProcedureException(sqlException.getSQLState(), exception);
            }
            throw exception;
        }
    }

    private void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private <T> T readJson(String json, TypeReference<T> responseType) {
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Resposta invalida da procedure de relatorio", exception);
        }
    }

    private SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
