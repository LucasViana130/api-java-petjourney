package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.dtos.request.ClinicRequest;
import br.com.fiap.petjourney.dtos.response.ClinicResponse;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.exceptions.ResourceNotFoundException;
import br.com.fiap.petjourney.models.Clinic;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository repository;
    private final AuthenticatedUserService authenticatedUser;

    public Page<ClinicResponse> findAll(Pageable pageable) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.ADMIN_CLINICA || role == UserRole.VETERINARIO) {
            Clinic clinic = repository.findById(authenticatedUser.clinicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Clínica autenticada não encontrada"));
            return new PageImpl<>(List.of(ClinicResponse.fromEntity(clinic)), pageable, 1);
        }
        if (role == UserRole.TUTOR) {
            List<ClinicResponse> clinics = repository.findByTutorId(authenticatedUser.tutorId())
                    .stream()
                    .map(ClinicResponse::fromEntity)
                    .toList();
            return new PageImpl<>(clinics, pageable, clinics.size());
        }

        return repository.findAll(pageable).map(ClinicResponse::fromEntity);
    }

    public ClinicResponse findById(Long id) {
        assertClinicAccess(id);
        return repository.findById(id)
                .map(ClinicResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada"));
    }

    public ClinicResponse create(ClinicRequest request) {
        if (authenticatedUser.role() != UserRole.ADMIN_SISTEMA) {
            throw new ForbiddenOperationException("Apenas administradores do sistema podem cadastrar clínicas");
        }
        return ClinicResponse.fromEntity(repository.save(new Clinic(request)));
    }

    public ClinicResponse update(Long id, ClinicRequest request) {
        assertSystemAdmin();
        assertClinicAccess(id);
        Clinic clinic = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada"));

        clinic.updateFrom(request);

        return ClinicResponse.fromEntity(repository.save(clinic));
    }

    public void delete(Long id) {
        assertSystemAdmin();
        assertClinicAccess(id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Clínica não encontrada");
        }

        repository.deleteById(id);
    }

    private void assertClinicAccess(Long clinicId) {
        UserRole role = authenticatedUser.role();
        if ((role == UserRole.ADMIN_CLINICA || role == UserRole.VETERINARIO)
                && !clinicId.equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Usuário não pode acessar dados de outra clínica");
        }
        if (role == UserRole.TUTOR && !clinicId.equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Tutor não pode acessar clínica sem vínculo");
        }
    }

    private void assertSystemAdmin() {
        if (authenticatedUser.role() != UserRole.ADMIN_SISTEMA) {
            throw new ForbiddenOperationException("Apenas administradores do sistema podem alterar clínicas");
        }
    }
}
