package br.com.fiap.petjourney.clients;

import br.com.fiap.petjourney.dtos.request.SmartCareAnalysisRequest;
import br.com.fiap.petjourney.exceptions.SmartCareUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmartCareClientTest {

    @Test
    void shouldPostSmartCareContractAndDeserializeResponse() {
        RestClient.Builder builder = smartCareBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SmartCareClient client = new SmartCareClient(builder.build());
        SmartCareAnalysisRequest request = request();

        server.expect(requestTo("http://localhost:8001/smartcare/analisar"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "petId": 123,
                          "dataUltimoAtendimento": "2025-08-06",
                          "possuiProximoAgendamento": false,
                          "possuiTratamentoRegistrado": true
                        }
                        """))
                .andRespond(withSuccess(validResponse(), MediaType.APPLICATION_JSON));

        var response = client.analyze(request);

        assertEquals(123L, response.petId());
        assertEquals(Integer.valueOf(100), response.score());
        assertEquals("ALTA", response.prioridade());
        assertEquals(List.of("Sem consulta há mais de 365 dias"), response.motivos());
        server.verify();
    }

    @Test
    void shouldAcceptLowPriorityResponseWithoutReasons() {
        RestClient.Builder builder = smartCareBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SmartCareClient client = new SmartCareClient(builder.build());

        server.expect(requestTo("http://localhost:8001/smartcare/analisar"))
                .andRespond(withSuccess("""
                        {
                          "petId": 123,
                          "score": 0,
                          "prioridade": "BAIXA",
                          "motivos": [],
                          "recomendacao": "Manter acompanhamento conforme planejamento atual"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.analyze(request());

        assertEquals(Integer.valueOf(0), response.score());
        assertEquals("BAIXA", response.prioridade());
        assertEquals(List.of(), response.motivos());
        server.verify();
    }

    @Test
    void shouldReturnControlledExceptionWhenSmartCareIsOffline() {
        assertTransportFailure(new IOException("Connection refused"));
    }

    @Test
    void shouldReturnControlledExceptionOnTimeout() {
        assertTransportFailure(new SocketTimeoutException("Read timed out"));
    }

    @Test
    void shouldReturnControlledExceptionOnExternalHttpError() {
        RestClient.Builder builder = smartCareBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SmartCareClient client = new SmartCareClient(builder.build());

        server.expect(requestTo("http://localhost:8001/smartcare/analisar"))
                .andRespond(withServerError());

        SmartCareUnavailableException exception = assertThrows(
                SmartCareUnavailableException.class,
                () -> client.analyze(request())
        );

        assertEquals("Servico SmartCare indisponivel no momento", exception.getMessage());
        server.verify();
    }

    @Test
    void shouldRejectResponseForAnotherPet() {
        assertInvalidResponse(validResponse().replace("\"petId\": 123", "\"petId\": 999"));
    }

    @Test
    void shouldRejectScoreOutsideExpectedRange() {
        assertInvalidResponse(validResponse().replace("\"score\": 100", "\"score\": 101"));
    }

    @Test
    void shouldRejectMissingScore() {
        assertInvalidResponse(validResponse().replace("\"score\": 100,", ""));
    }

    @Test
    void shouldRejectUnknownPriority() {
        assertInvalidResponse(validResponse().replace("\"ALTA\"", "\"URGENTE\""));
    }

    @Test
    void shouldRejectMissingPriority() {
        assertInvalidResponse(validResponse().replace("\"prioridade\": \"ALTA\",", ""));
    }

    @Test
    void shouldRejectNullReasons() {
        assertInvalidResponse(validResponse().replace(
                "[\"Sem consulta há mais de 365 dias\"]",
                "null"
        ));
    }

    @Test
    void shouldRejectNullReason() {
        assertInvalidResponse(validResponse().replace(
                "[\"Sem consulta há mais de 365 dias\"]",
                "[null]"
        ));
    }

    @Test
    void shouldRejectBlankReason() {
        assertInvalidResponse(validResponse().replace(
                "[\"Sem consulta há mais de 365 dias\"]",
                "[\"  \"]"
        ));
    }

    @Test
    void shouldRejectBlankRecommendation() {
        assertInvalidResponse(validResponse().replace("Recomendacao de teste", " "));
    }

    private void assertTransportFailure(IOException failure) {
        RestClient.Builder builder = smartCareBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SmartCareClient client = new SmartCareClient(builder.build());

        server.expect(requestTo("http://localhost:8001/smartcare/analisar"))
                .andRespond(httpRequest -> {
                    throw failure;
                });

        SmartCareUnavailableException exception = assertThrows(
                SmartCareUnavailableException.class,
                () -> client.analyze(request())
        );

        assertEquals("Servico SmartCare indisponivel no momento", exception.getMessage());
        server.verify();
    }

    private void assertInvalidResponse(String responseBody) {
        RestClient.Builder builder = smartCareBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SmartCareClient client = new SmartCareClient(builder.build());

        server.expect(requestTo("http://localhost:8001/smartcare/analisar"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        SmartCareUnavailableException exception = assertThrows(
                SmartCareUnavailableException.class,
                () -> client.analyze(request())
        );

        assertEquals("SmartCare retornou uma resposta invalida", exception.getMessage());
        server.verify();
    }

    private SmartCareAnalysisRequest request() {
        return new SmartCareAnalysisRequest(123L, LocalDate.of(2025, 8, 6), false, true);
    }

    private String validResponse() {
        return """
                {
                  "petId": 123,
                  "score": 100,
                  "prioridade": "ALTA",
                  "motivos": ["Sem consulta há mais de 365 dias"],
                  "recomendacao": "Recomendacao de teste"
                }
                """;
    }

    private RestClient.Builder smartCareBuilder() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return RestClient.builder()
                .baseUrl("http://localhost:8001")
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                });
    }
}
