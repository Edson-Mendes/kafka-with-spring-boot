package com.emendes.service.impl;

import com.emendes.entity.FailureRecord;
import com.emendes.repository.FailureRecordRepository;
import com.emendes.service.FailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class FailureServiceImpl implements FailureService {

  private final FailureRecordRepository failureRecordRepository;

  @Override
  public void saveFailedRecord(ConsumerRecord<Integer, String> consumerRecord, Exception e, String status) {
    FailureRecord failureRecord = FailureRecord.builder()
        .topic(consumerRecord.topic())
        .topicKey(consumerRecord.key())
        .errorRecord(consumerRecord.value())
        .partition(consumerRecord.partition())
        .offsetValue(consumerRecord.offset())
        .exception(e.getCause().getMessage())
        .status(status)
        .build();

    log.info("inside saveFailedRecord with status : {}", status);

    failureRecordRepository.save(failureRecord);
  }
}
