package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.dtos.request.VeterinarianRequest;
import br.com.fiap.petjourney.dtos.response.VeterinarianResponse;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.exceptions.ResourceNotFoundException;
import br.com.fiap.petjourney.models.Clinic;
import br.com.fiap.petjourney.models.Veterinarian;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.ClinicRepository;
import br.com.fiap.petjourney.repositories.VeterinarianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VeterinarianService {

    private final VeterinarianRepository repository;
    private final ClinicRepository clinicRepository;
    private final AuthenticatedUserService authenticatedUser;

    public Page<VeterinarianResponse> findAll(String name, Pageable pageable) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.ADMIN_CLINICA || role == UserRole.VETERINARIO) {
            Long clinicId = authenticatedUser.clinicId();
            if (name != null && !name.isBlank()) {
                return repository.findByClinicIdAndNameContainingIgnoreCase(clinicId, name, pageable).map(VeterinarianResponse::fromEntity);
            }
            return repository.findByClinicId(clinicId, pageable).map(VeterinarianResponse::fromEntity);
        }

        if (name != null && !name.isBlank()) {
            return repository.findByNameContainingIgnoreCase(name, pageable).map(VeterinarianResponse::fromEntity);
        }

        return repository.findAll(pageable).map(VeterinarianResponse::fromEntity);
    }

    public VeterinarianResponse findById(Long id) {
        return VeterinarianResponse.fromEntity(findAccessibleVeterinarian(id));
    }

    public VeterinarianResponse create(VeterinarianRequest request) {
        assertAdminClinic();
        Long clinicId = authenticatedUser.clinicId();
        if (!request.clinicId().equals(clinicId)) {
            throw new ForbiddenOperationException("Administrador não pode cadastrar veterinário em outra clínica");
        }

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada"));

        return VeterinarianResponse.fromEntity(repository.save(new Veterinarian(request, clinic)));
    }

    public VeterinarianResponse update(Long id, VeterinarianRequest request) {
        assertAdminClinic();
        Veterinarian veterinarian = findAccessibleVeterinarian(id);

        if (!request.clinicId().equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Administrador não pode mover veterinário para outra clínica");
        }

        Clinic clinic = clinicRepository.findById(authenticatedUser.clinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada"));

        veterinarian.updateFrom(request, clinic);

        return VeterinarianResponse.fromEntity(repository.save(veterinarian));
    }

    public void delete(Long id) {
        assertAdminClinic();
        Veterinarian veterinarian = findAccessibleVeterinarian(id);
        repository.delete(veterinarian);
    }

    public Veterinarian findAccessibleVeterinarian(Long id) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.ADMIN_CLINICA || role == UserRole.VETERINARIO) {
            return repository.findByIdAndClinicId(id, authenticatedUser.clinicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Veterinário não encontrado para esta clínica"));
        }
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinário não encontrado"));
    }

    private void assertAdminClinic() {
        if (authenticatedUser.role() != UserRole.ADMIN_CLINICA) {
            throw new ForbiddenOperationException("Apenas administradores de clínica podem alterar veterinários");
        }
    }
}
