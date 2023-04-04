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
@Table(name = "tb_failure_record")
public class FailureRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private String topic;
  private Integer topicKey;
  private String errorRecord;
  private Integer partition;
  private Long offsetValue;
  private String exception;
  private String status;

}
