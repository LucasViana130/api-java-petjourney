package br.com.fiap.petjourney.repositories;

import br.com.fiap.petjourney.models.Veterinarian;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VeterinarianRepository extends JpaRepository<Veterinarian, Long> {
    Page<Veterinarian> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Veterinarian> findByClinicId(Long clinicId, Pageable pageable);
    Page<Veterinarian> findByClinicIdAndNameContainingIgnoreCase(Long clinicId, String name, Pageable pageable);
    Optional<Veterinarian> findByIdAndClinicId(Long id, Long clinicId);
}
