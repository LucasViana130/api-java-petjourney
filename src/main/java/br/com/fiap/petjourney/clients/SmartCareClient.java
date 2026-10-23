package br.com.fiap.petjourney.clients;

import br.com.fiap.petjourney.dtos.request.SmartCareAnalysisRequest;
import br.com.fiap.petjourney.dtos.response.SmartCareAnalysisResponse;
import br.com.fiap.petjourney.exceptions.SmartCareUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Objects;
import java.util.Set;

@Component
public class SmartCareClient {

    private static final Set<String> VALID_PRIORITIES = Set.of("BAIXA", "MEDIA", "ALTA");

    private final RestClient restClient;

    public SmartCareClient(@Qualifier("smartCareRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public SmartCareAnalysisResponse analyze(SmartCareAnalysisRequest request) {
        try {
            SmartCareAnalysisResponse response = restClient.post()
                    .uri("/smartcare/analisar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SmartCareAnalysisResponse.class);

            validateResponse(request, response);
            return response;
        } catch (SmartCareUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new SmartCareUnavailableException("Servico SmartCare indisponivel no momento", exception);
        }
    }

    private void validateResponse(SmartCareAnalysisRequest request, SmartCareAnalysisResponse response) {
        if (response == null
                || !Objects.equals(request.petId(), response.petId())
                || response.score() == null
                || response.score() < 0
                || response.score() > 100
                || !StringUtils.hasText(response.prioridade())
                || !VALID_PRIORITIES.contains(response.prioridade())
                || response.motivos() == null
                || response.motivos().isEmpty()
                || response.motivos().stream().anyMatch(motivo -> !StringUtils.hasText(motivo))
                || !StringUtils.hasText(response.recomendacao())) {
            throw new SmartCareUnavailableException("SmartCare retornou uma resposta invalida", null);
        }
    }
}
