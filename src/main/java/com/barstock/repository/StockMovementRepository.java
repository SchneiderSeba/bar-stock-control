package com.barstock.repository;
import com.barstock.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {}

