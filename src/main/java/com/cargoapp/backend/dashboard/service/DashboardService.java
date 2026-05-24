package com.cargoapp.backend.dashboard.service;

import com.cargoapp.backend.common.component.BranchResolver;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.dashboard.dto.DailyStatResponse;
import com.cargoapp.backend.dashboard.dto.DashboardSummaryResponse;
import com.cargoapp.backend.products.entity.ProductStatus;
import com.cargoapp.backend.products.repository.ProductHistoryRepository;
import com.cargoapp.backend.products.repository.ProductRepository;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductHistoryRepository productHistoryRepository;
    private final BranchResolver branchResolver;

    public DashboardSummaryResponse getSummary(UUID currentManagerId, UUID branchId, boolean isSuperAdmin) {
        UUID effectiveBranchId = branchResolver.resolveForManager(currentManagerId, branchId);

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long totalUsers = effectiveBranchId != null
                ? userRepository.countUsersByBranch(effectiveBranchId)
                : userRepository.countAllUsers();

        long newUsersThisMonth = effectiveBranchId != null
                ? userRepository.countNewUsersByBranch(effectiveBranchId, monthStart, now)
                : userRepository.countNewUsers(monthStart, now);

        long inChina = effectiveBranchId != null
                ? productRepository.countByStatusAndBranch(ProductStatus.IN_CHINA, effectiveBranchId)
                : productRepository.countByStatusAll(ProductStatus.IN_CHINA);

        long onTheWay = effectiveBranchId != null
                ? productRepository.countByStatusAndBranch(ProductStatus.ON_THE_WAY, effectiveBranchId)
                : productRepository.countByStatusAll(ProductStatus.ON_THE_WAY);

        long awaitingPickup = effectiveBranchId != null
                ? productRepository.countByStatusAndBranch(ProductStatus.IN_KG, effectiveBranchId)
                : productRepository.countByStatusAll(ProductStatus.IN_KG);

        BigDecimal revenueThisWeek = null;
        if (isSuperAdmin) {
            LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
            revenueThisWeek = effectiveBranchId != null
                    ? productRepository.revenueForPeriodByBranch(weekStart, now, effectiveBranchId)
                    : productRepository.revenueForPeriod(weekStart, now);
        }

        return new DashboardSummaryResponse(
                totalUsers, newUsersThisMonth,
                inChina, onTheWay, awaitingPickup,
                revenueThisWeek
        );
    }

    public List<DailyStatResponse> getUsersChart(UUID currentManagerId, UUID branchId, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        UUID effectiveBranchId = branchResolver.resolveForManager(currentManagerId, branchId);
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        List<Object[]> rows = effectiveBranchId != null
                ? userRepository.countNewUsersByDayAndBranch(effectiveBranchId, fromDt, toDt)
                : userRepository.countNewUsersByDay(fromDt, toDt);

        return fillDailyStats(rows, from, to);
    }

    public List<DailyStatResponse> getProductsChart(UUID currentManagerId, UUID branchId,
                                                     ProductStatus status, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        UUID effectiveBranchId = branchResolver.resolveForManager(currentManagerId, branchId);
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        List<Object[]> rows = effectiveBranchId != null
                ? productHistoryRepository.countDailyByStatusAndBranch(status.name(), effectiveBranchId, fromDt, toDt)
                : productHistoryRepository.countDailyByStatus(status.name(), fromDt, toDt);

        return fillDailyStats(rows, from, to);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST,
                    "Дата начала не может быть позже даты конца");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST,
                    "Период не может превышать 366 дней");
        }
    }

    private List<DailyStatResponse> fillDailyStats(List<Object[]> rows, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> countByDate = rows.stream()
                .collect(Collectors.toMap(
                        r -> toLocalDate(r[0]),
                        r -> ((Number) r[1]).longValue()
                ));

        List<DailyStatResponse> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.add(new DailyStatResponse(d, countByDate.getOrDefault(d, 0L)));
        }
        return result;
    }

    private LocalDate toLocalDate(Object raw) {
        if (raw instanceof java.sql.Date d) return d.toLocalDate();
        return (LocalDate) raw;
    }
}
