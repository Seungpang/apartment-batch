package me.seungpang.apartmentbatch.core.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import me.seungpang.apartmentbatch.core.dto.AptDealDto;
import me.seungpang.apartmentbatch.core.dto.AptDto;
import me.seungpang.apartmentbatch.core.entity.Apt;
import me.seungpang.apartmentbatch.core.entity.AptDeal;
import me.seungpang.apartmentbatch.core.repository.AptDealRepository;
import me.seungpang.apartmentbatch.core.repository.AptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AptDealDto에 있는 것을 Apt, AptDeal 엔티티로 저장한다.
 */
@AllArgsConstructor
@Service
public class AptDealService {

    private final AptRepository aptRepository;
    private final AptDealRepository aptDealRepository;

    @Transactional
    public void upsert(AptDealDto dto) {

        Apt apt = getAptOnNew(dto);
        saveAptDeal(dto, apt);
    }

    private Apt getAptOnNew(AptDealDto dto) {
        Apt apt = aptRepository.findAptByAptNameAndJibun(dto.getAptName(), dto.getJibun())
            .orElseGet(() -> Apt.from(dto));
        return aptRepository.save(apt);
    }

    private void saveAptDeal(AptDealDto dto, Apt apt) {
        AptDeal aptDeal = aptDealRepository.findAptDealByAptAndExclusiveAreaAndDealDateAndDealAmountAndFloor(
                apt, dto.getExclusiveArea(), dto.getDealDate(), dto.getDealAmount(), dto.getFloor())
            .orElseGet(() -> AptDeal.of(dto, apt));
        aptDeal.setDealCanceled(dto.isDealCanceled());
        aptDeal.setDealCanceledDate(dto.getDealCanceledDate());
        aptDealRepository.save(aptDeal);
    }

    public List<AptDto> findyByGuLawdCdAndDealDate(String guLawdCd, LocalDate dealDate) {
        return aptDealRepository.findByDealCanceledIsFalseAndDealDateEquals(dealDate)
            .stream()
            .filter(aptDeal -> aptDeal.getApt().getGuLawdCd().equals(guLawdCd))
            .map(aptDeal -> new AptDto(aptDeal.getApt().getAptName(), aptDeal.getDealAmount()))
            .collect(Collectors.toList());
    }
}
