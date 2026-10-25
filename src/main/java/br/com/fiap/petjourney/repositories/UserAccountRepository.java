package br.com.fiap.petjourney.repositories;

import br.com.fiap.petjourney.models.UserAccount;
import br.com.fiap.petjourney.models.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByClinicIdAndRole(Long clinicId, UserRole role);

    @Query("select distinct u.clinic.id from UserAccount u " +
            "where u.clinic.id in :clinicIds and u.role = :role")
    Set<Long> findClinicIdsByRole(
            @Param("clinicIds") Collection<Long> clinicIds,
            @Param("role") UserRole role
    );

    Optional<UserAccount> findByUsernameAndFirstAccessCode(String username, String firstAccessCode);
    Optional<UserAccount> findByTutorId(Long tutorId);
    Optional<UserAccount> findByVeterinarianId(Long veterinarianId);

    @Modifying
    @Query("update UserAccount u set u.active = false where u.clinic.id = :clinicId")
    void deactivateClinicAccounts(@Param("clinicId") Long clinicId);

    @Modifying
    @Query("update UserAccount u set u.active = false where u.tutor.id = :tutorId")
    void deactivateTutorAccount(@Param("tutorId") Long tutorId);

    @Modifying
    @Query("update UserAccount u set u.active = false where u.veterinarian.id = :veterinarianId")
    void deactivateVeterinarianAccount(@Param("veterinarianId") Long veterinarianId);

    @Modifying
    @Query("update UserAccount u set u.active = false where u.tutor.id in (select t.id from Tutor t where t.clinic.id = :clinicId)")
    void deactivateTutorAccountsByClinicId(@Param("clinicId") Long clinicId);

    @Modifying
    @Query("update UserAccount u set u.active = false where u.veterinarian.id in (select v.id from Veterinarian v where v.clinic.id = :clinicId)")
    void deactivateVeterinarianAccountsByClinicId(@Param("clinicId") Long clinicId);
}
