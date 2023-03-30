package com.emendes.controller;

import com.emendes.domain.LibraryEvent;
import com.emendes.domain.LibraryEventType;
import com.emendes.exception.InvalidLibraryEventId;
import com.emendes.producer.LibraryEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@RestController
@Slf4j
public class LibraryEventsController {

  private final LibraryEventProducer libraryEventProducer;

  @PostMapping("/v1/libraryevents")
  public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException {

    libraryEvent.setLibraryEventType(LibraryEventType.NEW);
    libraryEventProducer.sendLibraryEvent_Approach2(libraryEvent);

    return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
  }

  @PutMapping("/v1/libraryevents")
  public ResponseEntity<LibraryEvent> putLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException {
    if (libraryEvent.getLibraryEventId() == null) throw new InvalidLibraryEventId("LibraryEventId must not be null");

    libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
    libraryEventProducer.sendLibraryEvent_Approach2(libraryEvent);

    return ResponseEntity.status(HttpStatus.OK).body(libraryEvent);
  }

}
