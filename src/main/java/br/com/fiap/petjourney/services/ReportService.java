package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.dtos.response.PetReportItemResponse;
import br.com.fiap.petjourney.dtos.response.PetWeightReportItemResponse;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.models.enums.PetSpecies;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.ReportProcedureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportProcedureRepository repository;
    private final AuthenticatedUserService authenticatedUser;

    @Transactional(readOnly = true)
    public List<PetReportItemResponse> pets() {
        return repository.findPets(authenticatedClinicId());
    }

    @Transactional(readOnly = true)
    public List<PetWeightReportItemResponse> petWeights(PetSpecies species) {
        return repository.findPetWeights(authenticatedClinicId(), species);
    }

    private Long authenticatedClinicId() {
        if (authenticatedUser.role() != UserRole.ADMIN_CLINICA) {
            throw new ForbiddenOperationException("Apenas o administrador da clinica pode acessar relatorios");
        }

        Long clinicId = authenticatedUser.clinicId();
        if (clinicId == null) {
            throw new ForbiddenOperationException("Usuario autenticado nao possui clinica vinculada");
        }
        return clinicId;
    }
}
