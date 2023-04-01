package com.emendes.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "tb_book")
public class Book {

  @Id
  private Integer bookId;
  private String bookName;
  private String bookAuthor;
  @OneToOne
  @JoinColumn(name = "library_event_id")
  private LibraryEvent libraryEvent;

}
