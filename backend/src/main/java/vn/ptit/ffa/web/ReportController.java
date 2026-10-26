package vn.ptit.ffa.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ptit.ffa.security.CurrentUser;
import vn.ptit.ffa.service.ReportService;
import vn.ptit.ffa.web.dto.ReportDto;

import java.time.LocalDate;
import java.util.List;

/** Báo cáo thống kê (DUAN-54, DUAN-55) — ket qua duoc cache Redis theo ky. */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reports;
    private final CurrentUser currentUser;

    public ReportController(ReportService reports, CurrentUser currentUser) {
        this.reports = reports;
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard")
    public ReportDto.DashboardView dashboard(@RequestParam(required = false) String month) {
        return reports.dashboard(currentUser.require(), month);
    }

    @GetMapping("/monthly")
    public ReportDto.YearlyView monthly(@RequestParam int year) {
        return reports.yearly(currentUser.require(), year);
    }

    @GetMapping("/by-category")
    public List<ReportDto.CategoryShareView> byCategory(@RequestParam(required = false) LocalDate from,
                                                        @RequestParam(required = false) LocalDate to,
                                                        @RequestParam(required = false) String kind,
                                                        @RequestParam(defaultValue = "10") int limit) {
        return reports.byCategory(currentUser.require(), from, to, kind, limit);
    }

    @GetMapping("/by-wallet")
    public List<ReportDto.WalletFlowView> byWallet(@RequestParam(required = false) LocalDate from,
                                                   @RequestParam(required = false) LocalDate to) {
        return reports.byWallet(currentUser.require(), from, to);
    }
}
