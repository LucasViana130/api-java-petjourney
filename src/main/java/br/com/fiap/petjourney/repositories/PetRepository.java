package br.com.fiap.petjourney.repositories;

import br.com.fiap.petjourney.models.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    Page<Pet> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Pet> findByTutorId(Long tutorId, Pageable pageable);
    Page<Pet> findByTutorIdAndNameContainingIgnoreCase(Long tutorId, String name, Pageable pageable);
    Optional<Pet> findByIdAndTutorId(Long id, Long tutorId);

    @Query("select distinct p from Pet p where p.tutor.clinic.id = :clinicId")
    Page<Pet> findPatientsByClinicId(@Param("clinicId") Long clinicId, Pageable pageable);

    @Query("select distinct p from Pet p where p.tutor.clinic.id = :clinicId and lower(p.name) like lower(concat('%', :name, '%'))")
    Page<Pet> findPatientsByClinicIdAndName(@Param("clinicId") Long clinicId, @Param("name") String name, Pageable pageable);

    @Query("select p from Pet p where p.id = :id and p.tutor.clinic.id = :clinicId")
    Optional<Pet> findPatientByIdAndClinicId(@Param("id") Long id, @Param("clinicId") Long clinicId);
}
