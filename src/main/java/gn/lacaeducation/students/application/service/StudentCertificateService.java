package gn.lacaeducation.students.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.MediaUploadService;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.students.application.port.in.StudentUseCase;
import gn.lacaeducation.students.domain.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Generates the school-leaving / transfer certificate (certificat de radiation/transfert) as a PDF. */
@Service
@RequiredArgsConstructor
public class StudentCertificateService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 10);

    private final StudentUseCase studentUseCase;
    private final MediaUploadService uploadService;
    private final TenantInfoProvider tenantInfoProvider;

    @Transactional(readOnly = true)
    public String generateTransferCertificate(Long studentId, String schoolName, String motif) {
        Student s = studentUseCase.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable : " + studentId));
        byte[] pdf = render(s, schoolName, motif);
        return uploadService.uploadBytes(pdf, "certificat-radiation-" + s.getStudentNumber() + ".pdf",
                "students/" + studentId + "/certificats");
    }

    private byte[] render(Student s, String schoolName, String motif) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 54);

        Long tenantId = s.getTenantId() != null ? s.getTenantId() : TenantContext.get();
        TenantPublicInfo info = tenantId == null ? null : tenantInfoProvider.findTenantInfo(tenantId).orElse(null);
        Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
        String school = info != null && info.schoolName() != null && !info.schoolName().isBlank()
                ? info.schoolName()
                : (schoolName != null && !schoolName.isBlank() ? schoolName : "L'Établissement");

        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new SchoolPdfPageFooter(school, brand));
        doc.open();

        // En-tête officiel de l'école (logo + nom + contacts + mentions légales).
        SchoolPdfHeader.render(doc, tenantInfoProvider, tenantId);

        Paragraph title = new Paragraph("CERTIFICAT DE RADIATION / TRANSFERT", TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(16);
        title.setSpacingAfter(24);
        doc.add(title);

        String fullName = (s.getFirstName() == null ? "" : s.getFirstName()) + " " + (s.getLastName() == null ? "" : s.getLastName());
        String birth = s.getBirthDate() != null
                ? "né(e) le " + s.getBirthDate().format(DATE) + (s.getBirthPlace() != null ? " à " + s.getBirthPlace() : "")
                : "";
        String body = "Je soussigné(e), la Direction de l'établissement " + school + ", certifie que l'élève "
                + fullName.trim() + ", portant le matricule " + (s.getStudentNumber() == null ? "—" : s.getStudentNumber())
                + " " + birth + ", a été régulièrement inscrit(e) et scolarisé(e) dans notre établissement.";
        doc.add(paragraph(body));

        doc.add(paragraph("L'élève est radié(e) de nos effectifs à la date du " + LocalDate.now().format(DATE) + "."));
        if (motif != null && !motif.isBlank()) {
            doc.add(paragraph("Motif : " + motif + "."));
        }
        doc.add(paragraph("L'intéressé(e) est en règle vis-à-vis de l'établissement et autorisé(e) à poursuivre "
                + "sa scolarité dans un autre établissement."));
        doc.add(paragraph("En foi de quoi, le présent certificat est délivré pour servir et valoir ce que de droit."));

        Paragraph place = new Paragraph("Fait le " + LocalDate.now().format(DATE), BODY);
        place.setAlignment(Element.ALIGN_RIGHT);
        place.setSpacingBefore(28);
        doc.add(place);
        Paragraph sign = new Paragraph("La Direction", SUB);
        sign.setAlignment(Element.ALIGN_RIGHT);
        sign.setSpacingBefore(28);
        doc.add(sign);

        doc.close();
        return out.toByteArray();
    }

    private Paragraph paragraph(String text) {
        Paragraph p = new Paragraph(text, BODY);
        p.setSpacingAfter(10);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }
}
