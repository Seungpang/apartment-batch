package me.seungpang.apartmentbatch.core.repository;

import java.util.Optional;
import me.seungpang.apartmentbatch.core.entity.Apt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AptRepository extends JpaRepository<Apt, Long> {

    Optional<Apt> findAptByAptNameAndJibun(String aptName, String jibun);

}
