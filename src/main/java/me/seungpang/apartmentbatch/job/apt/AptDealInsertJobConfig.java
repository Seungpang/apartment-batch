package me.seungpang.apartmentbatch.job.apt;

import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.seungpang.apartmentbatch.adapter.ApartmentApiResource;
import me.seungpang.apartmentbatch.core.dto.AptDealDto;
import me.seungpang.apartmentbatch.core.repository.LawdRepository;
import me.seungpang.apartmentbatch.core.service.AptDealService;
import me.seungpang.apartmentbatch.job.validator.YearMonthParameterValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AptDealInsertJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final ApartmentApiResource apartmentApiResource;

    @Bean
    public Job aptDealInsertJob(
        Step guLawdCdStep,
        Step aptDealInsertStep
    ) {
        return jobBuilderFactory.get("aptDealInsertJob")
            .incrementer(new RunIdIncrementer())
            .validator(new YearMonthParameterValidator())
            .start(guLawdCdStep)
            .on("CONTINUABLE").to(aptDealInsertStep).next(guLawdCdStep)
            .from(guLawdCdStep)
            .on("*").end()
            .end()
            .build();
    }


    @JobScope
    @Bean
    public Step guLawdCdStep(Tasklet guLawdCdTasklet) {
        return stepBuilderFactory.get("guLawdCdStep")
            .tasklet(guLawdCdTasklet)
            .build();
    }

    /**
     * ExecutionContext에 저장할 데이터
     * 1. guLawdCd - 구 코드 -> 다음 스텝에서 활용될 값
     * 2. guLawdList - 구 코드 리스트
     * 3. itemCount - 남아있는 구 코드 갯수
     * @return
     */
    @StepScope
    @Bean
    public Tasklet guLawdCdTasklet(LawdRepository lawdRepository) {
        return new GuLawdTasklet(lawdRepository);
    }


    @JobScope
    @Bean
    public Step aptDealInsertStep(
        StaxEventItemReader<AptDealDto> aptDealResourcesReader,
        ItemWriter<AptDealDto> aptDealWriter
    ) {
        return stepBuilderFactory.get("aptDealInsertStep")
            .<AptDealDto, AptDealDto>chunk(10)
            .reader(aptDealResourcesReader)
            .writer(aptDealWriter)
            .build();
    }


    @StepScope
    @Bean
    public StaxEventItemReader<AptDealDto> aptDealResourcesReader (
        @Value("#{jobParameters['yearMonth']}") String yearMonth,
        @Value("#{jobExecutionContext['guLawdCd']}") String guLawdCd,
        Jaxb2Marshaller aptDealDtoMarshaller
    ) {
        return new StaxEventItemReaderBuilder<AptDealDto>()
            .name("aptDealResourcesReader")
            .resource(apartmentApiResource.getResource(guLawdCd, YearMonth.parse(yearMonth)))
            .addFragmentRootElements("item")
            .unmarshaller(aptDealDtoMarshaller)
            .build();
    }

    @StepScope
    @Bean
    public Jaxb2Marshaller aptDealDtoMarshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(AptDealDto.class);
        return jaxb2Marshaller;
    }

    @StepScope
    @Bean
    public ItemWriter<AptDealDto> aptDealWriter(AptDealService aptDealService) {
        return items -> {
            items.forEach(aptDealService::upsert);
            System.out.println("================ writing Completed ==============");
        };
    }
}
