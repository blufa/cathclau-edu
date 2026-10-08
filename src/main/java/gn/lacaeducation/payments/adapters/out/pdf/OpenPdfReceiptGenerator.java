package gn.lacaeducation.payments.adapters.out.pdf;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.AdminDocumentLayout;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.payments.application.port.out.ReceiptPdfGenerator;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Rend un reçu de paiement officiel en PDF A4, sobre (noir sur blanc) façon reçu de caisse
 * administratif : en-tête établissement (logo + adresse + téléphone), titre encadré, bloc
 * identification compact (année scolaire, n° reçu, mode de paiement, élève, matricule, classe — deux
 * champs par ligne, voir {@link #compactFields}), tableau de TOUTES les factures de l'élève, total et
 * bloc de signature du comptable (nom de l'encaisseur, sans cadre — voir
 * {@link #comptableSignatureBlock}). A4 (au lieu d'A5) et bloc d'identification compact : garantissent
 * que le reçu tient sur une seule page quel que soit le nombre de factures listées (§retour comptable —
 * même raisonnement que {@code OpenPdfInvoiceGenerator}). La "Date de paiement" (bloc compact,
 * {@code payment.getPaidAt()}) est distincte du "Généré le ..." du pied de page (date de PRODUCTION du
 * fichier PDF, pas de l'encaissement) — un encaissement fait hors-ligne un jour puis synchronisé/réimprimé
 * un autre jour aurait sinon affiché une date de paiement fausse (§retour comptable).
 * Mise en page partagée avec la facture et la fiche de paie via {@link AdminDocumentLayout}.
 */
@Component
@RequiredArgsConstructor
class OpenPdfReceiptGenerator implements ReceiptPdfGenerator {

    private final TenantInfoProvider tenantInfoProvider;
    private final PayerDirectory payerDirectory;
    private final ObjectMapper objectMapper;
    private final gn.lacaeducation.payments.application.port.out.PaymentRepository paymentRepository;

    /** Une part d'un paiement groupé (miroir minimal de {@code GroupedInvoiceSettlementEvent.Share}). */
    private record GroupLine(Long invoiceId, Long studentId, BigDecimal amount) {}

    @Override
    public byte[] generate(Receipt receipt, Payment payment) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // A4 (au lieu d'A5) : garantit que le reçu tient sur une seule page quel que soit le nombre de
        // factures listées dans le tableau ci-dessous (§retour comptable) — A5 débordait dès qu'un
        // élève avait plus de quelques factures (mensualités + frais annexes sur l'année).
        Document doc = new Document(PageSize.A4, 36, 36, 28, 40);
        try {
            TenantPublicInfo info = payment.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(payment.getTenantId()).orElse(null);
            java.awt.Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            String academicYearLabel = payerDirectory.currentAcademicYearLabel(payment.getTenantId()).orElse(null);
            header(doc, info, academicYearLabel);
            titleBox(doc, "REÇU DE PAIEMENT OFFICIEL");

            // Bloc compact (deux champs par ligne) au lieu d'un champ par ligne, pour libérer de la
            // place verticale (§retour comptable). "Date de paiement" (encaissement réel, distincte du
            // "Généré le..." du pied de page) réintégrée : indispensable pour un encaissement hors-ligne
            // synchronisé/réimprimé en différé.
            doc.add(compactFields(
                    new String[]{"Année scolaire", safe(academicYearLabel)},
                    new String[]{"N° reçu", safe(receipt.getReference())},
                    new String[]{"Mode de paiement", channelLabel(payment)},
                    new String[]{"Date de paiement", payment.getPaidAt() != null
                            ? payment.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm")) : "-"}));

            // Fratrie (§payments) : un paiement groupé peut couvrir plusieurs enfants — un bloc + un
            // sous-tableau de frais par enfant plutôt qu'un bloc élève unique, dans l'ordre où ils
            // apparaissent dans les parts du paiement.
            java.util.LinkedHashSet<Long> studentIds = new java.util.LinkedHashSet<>();
            for (GroupLine line : parseGroupLines(payment.getGroupLines())) {
                if (line.studentId() != null) studentIds.add(line.studentId());
            }
            if (studentIds.isEmpty() && payment.getStudentId() != null) studentIds.add(payment.getStudentId());

            // Ordre demandé (§retour comptable) : le tableau de factures, puis "Encaissé aujourd'hui"
            // (CET encaissement précis), puis "Total payé" (cumul de toutes les factures réglées à ce
            // jour, même celles réglées un autre jour — distinct du précédent : voir leur Javadoc
            // respective plus bas), puis "Reste à payer" — jamais dans un autre ordre.
            // Collecté pendant l'affichage des tableaux ci-dessous (§retour comptable) : une facture
            // déjà réglée sans encaissement correspondant (crédit reporté d'un transfert de classe)
            // doit être expliquée, sinon le comptable ne comprend pas le calcul.
            List<String> creditNotes = new java.util.ArrayList<>();
            if (studentIds.size() <= 1) {
                Long studentId = studentIds.isEmpty() ? payment.getStudentId() : studentIds.iterator().next();
                PdfPTable studentBlock = compactFields(
                        new String[]{"Matricule", safe(payerDirectory.studentNumber(studentId).orElse(null))},
                        new String[]{"Nom & Prénom", safe(receipt.getStudentName())},
                        new String[]{"Classe", safe(payerDirectory.className(studentId).orElse(null))});
                studentBlock.setSpacingBefore(3f);
                doc.add(studentBlock);
                List<PayerDirectory.InvoiceRowInfo> invoices = payerDirectory.studentInvoices(studentId);
                doc.add(invoicesTable(invoices, paidAtByInvoiceId(studentId)));
                creditNotes.addAll(creditNotes(invoices));
                amountBox(doc, "Encaissé aujourd'hui", money(receipt.getAmount()) + " GNF",
                        SchoolPdfHeader.tint(brand, 0.12), DARK);
                amountBox(doc, "Total payé", money(sumPaidToDate(invoices)) + " GNF", brand, java.awt.Color.WHITE);
                remainingLine(doc, studentId);
            } else {
                BigDecimal totalPaidToDate = BigDecimal.ZERO;
                for (Long studentId : studentIds) {
                    PdfPTable studentBlock = compactFields(
                            new String[]{"Élève", safe(payerDirectory.studentName(studentId).orElse(null))},
                            new String[]{"Matricule", safe(payerDirectory.studentNumber(studentId).orElse(null))},
                            new String[]{"Classe", safe(payerDirectory.className(studentId).orElse(null))});
                    studentBlock.setSpacingBefore(3f);
                    doc.add(studentBlock);
                    List<PayerDirectory.InvoiceRowInfo> invoices = payerDirectory.studentInvoices(studentId);
                    doc.add(invoicesTable(invoices, paidAtByInvoiceId(studentId)));
                    creditNotes.addAll(creditNotes(invoices));
                    totalPaidToDate = totalPaidToDate.add(sumPaidToDate(invoices));
                }
                amountBox(doc, "Encaissé aujourd'hui", money(receipt.getAmount()) + " GNF",
                        SchoolPdfHeader.tint(brand, 0.12), DARK);
                amountBox(doc, "Total payé", money(totalPaidToDate) + " GNF", brand, java.awt.Color.WHITE);
                for (Long studentId : studentIds) {
                    remainingLine(doc, studentId);
                }
            }

            for (String line : creditNotes) {
                Paragraph credit = new Paragraph(line, font(FontFactory.HELVETICA, 9, GREY));
                credit.setSpacingBefore(2f);
                doc.add(credit);
            }

            for (String line : discountNotes(payment)) {
                Paragraph discount = new Paragraph(line, font(FontFactory.HELVETICA, 9, GREY));
                discount.setSpacingBefore(2f);
                doc.add(discount);
            }
            String platformFeeNote = platformFeeNote(payment);
            if (platformFeeNote != null) {
                Paragraph pf = new Paragraph(platformFeeNote, font(FontFactory.HELVETICA, 9, GREY));
                pf.setSpacingBefore(2f);
                doc.add(pf);
            }

            comptableSignatureBlock(doc, writer, receipt, payment.getCollectedByUserId());

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec de génération du PDF du reçu " + receipt.getReference(), e);
        }
    }

    /** Bloc "Libellé : valeur" compact, deux champs par ligne au lieu d'un seul (§retour comptable) —
     * libère assez de place verticale pour que le reçu tienne sur une seule page même avec un long
     * tableau de factures en dessous. Nombre de paires toujours pair ici (3 champs → complété par une
     * cellule vide), mais gère aussi un nombre impair au cas où. */
    private PdfPTable compactFields(String[]... pairs) {
        PdfPTable t = new PdfPTable(new float[]{0.9f, 1.6f, 0.9f, 1.6f});
        t.setWidthPercentage(100);
        for (int i = 0; i < pairs.length; i += 2) {
            addFieldCells(t, pairs[i][0], pairs[i][1]);
            if (i + 1 < pairs.length) {
                addFieldCells(t, pairs[i + 1][0], pairs[i + 1][1]);
            } else {
                blankCell(t);
                blankCell(t);
            }
        }
        return t;
    }

    private void addFieldCells(PdfPTable t, String label, String value) {
        PdfPCell l = new PdfPCell(new com.lowagie.text.Phrase(label, font(FontFactory.HELVETICA_BOLD, 9, DARK)));
        l.setBorder(0);
        l.setPaddingBottom(3f);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new com.lowagie.text.Phrase(": " + (value == null ? "-" : value), font(FontFactory.HELVETICA, 9, DARK)));
        v.setBorder(0);
        v.setPaddingBottom(3f);
        t.addCell(v);
    }

    private void blankCell(PdfPTable t) {
        PdfPCell c = new PdfPCell(new com.lowagie.text.Phrase(""));
        c.setBorder(0);
        t.addCell(c);
    }

    /**
     * Toutes les factures (non annulées) de l'élève — pas seulement celle(s) couvertes par ce
     * paiement — pour que le reçu serve de vue d'ensemble de sa situation (§retour comptable) : Réf.,
     * Montant, Observation (type de frais + période), Date & heure d'encaissement (dernier paiement
     * reçu sur CETTE facture précise, "-" si jamais payée), et une colonne Payé en gras (OUI/NON)
     * donnant son statut de règlement à cette date. Déjà triées par {@link PayerDirectory#studentInvoices}
     * (inscription, puis mensualité, puis frais annexes — §retour comptable : un nouveau frais annexe
     * souscrit plus tard s'ajoute donc en bas, jamais au milieu des tranches). Lignes compactes
     * (padding réduit, voir {@link #compactHeaderCell}/{@link #compactCell}) plutôt que
     * {@code AdminDocumentLayout#cell} : ce tableau grandit avec le nombre de factures de l'élève
     * (mensualités + frais annexes sur l'année) et était la cause du débordement du reçu sur une
     * deuxième page — jamais touché sur la facture/fiche de paie, qui n'ont pas ce problème.
     */
    private PdfPTable invoicesTable(List<PayerDirectory.InvoiceRowInfo> invoices,
                                     java.util.Map<Long, java.time.LocalDateTime> paidAtByInvoiceId) {
        PdfPTable t = dataTable(new float[]{1.05f, 0.9f, 1.35f, 0.85f, 0.5f});
        t.setSpacingBefore(4f);
        compactHeaderCell(t, "Réf.");
        compactHeaderCell(t, "Montant");
        compactHeaderCell(t, "Observation");
        compactHeaderCell(t, "Date & heure");
        compactHeaderCell(t, "Payé");

        DateTimeFormatter paidAtFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH'h'mm");
        for (PayerDirectory.InvoiceRowInfo inv : invoices) {
            compactCell(t, inv.reference(), Element.ALIGN_LEFT);
            compactCell(t, money(inv.amount()) + " GNF", Element.ALIGN_RIGHT);
            compactCell(t, inv.label(), Element.ALIGN_LEFT);
            java.time.LocalDateTime paidAt = paidAtByInvoiceId.get(inv.id());
            compactCell(t, paidAt != null ? paidAt.format(paidAtFormat) : "-", Element.ALIGN_CENTER);
            t.addCell(paidCell(inv.paid()));
        }
        return t;
    }

    /**
     * Date/heure du DERNIER paiement réussi reçu sur chaque facture de cet élève — une facture peut
     * être réglée en plusieurs fois (tranche payée en deux acomptes à des dates différentes), on garde
     * alors la plus récente. Couvre aussi bien un paiement simple ({@code Payment.invoiceId}) qu'un
     * paiement groupé fratrie ({@code groupLines}, où {@code invoiceId} reste null — voir
     * {@code PaymentService#initiateGroup}) : sans ce 2e cas, une facture réglée via un encaissement
     * groupé plusieurs enfants afficherait "-" à tort. Vide si l'élève n'a encore rien payé.
     */
    private java.util.Map<Long, java.time.LocalDateTime> paidAtByInvoiceId(Long studentId) {
        java.util.Map<Long, java.time.LocalDateTime> result = new java.util.HashMap<>();
        if (studentId == null) return result;
        for (Payment p : paymentRepository.findByStudentId(studentId)) {
            if (p.getStatus() != gn.lacaeducation.payments.domain.PaymentStatus.SUCCEEDED || p.getPaidAt() == null) continue;
            List<Long> invoiceIds = p.getInvoiceId() != null
                    ? List.of(p.getInvoiceId())
                    : parseGroupLines(p.getGroupLines()).stream().map(GroupLine::invoiceId).filter(java.util.Objects::nonNull).toList();
            for (Long invoiceId : invoiceIds) {
                result.merge(invoiceId, p.getPaidAt(), (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return result;
    }

    /**
     * Cumul de tout ce qui a été réellement encaissé à ce jour sur les factures de l'élève listées
     * ci-dessus (§retour comptable, point 3) — somme de {@code paidAmount} (le NET réellement reçu,
     * jamais {@code amount} qui, pour une facture remisée, reste le montant BRUT avant remise et
     * gonflerait ce total du montant de la remise). Distinct de "Encaissé aujourd'hui"
     * ({@code receipt.getAmount()}, l'encaissement précis de CE reçu) : un reçu émis un jour pour le
     * transport seul affiche ainsi le cumul complet (mensualité déjà réglée un autre jour + transport
     * de ce jour), là où "Encaissé aujourd'hui" reste le transport seul.
     */
    private BigDecimal sumPaidToDate(List<PayerDirectory.InvoiceRowInfo> invoices) {
        return invoices.stream()
                .map(PayerDirectory.InvoiceRowInfo::paidAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void compactHeaderCell(PdfPTable t, String label) {
        PdfPCell c = new PdfPCell(new com.lowagie.text.Phrase(label, font(FontFactory.HELVETICA_BOLD, 8.5f, java.awt.Color.WHITE)));
        c.setBackgroundColor(DARK);
        c.setPadding(3.5f);
        c.setBorderColor(LINE);
        t.addCell(c);
    }

    private void compactCell(PdfPTable t, String value, int align) {
        PdfPCell c = new PdfPCell(new com.lowagie.text.Phrase(value == null ? "-" : value, font(FontFactory.HELVETICA, 8.5f, DARK)));
        c.setPadding(3.5f);
        c.setBorderColor(LINE);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    /** OUI (vert) / NON (rouge) en gras — jamais dans la police neutre de {@link #compactCell}, pour
     * que le statut de règlement saute aux yeux sur le reçu papier. */
    private com.lowagie.text.pdf.PdfPCell paidCell(boolean paid) {
        java.awt.Color color = paid ? new java.awt.Color(20, 120, 40) : new java.awt.Color(170, 30, 30);
        com.lowagie.text.pdf.PdfPCell c = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(paid ? "OUI" : "NON", font(FontFactory.HELVETICA_BOLD, 8.5f, color)));
        c.setPadding(3.5f);
        c.setBorderColor(LINE);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    /** Total encore dû par l'élève, toutes factures confondues — mensualités, frais annexes, transport,
     * cantine, etc., toute facture encore générée et non soldée (§retour comptable) — jamais seulement
     * le solde de la facture réglée par ce paiement précis. Calculé côté serveur (jamais une valeur
     * transmise par le client au moment de l'encaissement, potentiellement incomplète sur une
     * fratrie). Rien n'est affiché si l'élève n'a plus aucune facture due. Le libellé "Reste à payer"
     * n'est ni souligné ni accompagné d'un filet/bordure (retour comptable : aucun trait sous ce
     * libellé), juste en gras. */
    /** Ligne compacte (sans tableau ni filet, même style que {@link #remainingLine}) pour "Encaissé
     * aujourd'hui" — garde la place occupée par ce 2e total au minimum, le filet/encadré restant
     * réservé au total le plus important visuellement : "Total payé" (cumul, via {@code totalRow}
     * partagé avec facture/fiche de paie). Évite de refaire déborder le reçu sur une 2e page
     * maintenant qu'il y a deux totaux au lieu d'un seul (§retour comptable). */
    /**
     * Bloc coloré "Libellé : Montant" (§retour comptable : "stylise les trois lignes" — Encaissé
     * aujourd'hui / Total payé / Reste à payer) — remplace l'ancien mélange d'un tableau bordé
     * ({@code AdminDocumentLayout#totalRow}) pour "Total payé" et de simples lignes de texte pour les
     * deux autres, par un style UNIQUE et cohérent pour les trois, seule la couleur changeant selon
     * l'importance (Total payé en couleur pleine de l'école, Encaissé aujourd'hui en teinte légère,
     * Reste à payer en orange d'alerte — voir {@link #remainingLine}). Padding volontairement réduit
     * (5f) : ce bloc s'ajoute à deux/trois autres sur le même reçu, jamais au prix d'un débordement
     * sur une 2e page (§retour comptable, point récurrent sur ce fichier).
     */
    private void amountBox(Document doc, String label, String amount, java.awt.Color bg, java.awt.Color textColor) {
        PdfPTable t = new PdfPTable(new float[]{2f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(3f);
        PdfPCell l = new PdfPCell(new com.lowagie.text.Phrase(label, font(FontFactory.HELVETICA_BOLD, 9.5f, textColor)));
        l.setBackgroundColor(bg);
        l.setBorder(0);
        l.setPadding(5f);
        PdfPCell v = new PdfPCell(new com.lowagie.text.Phrase(amount, font(FontFactory.HELVETICA_BOLD, 10f, textColor)));
        v.setBackgroundColor(bg);
        v.setBorder(0);
        v.setPadding(5f);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(l);
        t.addCell(v);
        doc.add(t);
    }

    /** Teinte d'alerte (orange) pour "Reste à payer" — seule ligne des trois signalant un montant
     * encore dû, distincte à dessein des deux autres (argent déjà en caisse). Rien n'est affiché si
     * l'élève n'a plus aucune facture due (comportement inchangé). */
    private static final java.awt.Color REMAINING_BG = new java.awt.Color(254, 243, 199);
    private static final java.awt.Color REMAINING_TEXT = new java.awt.Color(146, 64, 14);

    private void remainingLine(Document doc, Long studentId) {
        BigDecimal total = payerDirectory.unpaidInvoices(studentId).stream()
                .map(PayerDirectory.UnpaidInvoiceInfo::remaining)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) return;
        amountBox(doc, "Reste à payer", money(total) + " GNF", REMAINING_BG, REMAINING_TEXT);
    }

    /**
     * Remise(s) (exonération, avantage personnel, paiement anticipé…) portée(s) par les factures
     * réglées par ce paiement — une ligne PAR MOTIF DISTINCT (nom de l'exonération/règle, pourcentage
     * déjà intégré au libellé — voir {@code InvoiceService#computeApplicableDiscount} /
     * {@code #applyEarlyPaymentDiscount} —, et montant réellement déduit), visible sur le reçu même
     * quand la remise a été calculée et appliquée automatiquement avant l'encaissement. Un même
     * paiement peut, rarement, couvrir des factures à motifs différents (ex. exonération modifiée
     * entre deux générations) : les agréger sous un seul motif masquerait alors une partie de la
     * remise réellement appliquée (§retour comptable).
     */
    private List<String> discountNotes(Payment payment) {
        List<GroupLine> groupLines = parseGroupLines(payment.getGroupLines());
        List<Long> invoiceIds = !groupLines.isEmpty()
                ? groupLines.stream().map(GroupLine::invoiceId).toList()
                : (payment.getInvoiceId() != null ? List.of(payment.getInvoiceId()) : List.of());
        java.util.Map<String, BigDecimal> totalByReason = new java.util.LinkedHashMap<>();
        for (Long id : invoiceIds) {
            var info = payerDirectory.invoiceFeeInfo(id).orElse(null);
            if (info == null || info.discountAmount() == null || info.discountAmount().signum() <= 0) continue;
            String reason = info.discountReason() != null && !info.discountReason().isBlank()
                    ? info.discountReason() : "Remise";
            totalByReason.merge(reason, info.discountAmount(), BigDecimal::add);
        }
        return totalByReason.entrySet().stream()
                .map(e -> "Remise appliquée : -" + money(e.getValue()) + " GNF (" + e.getKey() + ")")
                .toList();
    }

    /**
     * Crédit reporté (ex. transfert de classe, §retour comptable) apparaissant sur une ou plusieurs
     * factures du tableau ci-dessus — sans cette note, une facture déjà réglée sans encaissement
     * correspondant ne s'expliquerait pas pour le comptable. Une ligne par facture créditée (pas
     * agrégé par motif comme {@link #discountNotes} : chaque facture garde son propre libellé, ex.
     * "Tranche 2 (novembre, décembre, janvier)").
     */
    private List<String> creditNotes(List<PayerDirectory.InvoiceRowInfo> invoices) {
        return invoices.stream()
                .filter(i -> i.creditedAmount() != null && i.creditedAmount().signum() > 0)
                .map(i -> "Crédit reporté sur " + i.label() + " : +" + money(i.creditedAmount()) + " GNF"
                        + (i.creditReason() != null && !i.creditReason().isBlank() ? " (" + i.creditReason() + ")" : ""))
                .toList();
    }

    /**
     * Part du paiement due au frais d'usage plateforme (§billing) — affichée pour que la ligne ne
     * disparaisse jamais silencieusement du reçu même quand l'élève a bien accepté l'accès plateforme.
     */
    private String platformFeeNote(Payment payment) {
        List<GroupLine> groupLines = parseGroupLines(payment.getGroupLines());
        List<Long> invoiceIds = !groupLines.isEmpty()
                ? groupLines.stream().map(GroupLine::invoiceId).toList()
                : (payment.getInvoiceId() != null ? List.of(payment.getInvoiceId()) : List.of());
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : invoiceIds) {
            var info = payerDirectory.invoiceFeeInfo(id).orElse(null);
            if (info == null || info.platformFeeAmount() == null || info.platformFeeAmount().signum() <= 0) continue;
            total = total.add(info.platformFeeAmount());
        }
        if (total.signum() <= 0) return null;
        return "Dont frais d'usage plateforme : " + money(total) + " GNF";
    }

    /** Bloc de signature du comptable (§retour comptable, point 2) : le libellé "Signature et Cachet
     * du Comptable", puis un espace blanc (SANS cadre : le comptable ne voulait pas de bordure
     * visible, seulement de la place) où il appose sa signature et son cachet à la main, et enfin —
     * en dessous de cet espace, pas au-dessus — son nom et son téléphone PERSONNEL, distinct du
     * "Tél :" de l'école affiché en en-tête. Le reçu ne doit JAMAIS déborder sur une deuxième page
     * (retour comptable) : une valeur fixe (essayé 80pt, puis 40pt) reste sujette au débordement dès
     * que le tableau de factures au-dessus est long — la hauteur de l'espace est donc calculée à
     * partir de ce qu'il reste réellement avant le bas de page, plutôt qu'une constante. L'espacement
     * AVANT le libellé lui-même est également réduit quand la place restante est déjà faible : un
     * `spacingBefore` fixe de 24pt pouvait, à lui seul, faire basculer le libellé (et donc tout le
     * bloc nom/téléphone qui le suit) sur une deuxième page même quand il ne restait qu'une poignée
     * de points sur la première — c'est ce qui causait le nom/téléphone imprimés seuls en page 2.
     */
    private void comptableSignatureBlock(Document doc, PdfWriter writer, Receipt receipt, Long collectedByUserId) {
        float availableBeforeLabel = writer.getVerticalPosition(true) - doc.bottom();
        Paragraph label = new Paragraph("Signature et Cachet du Comptable",
                font(FontFactory.HELVETICA_BOLD, 9.5f, DARK));
        label.setAlignment(Element.ALIGN_RIGHT);
        label.setSpacingBefore(availableBeforeLabel > 70f ? 10f : 3f);
        doc.add(label);
        // Place restante avant le bas de page, moins la place nécessaire au nom/téléphone qui suivent
        // (~22pt pour deux lignes) : l'espace de signature se réduit tout seul si le contenu
        // au-dessus était déjà long, au lieu d'imposer une hauteur fixe qui pousserait le nom/téléphone
        // sur une deuxième page.
        float remaining = writer.getVerticalPosition(true) - doc.bottom() - 22f;
        float spacerHeight = Math.max(6f, Math.min(24f, remaining));
        Paragraph spacer = new Paragraph(" ", font(FontFactory.HELVETICA, 9.5f, GREY));
        spacer.setSpacingBefore(spacerHeight);
        doc.add(spacer);
        String operator = receipt.getCollectedByName();
        if (operator != null && !operator.isBlank()) {
            Paragraph name = new Paragraph(operator, font(FontFactory.HELVETICA, 9.5f, GREY));
            name.setAlignment(Element.ALIGN_RIGHT);
            doc.add(name);
            String phone = payerDirectory.operatorPhone(collectedByUserId).orElse(null);
            if (phone != null) {
                Paragraph phoneLine = new Paragraph("Tél : " + phone, font(FontFactory.HELVETICA, 9, GREY));
                phoneLine.setAlignment(Element.ALIGN_RIGHT);
                phoneLine.setSpacingBefore(2f);
                doc.add(phoneLine);
            }
        }
    }

    private List<GroupLine> parseGroupLines(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<GroupLine>>() {});
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private static String channelLabel(Payment p) {
        if (p.getChannel() == null) return "-";
        return switch (p.getChannel()) {
            case CASH -> "Espèces";
            case ORANGE_MONEY -> "Orange Money";
            case BANK_CARD -> "Virement bancaire";
            case MTN_MONEY -> "MTN Money";
            case WAVE -> "Wave";
            case DJOMY -> "En ligne (Djomy)";
        };
    }
}
