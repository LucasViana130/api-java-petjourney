package br.com.fiap.petjourney.repositories;

import br.com.fiap.petjourney.models.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {
    Page<Tutor> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("select distinct t from Tutor t where t.clinic.id = :clinicId")
    Page<Tutor> findClientsByClinicId(@Param("clinicId") Long clinicId, Pageable pageable);

    @Query("select distinct t from Tutor t where t.clinic.id = :clinicId and lower(t.name) like lower(concat('%', :name, '%'))")
    Page<Tutor> findClientsByClinicIdAndName(@Param("clinicId") Long clinicId, @Param("name") String name, Pageable pageable);

    @Query("select t from Tutor t where t.id = :id and t.clinic.id = :clinicId")
    Optional<Tutor> findClientByIdAndClinicId(@Param("id") Long id, @Param("clinicId") Long clinicId);
}
