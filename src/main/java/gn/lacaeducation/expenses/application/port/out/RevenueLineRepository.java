package gn.lacaeducation.expenses.application.port.out;
import gn.lacaeducation.expenses.domain.RevenueLine;
import java.util.List;
import java.util.Optional;
public interface RevenueLineRepository {
    RevenueLine save(RevenueLine r);
    Optional<RevenueLine> findById(Long id);
    List<RevenueLine> findBySheetId(Long sheetId);
    void deleteById(Long id);
}
