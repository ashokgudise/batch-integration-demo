package com.integration.demos.config;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import com.integration.demos.batch.repository.BenefitiaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.DelayerEndpointSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.CollectionUtils;


@Configuration
@EnableIntegration
public class IntegrationConfig {

    private static final Logger log = LoggerFactory.getLogger(IntegrationConfig.class);

    public static final Long INPUT_DIRECTORY_POOL_RATE = (long) 1000;
    public static final String INPUT_DIRECTORY = "/Users/ashokgudise/workspaces/batch-integration/input";
    public static final String PROCESSED_DIRECTORY = "/Users/ashokgudise/workspaces/batch-integration/processed";

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private BenefitiaryRepository repository;

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryInitializer() {
        JobRegistryBeanPostProcessor initializer = new JobRegistryBeanPostProcessor();
        initializer.setJobRegistry(jobRegistry);
        return initializer;
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller() {
        return Pollers.fixedRate(INPUT_DIRECTORY_POOL_RATE).get();
    }

    @Bean
    public MessageChannel fileInputChannel() { return MessageChannels.direct().get(); }

    @Bean
    public MessageChannel jobRequestChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public MessageChannel jobExecutionChannel() {
        return MessageChannels.queue().get();
    }

    @Bean
    public MessageChannel jobStatusChannel() {
        return MessageChannels.publishSubscribe().get();
    }

    @Bean
    public MessageChannel jobRestartChannel() {
        return MessageChannels.queue().get();
    }

    @Bean
    public MessageChannel jobExecutionNotifiedChannel() {
        return MessageChannels.queue().get();
    }

    @Bean
    public MessageChannel jobCompletedChannel() {
        return MessageChannels.queue().get();
    }



    /**
     * <int:chain input-channel='fileInputChannel' output-channel='jobStatusChannel'>
     *      <int:gateway ref='fileReadingMessageSource' />
     *      <int:transformer ref='transformFileToRequest'/>
     *      <int:gateway ref='jobRequestHandler'/>
     *      <int:transformer ref='transformJobExecutionToStatus'/>
     * </int:chain>
     *
     * <int:service-activator ref='fileReadingMessageSource'/>
     *
     * <int:service-activator ref='jobRequestHandler' reply-channel='jobRequestChannel' />
     *
     * @return
     */
    @Bean
    public IntegrationFlow processFilesFlow() {
        return IntegrationFlows.from(fileReadingMessageSource(), c -> c.poller(poller()))
                .channel(fileInputChannel())
                .<File, JobLaunchRequest>transform(f -> transformFileToRequest(f))
                .channel(jobRequestChannel())
                .handle(jobRequestHandler())
                .<JobExecution, String>transform(e -> transformJobExecutionToStatus(e))
                .channel(jobStatusChannel())
                .get();
    }

    /**
     * <int:chain input-channel='jobExecutionChannel'>
     *     <int:router>
     *         <int:mapping value='true'  output-channel='jobRestartChannel'/>
     *         <int:mapping value='false' output-channel='jobExecutionNotifiedChannel/>
     *     </int:router>
     *
     * </int:chain>
     *
     *
     * @return Integration Flow
     */
    @Bean
    public IntegrationFlow processExecutionsFlow() {
        return IntegrationFlows.from(jobExecutionChannel())
                .route(JobExecution.class, e -> e.getStatus().equals(BatchStatus.FAILED),
                        m -> m.subFlowMapping(true, f -> f.channel(jobRestartChannel()))
                                .subFlowMapping(false, f -> f.channel(jobExecutionNotifiedChannel())))
                .get();
    }

    /**
     *  <int:chain id='processExecutionRestartsFlow' request-channel='jobRestartChannel'>
     *
     *      <int:gateway ref='jobRestarter'/>
     *
     *  </int:chain>
     * @return Integration Flow
     */
    @Bean
    public IntegrationFlow processExecutionRestartsFlow() {
        return IntegrationFlows.from(jobRestartChannel())
                .delay("wait5sec", (DelayerEndpointSpec e) -> e.defaultDelay(5000))
                .handle(e -> jobRestarter((JobExecution)e.getPayload()))
                .get();
    }

    /**
     *
     * <int:chain input-channel='jobExecutionNotifiedChannel'>
     *
     *  <int:router>
     *      <int:mapping value='true' reply-channel='jobCompletedChannel'/>
     *      <int:mapping value='false' reply-channel='jobStatusChannel/>
     *  </int:router>
     *
     * </int:chain>
     * @return Integration Flow
     */
    @Bean
    public IntegrationFlow processNotifiedExecutionFlow() {
        return IntegrationFlows.from(jobExecutionNotifiedChannel())
                .route(JobExecution.class, e -> e.getStatus().equals(BatchStatus.COMPLETED),
                        m -> m.subFlowMapping(true, f -> f.channel(jobCompletedChannel()))
                                .subFlowMapping(false, f -> f.<JobExecution, String>transform(e -> transformJobExecutionToStatus(e))
                                        .channel(jobStatusChannel())))

                .get();
    }

    /**
     * <int:chain input-channel='jobCompletedChannel' output-channel='jobStatusChannel'>
     *  <int:transformer ref='jobExecutionToFileTransformer'></int:transformer>
     *     <int:gateway ref='processedFileWritingHandler'></int:gateway>
     * </int:chain>
     *
     *
     * @return Interation Flow
     */
    @Bean
    public IntegrationFlow processExecutionCompletedFlow() {
        return IntegrationFlows.from(jobCompletedChannel())
                .transform(jobExecutionToFileTransformer())
                .handle(processedFileWritingHandler())
                .channel(jobStatusChannel())
                .get();
    }

    /**
     *  Adding Gateway Capabilities to JobExecutionListener with channel 'jobExecutionChannel'
     * @return
     */
    @Bean
    public GatewayProxyFactoryBean jobExecutionsListener(){
        GatewayProxyFactoryBean factoryBean = new GatewayProxyFactoryBean(JobExecutionListener.class);
        factoryBean.setDefaultRequestChannel(jobExecutionChannel());
        return factoryBean;
    }

    /**
     * FileFilter - Setting file-patterns, directory, directory filter etc.
     * @return
     */
    @Bean
    public MessageSource<File> fileReadingMessageSource() {
        CompositeFileListFilter<File> filters = new CompositeFileListFilter<>();
        filters.addFilter(new SimplePatternFileListFilter("*.txt"));
        filters.addFilter(new AcceptOnceFileListFilter<>());

        FileReadingMessageSource source = new FileReadingMessageSource();
        source.setAutoCreateDirectory(true);
        source.setDirectory(new File(INPUT_DIRECTORY));
        source.setFilter(filters);

        return source;
    }

    /**
     * JobLauncher as ServiceActivator
     * @return
     */
    @Bean
    public JobLaunchingMessageHandler jobRequestHandler() {
        return new JobLaunchingMessageHandler(jobLauncher);
    }

    /**
     * Processing Directory Properties - Directory Filters, File Over-writing type Replace,Append etc.
     *
     * @return
     */
    @Bean
    public FileWritingMessageHandler processedFileWritingHandler() {
        FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(PROCESSED_DIRECTORY));
        handler.setAutoCreateDirectory(true);
        handler.setDeleteSourceFiles(true);
        handler.setFileExistsMode(FileExistsMode.REPLACE);
        return handler;
    }

