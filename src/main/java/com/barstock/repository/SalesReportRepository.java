package com.barstock.repository;
import com.barstock.model.SalesReport;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.LocalDate;
public interface SalesReportRepository extends JpaRepository<SalesReport,Long> {
    List<SalesReport> findAllByOrderByUploadedAtDescIdDesc();
    List<SalesReport> findAllByStatus(SalesReport.Status status);
    Optional<SalesReport> findByImportKey(String key);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from SalesReport r where r.id=:id")
    Optional<SalesReport> locked(@Param("id") Long id);
    boolean existsByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(SalesReport.Status status,LocalDate end,LocalDate start);
}
