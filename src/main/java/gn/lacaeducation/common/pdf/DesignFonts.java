package gn.lacaeducation.common.pdf;

import com.lowagie.text.pdf.BaseFont;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * Polices embarquées pour les documents "design premium" (§retour fondateur, refonte des documents de
 * /features/document-school) : Cormorant Garamond, Sora, JetBrains Mono — mêmes polices Google Fonts
 * que la référence HTML, sources officielles (CatharsisFonts/Cormorant, sora-xor/sora-font,
 * JetBrains/JetBrainsMono, toutes licence OFL), fichiers statiques dans {@code resources/fonts/}.
 * Chargées une seule fois (coût non négligeable de {@link BaseFont#createFont}) et réutilisées par tous
 * les générateurs PDF concernés.
 */
@Slf4j
public final class DesignFonts {

    private DesignFonts() {}

    public static final BaseFont SERIF = load("CormorantGaramond-Regular.ttf");
    public static final BaseFont SERIF_ITALIC = load("CormorantGaramond-Italic.ttf");
    public static final BaseFont SERIF_SEMIBOLD = load("CormorantGaramond-SemiBold.ttf");
    public static final BaseFont SERIF_SEMIBOLD_ITALIC = load("CormorantGaramond-SemiBoldItalic.ttf");
    public static final BaseFont SERIF_BOLD = load("CormorantGaramond-Bold.ttf");
    public static final BaseFont SERIF_BOLD_ITALIC = load("CormorantGaramond-BoldItalic.ttf");

    public static final BaseFont SANS = load("Sora-Regular.ttf");
    public static final BaseFont SANS_SEMIBOLD = load("Sora-SemiBold.ttf");
    public static final BaseFont SANS_BOLD = load("Sora-Bold.ttf");
    public static final BaseFont SANS_EXTRABOLD = load("Sora-ExtraBold.ttf");

    public static final BaseFont MONO = load("JetBrainsMono-Medium.ttf");
    public static final BaseFont MONO_SEMIBOLD = load("JetBrainsMono-SemiBold.ttf");

    /** Charge une police TTF embarquée depuis le classpath ({@code resources/fonts/}) — {@code null} si
     * introuvable/corrompue, auquel cas l'appelant doit se replier sur une police standard OpenPDF
     * (Times/Helvetica/Courier) plutôt que de planter la génération du document. */
    private static BaseFont load(String fileName) {
        try (InputStream in = DesignFonts.class.getResourceAsStream("/fonts/" + fileName)) {
            if (in == null) {
                log.warn("[PDF] Police {} introuvable dans resources/fonts — repli sur une police standard.", fileName);
                return null;
            }
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(fileName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (IOException | RuntimeException e) {
            log.warn("[PDF] Échec chargement police {} — repli sur une police standard : {}", fileName, e.getMessage());
            return null;
        }
    }
}
