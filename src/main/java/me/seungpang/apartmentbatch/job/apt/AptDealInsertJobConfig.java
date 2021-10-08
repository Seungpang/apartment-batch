package me.seungpang.apartmentbatch.job.apt;

import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.seungpang.apartmentbatch.adapter.ApartmentApiResource;
import me.seungpang.apartmentbatch.core.dto.AptDealDto;
import me.seungpang.apartmentbatch.job.validator.FilePathParameterVaildator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AptDealInsertJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final ApartmentApiResource apartmentApiResource;

    @Bean
    public Job aptDealInsertJob(Step aptDealInsertStep) {
        return jobBuilderFactory.get("aptDealInsertJob")
            .incrementer(new RunIdIncrementer())
            .validator(new FilePathParameterVaildator())
            .start(aptDealInsertStep)
            .build();
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
        //@Value("#{jobParameters['filePath']}") String filePath,
        @Value("#{jobParameters['yearMonth']}") String yearMonth,
        @Value("#{jobParameters['lawdCd']}") String lawdCd,
        Jaxb2Marshaller aptDealDtoMarshaller
    ) {
        return new StaxEventItemReaderBuilder<AptDealDto>()
            .name("aptDealResourcesReader")
            //.resource(new ClassPathResource(filePath))
            .resource(apartmentApiResource.getResource(lawdCd, YearMonth.parse(yearMonth)))
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
    public ItemWriter<AptDealDto> aptDealWriter() {
        return items -> {
            items.forEach(System.out::println);
            System.out.println("================ writing Completed ==============");
        };
    }
}
