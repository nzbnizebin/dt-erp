package com.example.attendance.repository;

import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    @Query("SELECT lr FROM LeaveRequest lr WHERE (:englishName IS NULL OR LOWER(lr.employee.englishName) = LOWER(:englishName)) " +
            "AND (:type IS NULL OR lr.type = :type) " +
            "AND (:start IS NULL OR lr.startTime >= :start) " +
            "AND (:end IS NULL OR lr.endTime <= :end)")
    Page<LeaveRequest> search(@Param("englishName") String englishName,
                              @Param("type") LeaveType type,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end,
                              Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(hours),0) FROM leave_requests lr WHERE lr.employee_id = :employeeId " +
            "AND lr.type = :type AND strftime('%Y', lr.start_time) = :year", nativeQuery = true)
    double sumHoursByEmployeeAndTypeAndYear(@Param("employeeId") Long employeeId,
                                            @Param("type") String type,
                                            @Param("year") String year);

    @Query(value = "SELECT COALESCE(SUM(hours),0) FROM leave_requests lr WHERE lr.employee_id = :employeeId " +
            "AND lr.type = :type AND strftime('%Y-%m', lr.start_time) = :yearMonth", nativeQuery = true)
    double sumHoursByEmployeeAndTypeAndMonth(@Param("employeeId") Long employeeId,
                                             @Param("type") String type,
                                             @Param("yearMonth") String yearMonth);

    @Query(value = "SELECT COALESCE(SUM(hours),0) FROM leave_requests lr WHERE lr.employee_id = :employeeId " +
            "AND lr.type = :type AND lr.start_time >= :from AND lr.start_time < :to", nativeQuery = true)
    double sumHoursByEmployeeAndTypeBetween(@Param("employeeId") Long employeeId,
                                            @Param("type") String type,
                                            @Param("from") String from,
                                            @Param("to") String to);
}
