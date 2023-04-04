package com.emendes.scheduler;

import com.emendes.config.LibraryEventConsumerConfig;
import com.emendes.entity.FailureRecord;
import com.emendes.repository.FailureRecordRepository;
import com.emendes.service.LibraryEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
@Component
public class RetryScheduler {

  private final FailureRecordRepository failureRecordRepository;
  private final LibraryEventService libraryEventService;

  @Scheduled(fixedRate = 10_000)
  public void retryFailed() {

    List<FailureRecord> failureRecordList = failureRecordRepository.findAllByStatus(LibraryEventConsumerConfig.RETRY);

    if (failureRecordList.isEmpty()) {
      log.info("No FailureRecord found!");
      return;
    }

    log.info("Retrying failed records started!");
    failureRecordList.forEach(failureRecord -> {

      ConsumerRecord<Integer, String> consumerRecord = buildConsumerRecord(failureRecord);
      libraryEventService.processLibraryEvent(consumerRecord);
      failureRecord.setStatus(LibraryEventConsumerConfig.SUCCESS);
      failureRecordRepository.save(failureRecord);
    });

    log.info("Retrying failed records completed!");
  }

  private ConsumerRecord<Integer, String> buildConsumerRecord(FailureRecord failureRecord) {
    return new ConsumerRecord<>(
        failureRecord.getTopic(),
        failureRecord.getPartition(),
        failureRecord.getOffsetValue(),
        failureRecord.getTopicKey(),
        failureRecord.getErrorRecord()
    );
  }

}
