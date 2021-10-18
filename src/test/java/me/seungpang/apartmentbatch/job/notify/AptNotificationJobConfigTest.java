package me.seungpang.apartmentbatch.job.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import me.seungpang.apartmentbatch.BatchTestConfig;
import me.seungpang.apartmentbatch.adapter.FakeSendService;
import me.seungpang.apartmentbatch.core.dto.AptDto;
import me.seungpang.apartmentbatch.core.entity.AptNotification;
import me.seungpang.apartmentbatch.core.entity.Lawd;
import me.seungpang.apartmentbatch.core.repository.AptNotificationRepository;
import me.seungpang.apartmentbatch.core.repository.LawdRepository;
import me.seungpang.apartmentbatch.core.service.AptDealService;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBatchTest
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {AptNotificationJobConfig.class, BatchTestConfig.class})
class AptNotificationJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private AptNotificationRepository aptNotificationRepository;

    @MockBean
    private AptDealService aptDealService;

    @MockBean
    private LawdRepository lawdRepository;

    @MockBean
    private FakeSendService fakeSendService;

    @AfterEach
    void tearDown() {
        aptNotificationRepository.deleteAll();
    }

    @Test
    void sucess() throws Exception {
        //given
        LocalDate dealDate = LocalDate.now().minusDays(1);
        String guLawdCd = "11110";
        String email = "test@gmail.com";
        String anoterEamil = "test2@gmail.com";
        givenAptNotification(guLawdCd, email, true);
        givenAptNotification(guLawdCd, anoterEamil, false);
        givenLawdCd(guLawdCd);
        givenAptDeal(guLawdCd, dealDate);

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(
            new JobParameters(Maps.newHashMap("dealDate", new JobParameter(dealDate.toString())))
        );

        //then
        assertEquals(jobExecution.getExitStatus(), ExitStatus.COMPLETED);
        verify(fakeSendService, times(1)).send(eq(email), anyString());
        verify(fakeSendService, never()).send(eq(anoterEamil), anyString());
    }

    private void givenAptNotification(String guLawdCd, String email, boolean enabled) {
        AptNotification notification = new AptNotification();
        notification.setEmail(email);
        notification.setGuLawdCd(guLawdCd);
        notification.setEnabled(enabled);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        aptNotificationRepository.save(notification);
    }

    private void givenLawdCd(String guLawdCd) {
        String lawdCd = guLawdCd + "00000";
        Lawd lawd = new Lawd();
        lawd.setLawdCd(lawdCd);
        lawd.setLawdDong("경기도 성남시 분당구");
        lawd.setExist(true);
        lawd.setCreatedAt(LocalDateTime.now());
        lawd.setUpdatedAt(LocalDateTime.now());
        when(lawdRepository.findByLawdCd(lawdCd))
            .thenReturn(Optional.of(lawd));
    }

    private void givenAptDeal(String guLawdCd, LocalDate dealDate) {
        when(aptDealService.findyByGuLawdCdAndDealDate(guLawdCd, dealDate))
            .thenReturn(Arrays.asList(
                new AptDto("아파트1", 2000000000L),
                new AptDto("아파트2", 1200000000L)
            ));
    }
}