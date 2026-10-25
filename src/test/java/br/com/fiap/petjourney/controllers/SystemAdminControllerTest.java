package br.com.fiap.petjourney.controllers;

import br.com.fiap.petjourney.dtos.request.CreateClinicAdminRequest;
import br.com.fiap.petjourney.exceptions.ConflictOperationException;
import br.com.fiap.petjourney.exceptions.GlobalExceptionHandler;
import br.com.fiap.petjourney.services.SystemAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SystemAdminControllerTest {

    @Test
    void shouldReturnConflictWhenClinicAlreadyHasAdmin() throws Exception {
        SystemAdminService service = mock(SystemAdminService.class);
        when(service.createClinicAdmin(any(Long.class), any(CreateClinicAdminRequest.class)))
                .thenThrow(new ConflictOperationException("Esta clínica já possui um administrador."));
        MockMvc mockMvc = standaloneSetup(new SystemAdminController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/system/clinics/1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "outro@petfeliz.com",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Esta clínica já possui um administrador."))
                .andExpect(jsonPath("$.path").value("/system/clinics/1/admins"));
    }
}
