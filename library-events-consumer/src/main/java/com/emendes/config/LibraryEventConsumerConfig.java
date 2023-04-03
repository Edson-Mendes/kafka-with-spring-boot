package com.emendes.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

@Slf4j
@Configuration
@EnableKafka
public class LibraryEventConsumerConfig {

  public CommonErrorHandler errorHandler() {
    FixedBackOff fixedBackOff = new FixedBackOff(1000L, 2);

    ExponentialBackOffWithMaxRetries expBackOff = new ExponentialBackOffWithMaxRetries(2);
    expBackOff.setInitialInterval(1_000L);
    expBackOff.setMultiplier(2.0);
    expBackOff.setMaxInterval(2_000L);

//    DefaultErrorHandler errorHandler = new DefaultErrorHandler(fixedBackOff);
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(expBackOff);

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
