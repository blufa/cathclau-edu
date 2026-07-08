package gn.lacaeducation.expenses.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.expenses.application.port.out.ExpenseCategoryRepository;
import gn.lacaeducation.expenses.application.port.out.SupplierRepository;
import gn.lacaeducation.expenses.domain.ExpenseCategory;
import gn.lacaeducation.expenses.domain.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD for expense reference data: suppliers and categories. */
@Service
@Transactional
@RequiredArgsConstructor
public class ExpenseReferenceService {

    private final SupplierRepository supplierRepo;
    private final ExpenseCategoryRepository categoryRepo;

    // ── Suppliers ─────────────────────────────────────────────────────
    public Supplier saveSupplier(Supplier s) { return supplierRepo.save(s); }
    @Transactional(readOnly = true) public List<Supplier> suppliers(Long tenantId) { return supplierRepo.findByTenantId(tenantId); }
    public Supplier updateSupplier(Long id, Supplier patch) {
        Supplier s = supplierRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable : " + id));
        s.setName(patch.getName()); s.setType(patch.getType()); s.setPhone(patch.getPhone());
        s.setEmail(patch.getEmail()); s.setAddress(patch.getAddress()); s.setBankAccount(patch.getBankAccount());
        return supplierRepo.save(s);
    }
    public void deleteSupplier(Long id) { supplierRepo.deleteById(id); }

    // ── Categories ────────────────────────────────────────────────────
    public ExpenseCategory saveCategory(ExpenseCategory c) { return categoryRepo.save(c); }
    @Transactional(readOnly = true) public List<ExpenseCategory> categories(Long tenantId) { return categoryRepo.findByTenantId(tenantId); }
    public ExpenseCategory updateCategory(Long id, ExpenseCategory patch) {
        ExpenseCategory c = categoryRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
        c.setName(patch.getName()); c.setParentId(patch.getParentId());
        return categoryRepo.save(c);
    }
    public void deleteCategory(Long id) { categoryRepo.deleteById(id); }
}
