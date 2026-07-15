package com.clinova.repository;

import com.clinova.entity.RequisitoLegal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequisitoLegalRepository extends JpaRepository<RequisitoLegal, Long> {
}
