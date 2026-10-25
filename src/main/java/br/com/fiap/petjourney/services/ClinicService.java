package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.dtos.request.ClinicRequest;
import br.com.fiap.petjourney.dtos.response.ClinicResponse;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.exceptions.ResourceNotFoundException;
import br.com.fiap.petjourney.models.Clinic;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.ClinicRepository;
import br.com.fiap.petjourney.repositories.PetRepository;
import br.com.fiap.petjourney.repositories.TutorRepository;
import br.com.fiap.petjourney.repositories.UserAccountRepository;
import br.com.fiap.petjourney.repositories.VeterinarianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository repository;
    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthenticatedUserService authenticatedUser;

    public Page<ClinicResponse> findAll(Pageable pageable) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.ADMIN_CLINICA || role == UserRole.VETERINARIO) {
            Clinic clinic = repository.findByIdAndActiveTrue(authenticatedUser.clinicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Clinica autenticada nao encontrada"));
            return new PageImpl<>(toResponses(List.of(clinic)), pageable, 1);
        }
        if (role == UserRole.TUTOR) {
            List<ClinicResponse> clinics = toResponses(repository.findByTutorId(authenticatedUser.tutorId()));
            return new PageImpl<>(clinics, pageable, clinics.size());
        }

        Page<Clinic> clinics = repository.findByActiveTrue(pageable);
        Set<Long> clinicIdsWithAdmin = findClinicIdsWithAdmin(clinics.getContent());
        return clinics.map(clinic -> ClinicResponse.fromEntity(
                clinic,
                clinicIdsWithAdmin.contains(clinic.getId())
        ));
    }

    public ClinicResponse findById(Long id) {
        assertClinicAccess(id);
        Clinic clinic = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada"));
        boolean hasAdmin = userAccountRepository.existsByClinicIdAndRole(id, UserRole.ADMIN_CLINICA);
        return ClinicResponse.fromEntity(clinic, hasAdmin);
    }

    public ClinicResponse create(ClinicRequest request) {
        if (authenticatedUser.role() != UserRole.ADMIN_SISTEMA) {
            throw new ForbiddenOperationException("Apenas administradores do sistema podem cadastrar clinicas");
        }
        return ClinicResponse.fromEntity(repository.save(new Clinic(request)), false);
    }

    public ClinicResponse update(Long id, ClinicRequest request) {
        assertSystemAdmin();
        assertClinicAccess(id);
        Clinic clinic = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada"));

        clinic.updateFrom(request);

        boolean hasAdmin = userAccountRepository.existsByClinicIdAndRole(id, UserRole.ADMIN_CLINICA);
        return ClinicResponse.fromEntity(repository.save(clinic), hasAdmin);
    }

    @Transactional
    public void delete(Long id) {
        assertSystemAdmin();
        assertClinicAccess(id);

        Clinic clinic = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada"));

        userAccountRepository.deactivateClinicAccounts(id);
        userAccountRepository.deactivateTutorAccountsByClinicId(id);
        userAccountRepository.deactivateVeterinarianAccountsByClinicId(id);
        petRepository.deactivateByClinicId(id);
        tutorRepository.deactivateByClinicId(id);
        veterinarianRepository.deactivateByClinicId(id);

        clinic.setActive(false);
        repository.save(clinic);
    }

    private void assertClinicAccess(Long clinicId) {
        UserRole role = authenticatedUser.role();
        if ((role == UserRole.ADMIN_CLINICA || role == UserRole.VETERINARIO)
                && !clinicId.equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Usuario nao pode acessar dados de outra clinica");
        }
        if (role == UserRole.TUTOR && !clinicId.equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Tutor nao pode acessar clinica sem vinculo");
        }
    }

    private List<ClinicResponse> toResponses(List<Clinic> clinics) {
        Set<Long> clinicIdsWithAdmin = findClinicIdsWithAdmin(clinics);
        return clinics.stream()
                .map(clinic -> ClinicResponse.fromEntity(
                        clinic,
                        clinicIdsWithAdmin.contains(clinic.getId())
                ))
                .toList();
    }

    private Set<Long> findClinicIdsWithAdmin(List<Clinic> clinics) {
        if (clinics.isEmpty()) {
            return Set.of();
        }

        List<Long> clinicIds = clinics.stream()
                .map(Clinic::getId)
                .toList();
        return userAccountRepository.findClinicIdsByRole(clinicIds, UserRole.ADMIN_CLINICA);
    }

    private void assertSystemAdmin() {
        if (authenticatedUser.role() != UserRole.ADMIN_SISTEMA) {
            throw new ForbiddenOperationException("Apenas administradores do sistema podem alterar clinicas");
        }
    }
}
