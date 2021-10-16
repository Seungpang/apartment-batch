package me.seungpang.apartmentbatch.job.notify;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.seungpang.apartmentbatch.adapter.FakeSendService;
import me.seungpang.apartmentbatch.core.dto.AptDto;
import me.seungpang.apartmentbatch.core.dto.NotificationDto;
import me.seungpang.apartmentbatch.core.entity.AptNotification;
import me.seungpang.apartmentbatch.core.repository.AptNotificationRepository;
import me.seungpang.apartmentbatch.core.repository.LawdRepository;
import me.seungpang.apartmentbatch.core.service.AptDealService;
import me.seungpang.apartmentbatch.job.validator.DealDateParameterValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AptNotificationJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job aptNotificationJob(Step aptNotificationStep) {
        return jobBuilderFactory.get("aptNotificationJob")
            .incrementer(new RunIdIncrementer())
            .validator(new DealDateParameterValidator())
            .start(aptNotificationStep)
            .build();
    }

    @JobScope
    @Bean
    public Step aptNotificationStep(
        RepositoryItemReader<AptNotification> aptNotificationRepositoryItemReader,
        ItemProcessor<AptNotification, NotificationDto> aptNotificationProcessor,
        ItemWriter<NotificationDto> aptNotificationWriter
    ) {
        return stepBuilderFactory.get("aptNotificationStep")
            .<AptNotification, NotificationDto>chunk(10)
            .reader(aptNotificationRepositoryItemReader)
            .processor(aptNotificationProcessor)
            .writer(aptNotificationWriter)
            .build();
    }

    @StepScope
    @Bean
    public RepositoryItemReader<AptNotification> aptNotificationRepositoryItemReader(AptNotificationRepository aptNotificationRepository) {
        return new RepositoryItemReaderBuilder<AptNotification>()
            .name("aptNotificationRepositoryItemReader")
            .repository(aptNotificationRepository)
            .methodName("findByEnabledIsTrue")
            .pageSize(10)
            .arguments(List.of())
            .sorts(Collections.singletonMap("aptNotificationId", Direction.DESC))
            .build();
    }

    @StepScope
    @Bean
    public ItemProcessor<AptNotification, NotificationDto> aptNotificationProcessor(
        @Value("#{jobParameters['dealDate']}") String dealDate,
        AptDealService aptDealService,
        LawdRepository lawdRepository
    ) {
        return aptNotification -> {
            List<AptDto> aptDtoList = aptDealService.findyByGuLawdCdAndDealDate(
                aptNotification.getGuLawdCd(),
                LocalDate.parse(dealDate));

            if (aptDtoList.isEmpty()) {
                return null;
            }

            String guName = lawdRepository.findByLawdCd(aptNotification.getGuLawdCd() + "00000")
                .orElseThrow().getLawdDong();

            return NotificationDto.builder()
                .email(aptNotification.getEmail())
                .guName(guName)
                .count(aptDtoList.size())
                .aptDeals(aptDtoList)
                .build();
        };
    }

    @StepScope
    @Bean
    public ItemWriter<NotificationDto> aptNotificationWriter(FakeSendService fakeSendService) {
        return items -> items.forEach(item -> fakeSendService.send(item.getEmail(), item.toMessage()));
    }
}
