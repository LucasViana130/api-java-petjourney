package br.com.fiap.petjourney.controllers;

import br.com.fiap.petjourney.dtos.response.SmartCareAnalysisResponse;
import br.com.fiap.petjourney.dtos.response.SmartCareRankingResponse;
import br.com.fiap.petjourney.exceptions.GlobalExceptionHandler;
import br.com.fiap.petjourney.exceptions.SmartCareDataUnavailableException;
import br.com.fiap.petjourney.exceptions.SmartCareUnavailableException;
import br.com.fiap.petjourney.services.SmartCareService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SmartCareControllerTest {

    @Test
    void shouldReturnStructuredAnalysis() throws Exception {
        SmartCareService service = mock(SmartCareService.class);
        when(service.analyze(1L)).thenReturn(new SmartCareAnalysisResponse(
                1L, 50, "MEDIA", List.of("Motivo"), "Recomendacao"
        ));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/smartcare/pets/1/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(1))
                .andExpect(jsonPath("$.score").value(50))
                .andExpect(jsonPath("$.prioridade").value("MEDIA"))
                .andExpect(jsonPath("$.motivos[0]").value("Motivo"))
                .andExpect(jsonPath("$.recomendacao").value("Recomendacao"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenPetHasNoClinicalData() throws Exception {
        SmartCareService service = mock(SmartCareService.class);
        when(service.analyze(2L)).thenThrow(
                new SmartCareDataUnavailableException("Pet nao possui atendimento clinico registrado para analise SmartCare")
        );
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/smartcare/pets/2/analysis"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void shouldReturnServiceUnavailableWhenSmartCareIsOffline() throws Exception {
        SmartCareService service = mock(SmartCareService.class);
        when(service.ranking()).thenThrow(
                new SmartCareUnavailableException("Servico SmartCare indisponivel no momento", null)
        );
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/smartcare/ranking"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Servico SmartCare indisponivel no momento"));
    }

    @Test
    void shouldReturnEmptyStructuredRanking() throws Exception {
        SmartCareService service = mock(SmartCareService.class);
        when(service.ranking()).thenReturn(new SmartCareRankingResponse(List.of(), List.of()));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/smartcare/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analises").isArray())
                .andExpect(jsonPath("$.semDadosSuficientes").isArray());
    }

    private MockMvc mockMvc(SmartCareService service) {
        return standaloneSetup(new SmartCareController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
