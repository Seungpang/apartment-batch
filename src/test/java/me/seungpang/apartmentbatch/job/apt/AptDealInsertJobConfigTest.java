package me.seungpang.apartmentbatch.job.apt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import me.seungpang.apartmentbatch.BatchTestConfig;
import me.seungpang.apartmentbatch.adapter.ApartmentApiResource;
import me.seungpang.apartmentbatch.core.repository.LawdRepository;
import me.seungpang.apartmentbatch.core.service.AptDealService;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBatchTest
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {AptDealInsertJobConfig.class, BatchTestConfig.class})
class AptDealInsertJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockBean
    private AptDealService aptDealService;

    @MockBean
    private LawdRepository lawdRepository;

    @MockBean
    private ApartmentApiResource apartmentApiResource;

    @Test
    void success() throws Exception {
        //given
        when(lawdRepository.findDistinctGuLawdCd()).thenReturn(Arrays.asList("41135", "41136"));
        when(apartmentApiResource.getResource(anyString(), any())).thenReturn(
            new ClassPathResource("test-api-response.xml")
        );

        //when
        JobExecution execution = jobLauncherTestUtils.launchJob(
            new JobParameters(Maps.newHashMap("yearMonth", new JobParameter("2021-07")))
        );

        //then
        assertEquals(execution.getExitStatus(), ExitStatus.COMPLETED);
        verify(aptDealService, times(6)).upsert(any());
    }

    @Test
    void fail_whenYearMonthNoExist() throws Exception {
        //given
        when(lawdRepository.findDistinctGuLawdCd()).thenReturn(List.of("41135"));
        when(apartmentApiResource.getResource(anyString(), any())).thenReturn(
            new ClassPathResource("test-api-response.xml"));

        //when
        assertThrows(JobParametersInvalidException.class,
            () -> jobLauncherTestUtils.launchJob());

        //then
        verify(aptDealService, never()).upsert(any());
    }
}