    /**
     *
     * @param execution
     * @return
     */
    public JobExecution jobRestarter(JobExecution execution) {
        log.info("Restarting job...");
        try {
            Job job = jobRegistry.getJob(execution.getJobInstance().getJobName());
            return jobLauncher.run(job, execution.getJobParameters());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creating file by reading file path from Job Parameter
     * @return
     */
    @Bean
    public GenericTransformer<JobExecution, File> jobExecutionToFileTransformer() {
        return new GenericTransformer<JobExecution, File>(){
            @Override
            public File transform(JobExecution source) {
                String path = source.getJobParameters().getString("input.file.path");
                return new File(path);
            }
        };
    }

    /**
     * Creating a Job with FileName, DateTime as Job Parameters - as JobRequest
     * @param file
     * @return
     */
    public JobLaunchRequest transformFileToRequest(File file) {
        log.info("Creating request");

        Job job = getJobByFileName();
        log.info("Job = " + job.getName());

        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addDate("start.date", new Date());
        paramsBuilder.addString("input.file.path", file.getPath());

        log.info("Parameters = " + paramsBuilder.toString());

        JobLaunchRequest request = new JobLaunchRequest(job, paramsBuilder.toJobParameters());
        return request;
    }

    /**
     * Read Job Status from JobExecution and preparing a customized message
     * @param execution
     * @return
     */
    public String transformJobExecutionToStatus(JobExecution execution) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SS");
        StringBuilder builder = new StringBuilder();

        builder.append(execution.getJobInstance().getJobName());

        BatchStatus evaluatedStatus = endingBatchStatus(execution);
        if (evaluatedStatus == BatchStatus.COMPLETED || evaluatedStatus.compareTo(BatchStatus.STARTED) > 0) {
            builder.append(" has completed with a status of ")
                    .append(execution.getStatus().name())
                    .append(" at ")
                    .append(formatter.format(new Date()));

            builder.append(" with " )
                    .append(repository.count())
                    .append(" processed records.");
        } else {
            builder.append(" has started at ")
                    .append(formatter.format(new Date()));
        }

        return builder.toString();
    }


    private BatchStatus endingBatchStatus(JobExecution execution) {
        BatchStatus status = execution.getStatus();
        Collection<StepExecution> stepExecutions = execution.getStepExecutions();

        if (!CollectionUtils.isEmpty(stepExecutions)) {
            for (StepExecution stepExecution : stepExecutions) {

                if (stepExecution.getStatus().equals(BatchStatus.FAILED)) {
                    status = BatchStatus.FAILED;
                    break;
                } else {
                    status = BatchStatus.COMPLETED;
                }
            }
        }

        return status;
    }

    /**
     * Creating a new Job
     * @return
     */
    private Job getJobByFileName() {
        try {
            return jobRegistry.getJob(BatchConfig.DEFAULT_JOB_NAME);
        } catch (NoSuchJobException e) {
            log.error(e.getLocalizedMessage());
            e.printStackTrace();
            return null;
        }
    }
}