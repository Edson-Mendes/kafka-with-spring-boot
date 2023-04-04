package com.emendes.repository;

import com.emendes.entity.FailureRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailureRecordRepository extends JpaRepository<FailureRecord, Integer> {
}
