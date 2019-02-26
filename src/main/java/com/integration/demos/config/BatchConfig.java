package com.integration.demos.config;


import com.integration.demos.batch.mappers.BenefitiaryFieldSetMapper;
import com.integration.demos.batch.readers.BenifitiaryCustomItemReader;
import com.integration.demos.batch.repository.BenefitiaryRepository;
import com.integration.demos.model.Benefitiary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.*;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    private static final Logger log = LoggerFactory.getLogger(BatchConfig.class);

    public static final String DEFAULT_STEP_NAME = "processingStep";
    public static final String DEFAULT_JOB_NAME = "processingJob";

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BenefitiaryRepository repository;

    @Bean
    @StepScope
    public FlatFileItemReader<Benefitiary> reader(
            @Value("file:///#{jobParameters['input.file.path']}") Resource resource) throws Exception {

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer("|");
        tokenizer.setNames(new String[] { "beneId","name", "phoneNumber" });

        DefaultLineMapper<Benefitiary> benefitiaryLineMapper = new DefaultLineMapper<>();
        benefitiaryLineMapper.setLineTokenizer(tokenizer);
        benefitiaryLineMapper.setFieldSetMapper(new BenefitiaryFieldSetMapper());
        benefitiaryLineMapper.afterPropertiesSet();

        FlatFileItemReader<Benefitiary> reader = new FlatFileItemReader<>();
        reader.setResource(resource);
        reader.setLinesToSkip(1);
        reader.setLineMapper(benefitiaryLineMapper);
        reader.afterPropertiesSet();

        return reader;
    }

    @Bean
    public ItemProcessor<Benefitiary, Benefitiary> processor(){
        return new ItemProcessor<Benefitiary, Benefitiary>(){

            @Override
            public Benefitiary process(Benefitiary item) throws Exception {
                log.info("Processing: " + item.getName());
                return item;
            }

        };
    }

    @Bean
    public RepositoryItemWriter<Benefitiary> writer() {
        RepositoryItemWriter<Benefitiary> writer = new RepositoryItemWriter<>();
        writer.setRepository(repository);
        writer.setMethodName("save");
        return writer;
    }


    @Bean
    public Step processingStepBean(ItemReader<Benefitiary> reader, ItemProcessor<Benefitiary, Benefitiary> processor, ItemWriter<Benefitiary> writer) {
        log.debug("Configuring Step: " + DEFAULT_STEP_NAME);
        return stepBuilderFactory.get(DEFAULT_STEP_NAME)
                .<Benefitiary, Benefitiary>chunk(5)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job processingJobBean(Step processingStep, JobExecutionListener listener) {
        log.debug("Configuring Job: " + DEFAULT_JOB_NAME);
        return jobBuilderFactory.get(DEFAULT_JOB_NAME)
                .listener(listener)
                .incrementer(new RunIdIncrementer())
                .flow(processingStep)
                .end()
                .build();
    }




}