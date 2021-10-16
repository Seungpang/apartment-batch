package me.seungpang.apartmentbatch.core.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.seungpang.apartmentbatch.core.entity.Apt;
import me.seungpang.apartmentbatch.core.entity.AptDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AptDealRepository extends JpaRepository<AptDeal, Long> {

    Optional<AptDeal> findAptDealByAptAndExclusiveAreaAndDealDateAndDealAmountAndFloor(
        Apt apt, Double exclusiveArea, LocalDate dealDate, Long DealAmount, Integer floor
    );

    @Query("select ad from AptDeal ad join fetch ad.apt where ad.dealCanceled = 0 and ad.dealDate = ?1")
    List<AptDeal> findByDealCanceledIsFalseAndDealDateEquals(LocalDate dealDate);
}
