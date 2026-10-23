package gn.lacaeducation.common.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Image;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Génère un QR code (ZXing) encodant une chaîne courte (ex. référence + montant d'un reçu de paiement,
 * pour vérification terrain) et le rend directement en {@link Image} OpenPDF — jamais une exception au
 * client final : {@code null} si l'encodage échoue (contenu vide, caractères non supportés…), auquel
 * cas l'appelant doit simplement omettre le QR plutôt que de faire échouer tout le document.
 */
@Slf4j
public final class QrCodeImage {

    private QrCodeImage() {}

    public static Image render(String content, int sizePx) {
        if (content == null || content.isBlank()) return null;
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0);
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            BufferedImage buffered = new BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    buffered.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", out);
            return Image.getInstance(out.toByteArray());
        } catch (WriterException | java.io.IOException | RuntimeException e) {
            log.warn("QR code non généré ({} caractères) : {}", content.length(), e.getMessage());
            return null;
        }
    }
}
