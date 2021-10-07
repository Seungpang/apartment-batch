package me.seungpang.apartmentbatch.core.repository;

import java.util.Optional;
import me.seungpang.apartmentbatch.core.entity.Lawd;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LawdRepository extends JpaRepository<Lawd,Long> {

    Optional<Lawd> findByLawdCd(String lawdCd);
}
