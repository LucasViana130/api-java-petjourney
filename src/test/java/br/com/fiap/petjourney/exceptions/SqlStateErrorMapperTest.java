package br.com.fiap.petjourney.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SqlStateErrorMapperTest {

    @Test
    void mapsReportValidationAndConflictStates() {
        assertEquals(HttpStatus.BAD_REQUEST, SqlStateErrorMapper.find("PJ057").status());
        assertEquals(HttpStatus.CONFLICT, SqlStateErrorMapper.find("PJ055").status());
        assertEquals(HttpStatus.NOT_FOUND, SqlStateErrorMapper.find("PJ054").status());
    }

    @Test
    void mapsStandardIntegrityStates() {
        assertEquals(HttpStatus.BAD_REQUEST, SqlStateErrorMapper.find("23514").status());
        assertEquals(HttpStatus.CONFLICT, SqlStateErrorMapper.find("23505").status());
    }

    @Test
    void ignoresUnknownState() {
        assertNull(SqlStateErrorMapper.find("XX999"));
    }
}
