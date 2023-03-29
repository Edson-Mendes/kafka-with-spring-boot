package com.emendes.controller;

import com.emendes.domain.LibraryEvent;
import com.emendes.producer.LibraryEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@RestController
@Slf4j
public class LibraryEventsController {

  private final LibraryEventProducer libraryEventProducer;

  @PostMapping("/v1/libraryevents")
  public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException {

    log.info("Before sendLibraryEvent");

//    libraryEventProducer.sendLibraryEvent(libraryEvent);
    SendResult<Integer, String> sendResult = libraryEventProducer.sendLibraryEventSynchronous(libraryEvent);
    log.info("SendResult is {}", sendResult.toString());
    log.info("After sendLibraryEvent");

    log.info("A LibraryEvent was sent!");
    return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
  }

}
