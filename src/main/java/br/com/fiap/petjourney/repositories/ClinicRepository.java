package br.com.fiap.petjourney.repositories;

import br.com.fiap.petjourney.models.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    @Query("select t.clinic from Tutor t where t.id = :tutorId")
    List<Clinic> findByTutorId(@Param("tutorId") Long tutorId);
}
