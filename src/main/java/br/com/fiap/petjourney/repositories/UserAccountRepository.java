package br.com.fiap.petjourney.repositories;

import br.com.fiap.petjourney.models.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<UserAccount> findByUsernameAndFirstAccessCode(String username, String firstAccessCode);
    Optional<UserAccount> findByTutorId(Long tutorId);
    Optional<UserAccount> findByVeterinarianId(Long veterinarianId);
}
