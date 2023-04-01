package com.emendes.entity;

import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "tb_library_event")
public class LibraryEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer libraryEventId;
  @Enumerated(EnumType.STRING)
  private LibraryEventType libraryEventType;
  @OneToOne(mappedBy = "libraryEvent", cascade = {CascadeType.ALL})
  @ToString.Exclude
  private Book book;

}
