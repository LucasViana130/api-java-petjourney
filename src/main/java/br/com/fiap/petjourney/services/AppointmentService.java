package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.dtos.request.AppointmentRequest;
import br.com.fiap.petjourney.dtos.request.ScheduleAppointmentRequest;
import br.com.fiap.petjourney.dtos.response.AppointmentResponse;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.exceptions.ResourceNotFoundException;
import br.com.fiap.petjourney.models.Appointment;
import br.com.fiap.petjourney.models.Clinic;
import br.com.fiap.petjourney.models.Pet;
import br.com.fiap.petjourney.models.Veterinarian;
import br.com.fiap.petjourney.models.enums.AppointmentStatus;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.AppointmentRepository;
import br.com.fiap.petjourney.repositories.ClinicRepository;
import br.com.fiap.petjourney.repositories.PetRepository;
import br.com.fiap.petjourney.repositories.VeterinarianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PetRepository petRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final ClinicRepository clinicRepository;
    private final VeterinarianAvailabilityService availabilityService;
    private final AuthenticatedUserService authenticatedUser;
    private final EmailService emailService;

    public Page<AppointmentResponse> findByPetId(Long petId, Pageable pageable) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.TUTOR) {
            return repository.findByPetIdAndPetTutorId(petId, authenticatedUser.tutorId(), pageable)
                    .map(AppointmentResponse::fromEntity);
        }
        if (role == UserRole.ADMIN_CLINICA) {
            return repository.findByPetIdAndClinicId(petId, authenticatedUser.clinicId(), pageable)
                    .map(AppointmentResponse::fromEntity);
        }
        if (role == UserRole.VETERINARIO) {
            return repository.findByPetIdAndVeterinarianId(petId, authenticatedUser.veterinarianId(), pageable)
                    .map(AppointmentResponse::fromEntity);
        }
        return repository.findByPetId(petId, pageable).map(AppointmentResponse::fromEntity);
    }

    public Page<AppointmentResponse> findByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.TUTOR) {
            return repository.findByPetTutorIdAndDateTimeBetween(authenticatedUser.tutorId(), start, end, pageable)
                    .map(AppointmentResponse::fromEntity);
        }
        if (role == UserRole.ADMIN_CLINICA) {
            return repository.findByClinicIdAndDateTimeBetween(authenticatedUser.clinicId(), start, end, pageable)
                    .map(AppointmentResponse::fromEntity);
        }
        if (role == UserRole.VETERINARIO) {
            return repository.findByVeterinarianIdAndDateTimeBetween(authenticatedUser.veterinarianId(), start, end, pageable)
                    .map(AppointmentResponse::fromEntity);
        }
        return repository.findByDateTimeBetween(start, end, pageable).map(AppointmentResponse::fromEntity);
    }

    public AppointmentResponse findById(Long id) {
        return AppointmentResponse.fromEntity(findAccessibleAppointment(id));
    }

    public AppointmentResponse create(AppointmentRequest request) {
        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado"));

        Veterinarian veterinarian = veterinarianRepository.findById(request.veterinarianId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinário não encontrado"));

        Clinic clinic = request.clinicId() != null
                ? clinicRepository.findById(request.clinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada"))
                : null;

        assertAppointmentAccess(pet, veterinarian, clinic);
        validateAvailableSlot(veterinarian.getId(), request.dateTime());
        validateScheduleConflict(veterinarian.getId(), request.dateTime(), null);

        if (request.status() == null) {
            request = new AppointmentRequest(
                    request.title(),
                    request.description(),
                    request.dateTime(),
                    AppointmentStatus.PENDENTE,
                    request.petId(),
                    request.veterinarianId(),
                    request.clinicId()
            );
        }

        return AppointmentResponse.fromEntity(repository.save(new Appointment(request, pet, veterinarian, clinic)));
    }

    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = findAccessibleAppointment(id);

        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado"));

        Veterinarian veterinarian = veterinarianRepository.findById(request.veterinarianId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinário não encontrado"));

        Clinic clinic = request.clinicId() != null
                ? clinicRepository.findById(request.clinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada"))
                : null;

        assertAppointmentAccess(pet, veterinarian, clinic);
        validateAvailableSlot(veterinarian.getId(), request.dateTime());
        validateScheduleConflict(veterinarian.getId(), request.dateTime(), id);

        appointment.updateFrom(request, pet, veterinarian, clinic);
        return AppointmentResponse.fromEntity(repository.save(appointment));
    }

    public AppointmentResponse complete(Long id) {
        Appointment appointment = findAccessibleAppointment(id);
        UserRole role = authenticatedUser.role();
        if (role != UserRole.ADMIN_CLINICA && role != UserRole.VETERINARIO) {
            throw new ForbiddenOperationException("Apenas clínica ou veterinário podem marcar consulta como concluída");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new ForbiddenOperationException("Não é possível concluir agendamento cancelado");
        }

        appointment.setStatus(AppointmentStatus.REALIZADO);
        return AppointmentResponse.fromEntity(repository.save(appointment));
    }

    public AppointmentResponse cancel(Long id) {
        Appointment appointment = findAccessibleAppointment(id);
        UserRole role = authenticatedUser.role();
        if (role != UserRole.TUTOR && role != UserRole.ADMIN_CLINICA && role != UserRole.VETERINARIO) {
            throw new ForbiddenOperationException("Perfil sem permissão para cancelar consulta");
        }
        if (appointment.getStatus() == AppointmentStatus.REALIZADO) {
            throw new ForbiddenOperationException("Não é possível cancelar consulta já concluída");
        }
        if (!appointment.getDateTime().isAfter(LocalDateTime.now().plusHours(24))) {
            throw new ForbiddenOperationException("Cancelamento permitido apenas com mais de 24 horas de antecedência");
        }

        appointment.setStatus(AppointmentStatus.CANCELADO);
        Appointment saved = repository.save(appointment);
        notifyCancellation(saved, role);
        return AppointmentResponse.fromEntity(saved);
    }

    public void delete(Long id) {
        Appointment appointment = findAccessibleAppointment(id);
        UserRole role = authenticatedUser.role();
        if (role != UserRole.ADMIN_CLINICA && role != UserRole.VETERINARIO) {
            throw new ForbiddenOperationException("Perfil sem permissao para excluir agendamento");
        }
        repository.delete(appointment);
    }

    public AppointmentResponse schedule(ScheduleAppointmentRequest request) {
        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado"));
        Veterinarian veterinarian = veterinarianRepository.findById(request.veterinarianId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinário não encontrado"));

        Long clinicId = resolveClinicIdForSchedule(request, veterinarian);
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada"));

        assertAppointmentAccess(pet, veterinarian, clinic);
        validateAvailableSlot(veterinarian.getId(), request.dateTime());
        validateScheduleConflict(veterinarian.getId(), request.dateTime(), null);

        Appointment appointment = Appointment.builder()
                .title(request.title())
                .description(request.description())
                .dateTime(request.dateTime())
                .status(AppointmentStatus.PENDENTE)
                .pet(pet)
                .veterinarian(veterinarian)
                .clinic(clinic)
                .build();

        return AppointmentResponse.fromEntity(repository.save(appointment));
    }

    public Appointment findAccessibleAppointment(Long id) {
        Appointment appointment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        UserRole role = authenticatedUser.role();
        if (role == UserRole.TUTOR && !appointment.getPet().getTutor().getId().equals(authenticatedUser.tutorId())) {
            throw new ForbiddenOperationException("Tutor não pode acessar agendamento de outro tutor");
        }
        if (role == UserRole.ADMIN_CLINICA && !appointment.getClinic().getId().equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Administrador não pode acessar agendamento de outra clínica");
        }
        if (role == UserRole.VETERINARIO && !appointment.getVeterinarian().getId().equals(authenticatedUser.veterinarianId())) {
            throw new ForbiddenOperationException("Veterinário não pode acessar agendamento de outro veterinário");
        }

        return appointment;
    }

    private Long resolveClinicIdForSchedule(ScheduleAppointmentRequest request, Veterinarian veterinarian) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.ADMIN_CLINICA || role == UserRole.VETERINARIO) {
            return authenticatedUser.clinicId();
        }
        if (request.clinicId() == null) {
            throw new ForbiddenOperationException("A clínica é obrigatória para agendamento feito pelo tutor");
        }
        if (veterinarian.getClinic() == null || !veterinarian.getClinic().getId().equals(request.clinicId())) {
            throw new ForbiddenOperationException("Veterinário não pertence à clínica selecionada");
        }
        return request.clinicId();
    }

    private void assertAppointmentAccess(Pet pet, Veterinarian veterinarian, Clinic clinic) {
        if (clinic == null) {
            throw new ForbiddenOperationException("Agendamento deve estar vinculado a uma clínica");
        }
        if (veterinarian.getClinic() == null || !veterinarian.getClinic().getId().equals(clinic.getId())) {
            throw new ForbiddenOperationException("Veterinário não pertence à clínica do agendamento");
        }

        UserRole role = authenticatedUser.role();
        if (role == UserRole.ADMIN_SISTEMA) {
            throw new ForbiddenOperationException("Administrador do sistema não realiza agendamentos clínicos");
        }
        if (role == UserRole.TUTOR && !pet.getTutor().getId().equals(authenticatedUser.tutorId())) {
            throw new ForbiddenOperationException("Tutor não pode agendar para pet de outro tutor");
        }
        if (role == UserRole.TUTOR && !clinic.getId().equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Tutor não pode agendar em clínica sem vínculo");
        }
        if (role == UserRole.ADMIN_CLINICA && !clinic.getId().equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Administrador não pode agendar em outra clínica");
        }
        if ((role == UserRole.ADMIN_CLINICA || role == UserRole.VETERINARIO)
                && (pet.getTutor().getClinic() == null || !clinic.getId().equals(pet.getTutor().getClinic().getId()))) {
            throw new ForbiddenOperationException("Pet não pertence à carteira de clientes desta clínica");
        }
        if (role == UserRole.VETERINARIO && !veterinarian.getId().equals(authenticatedUser.veterinarianId())) {
            throw new ForbiddenOperationException("Veterinário só pode criar agendamento para si mesmo");
        }
    }

    private void validateScheduleConflict(Long veterinarianId, LocalDateTime dateTime, Long ignoredId) {
        if (repository.existsScheduleConflict(veterinarianId, dateTime, ignoredId)) {
            throw new ForbiddenOperationException("Veterinário já possui agendamento neste horário");
        }
    }

    private void validateAvailableSlot(Long veterinarianId, LocalDateTime dateTime) {
        if (!availabilityService.isSlotAvailable(veterinarianId, dateTime)) {
            throw new ForbiddenOperationException("Horário não está disponível para este veterinário");
        }
    }

    private void notifyCancellation(Appointment appointment, UserRole cancelledBy) {
        if (cancelledBy == UserRole.TUTOR) {
            emailService.sendAppointmentCancellationToClinicOrVet(appointment, cancelledBy);
            return;
        }
        if (cancelledBy == UserRole.VETERINARIO || cancelledBy == UserRole.ADMIN_CLINICA) {
            emailService.sendAppointmentCancellationToTutor(appointment, cancelledBy);
        }
    }
}
