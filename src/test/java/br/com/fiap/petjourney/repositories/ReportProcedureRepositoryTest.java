package br.com.fiap.petjourney.repositories;

import br.com.fiap.petjourney.dtos.response.PetReportItemResponse;
import br.com.fiap.petjourney.dtos.response.PetWeightReportItemResponse;
import br.com.fiap.petjourney.exceptions.DatabaseProcedureException;
import br.com.fiap.petjourney.models.enums.PetSpecies;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportProcedureRepositoryTest {

    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private ReportProcedureRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(resultSet.next()).thenReturn(true);
        repository = new ReportProcedureRepository(new JdbcTemplate(dataSource), new ObjectMapper());
    }

    @Test
    void bindsClinicAndParsesSpecialCharacters() throws SQLException {
        String specialName = "Lua \"Azul\"\\\n\tç";
        String json = "[{\"petId\":11,\"tutorId\":12,\"clinicId\":13,"
                + "\"pet\":\"Lua \\\"Azul\\\"\\\\\\n\\tç\",\"tutor\":\"Ana\",\"clinica\":\"Clinica\"}]";
        when(connection.prepareStatement("CALL pkg_petjourney.prc_relatorio_pets_json(?, ?)"))
                .thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getString(1)).thenReturn(json);

        List<PetReportItemResponse> result = repository.findPets(13L);

        assertEquals(1, result.size());
        assertEquals(specialName, result.get(0).pet());
        assertEquals(13L, result.get(0).clinicId());
        verify(statement).setLong(1, 13L);
        verify(statement).setNull(2, Types.VARCHAR);
    }

    @Test
    void bindsNullSpeciesAndParsesNullFieldsAndTotals() throws SQLException {
        String json = "[{\"tipo\":\"SUBTOTAL\",\"species\":\"GATO\",\"sex\":null,\"pesoKg\":9.00},"
                + "{\"tipo\":\"TOTAL\",\"species\":null,\"sex\":null,\"pesoKg\":54.00}]";
        when(connection.prepareStatement("CALL pkg_petjourney.prc_relatorio_peso_pets(?, ?, ?)"))
                .thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getString(1)).thenReturn(json);

        List<PetWeightReportItemResponse> result = repository.findPetWeights(3L, null);

        assertEquals(2, result.size());
        assertEquals(PetWeightReportItemResponse.Type.TOTAL, result.get(1).tipo());
        assertNull(result.get(1).species());
        assertNull(result.get(1).sex());
        assertEquals("54.00", result.get(1).pesoKg().toPlainString());
        verify(statement).setLong(1, 3L);
        verify(statement).setNull(2, Types.VARCHAR);
        verify(statement).setNull(3, Types.VARCHAR);
    }

    @Test
    void bindsSpeciesAsEnumName() throws SQLException {
        when(connection.prepareStatement("CALL pkg_petjourney.prc_relatorio_peso_pets(?, ?, ?)"))
                .thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getString(1)).thenReturn("[]");

        repository.findPetWeights(8L, PetSpecies.GATO);

        verify(statement).setString(2, "GATO");
    }

    @Test
    void preservesSqlStateFromProcedureFailure() throws SQLException {
        when(connection.prepareStatement("CALL pkg_petjourney.prc_relatorio_peso_pets(?, ?, ?)"))
                .thenReturn(statement);
        when(statement.executeQuery()).thenThrow(new SQLException("especie invalida", "PJ057"));

        DatabaseProcedureException exception = assertThrows(
                DatabaseProcedureException.class,
                () -> repository.findPetWeights(8L, PetSpecies.GATO)
        );

        assertEquals("PJ057", exception.getSqlState());
    }
}
