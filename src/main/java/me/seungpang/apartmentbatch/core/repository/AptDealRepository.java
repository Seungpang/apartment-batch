package me.seungpang.apartmentbatch.core.repository;

import java.time.LocalDate;
import java.util.Optional;
import me.seungpang.apartmentbatch.core.entity.Apt;
import me.seungpang.apartmentbatch.core.entity.AptDeal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AptDealRepository extends JpaRepository<AptDeal, Long> {

    Optional<AptDeal> findAptDealByAptAndExclusiveAreaAndDealDateAndDealAmountAndFloor(
        Apt apt, Double exclusiveArea, LocalDate dealDate, Long DealAmount, Integer floor
    );
}
