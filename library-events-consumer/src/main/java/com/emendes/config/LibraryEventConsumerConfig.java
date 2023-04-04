package com.emendes.config;


import com.emendes.service.FailureService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@EnableKafka
public class LibraryEventConsumerConfig {

  public static final String RETRY = "RETRY";
  public static final String DEAD = "DEAD";
  public static final String SUCCESS = "SUCCESS";

  @Autowired
  private KafkaTemplate<Integer, String> kafkaTemplate;
  @Autowired
  private FailureService failureService;

  @Value("${topics.retry}")
  private String retryTopic;

  @Value("${topics.dlt}")
  private String deadLetterTopic;

  public DeadLetterPublishingRecoverer publishingRecoverer() {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
        (r, e) -> {
          if (e.getCause() instanceof RecoverableDataAccessException) {
            return new TopicPartition(retryTopic, r.partition());
          }
          else {
            return new TopicPartition(deadLetterTopic, r.partition());
          }
        });
    CommonErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 2L));

    return recoverer;
  }


  public ConsumerRecordRecoverer consumerRecordRecoverer = ((consumerRecord, e) -> {
    log.info("Exception in consumerRecordRecover : {}", e.getMessage());
    ConsumerRecord<Integer, String> record = (ConsumerRecord<Integer, String>) consumerRecord;
    if (e.getCause() instanceof RecoverableDataAccessException) {
      // recovery logic
      log.info("inside recovery");
      failureService.saveFailedRecord(record, e, RETRY);
    }
    else {
      // non-recovery logic
      log.info("inside non-recovery");
      failureService.saveFailedRecord(record, e, DEAD);
    }
  });

  public CommonErrorHandler errorHandler() {
    FixedBackOff fixedBackOff = new FixedBackOff(1000L, 2);

    ExponentialBackOffWithMaxRetries expBackOff = new ExponentialBackOffWithMaxRetries(2);
    expBackOff.setInitialInterval(1_000L);
    expBackOff.setMultiplier(2.0);
    expBackOff.setMaxInterval(2_000L);

//    DefaultErrorHandler errorHandler = new DefaultErrorHandler(fixedBackOff);
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(
//        publishingRecoverer(),
        consumerRecordRecoverer,
        expBackOff);

    errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

    errorHandler.setRetryListeners(((record, ex, deliveryAttempt) -> {
      log.info("Failed record in retry listener, Exception: {}, deliveryAttempt: {}", ex.getMessage(), deliveryAttempt);
    }));

    return errorHandler;
  }

  @Bean
  @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
  public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
      ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
      ConsumerFactory<Object, Object> kafkaConsumerFactory) {

    ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
    configurer.configure(factory, kafkaConsumerFactory);

    factory.setCommonErrorHandler(errorHandler());

    return factory;
  }

}
