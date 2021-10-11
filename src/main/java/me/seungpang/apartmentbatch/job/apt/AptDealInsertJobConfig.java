package me.seungpang.apartmentbatch.job.apt;

import java.time.YearMonth;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.seungpang.apartmentbatch.adapter.ApartmentApiResource;
import me.seungpang.apartmentbatch.core.dto.AptDealDto;
import me.seungpang.apartmentbatch.core.repository.LawdRepository;
import me.seungpang.apartmentbatch.job.validator.FilePathParameterVaildator;
import me.seungpang.apartmentbatch.job.validator.LawdCdParameterValidator;
import me.seungpang.apartmentbatch.job.validator.YearMonthParameterValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
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
    private final LawdRepository lawdRepository;

    @Bean
    public Job aptDealInsertJob(
        Step guLawdCdStep
        //Step aptDealInsertStep
    ) {
        return jobBuilderFactory.get("aptDealInsertJob")
            .incrementer(new RunIdIncrementer())
            //.validator(aptDealJobParameterValidator())
            .start(guLawdCdStep)
            .build();
    }

    private JobParametersValidator aptDealJobParameterValidator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        validator.setValidators(Arrays.asList(
            new YearMonthParameterValidator(),
            new LawdCdParameterValidator()
        ));
        return validator;
    }

    @JobScope
    @Bean
    public Step guLawdCdStep(Tasklet guLawdCdTasklet) {
        return stepBuilderFactory.get("guLawdCdStep")
            .tasklet(guLawdCdTasklet)
            .build();
    }

    @StepScope
    @Bean
    public Tasklet guLawdCdTasklet() {
        return (contribution, chunkContext) -> {
            lawdRepository.findDistinctGuLawdCd()
                .forEach(System.out::println);
            return RepeatStatus.FINISHED;
        };
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
