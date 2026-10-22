package gn.lacaeducation.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaUploadServiceTest {

    // Identifiants volontairement vides : évite tout appel réseau réel vers Cloudinary dans ces
    // tests — uploadBytes() refuse avant de tenter la requête HTTP ("Cloudinary non configuré").
    private final MediaUploadService service = new MediaUploadService("", "", "", new ObjectMapper());

    @Test
    void upload_emptyFile_rejected() {
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> service.upload(file, "students/photos"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void upload_nonImageInPhotosFolder_rejected() {
        // §11.3 cahier des charges : un fichier non-image ne doit jamais devenir une photo de profil.
        var file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.upload(file, "staff/photos"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("image");
    }

    @Test
    void upload_nonImageInDocumentsFolder_notRejectedByTypeCheck() {
        // Un dossier non "*/photos" (ex. justificatifs) n'est pas restreint aux images. Avec des
        // identifiants Cloudinary vides, l'appel échoue quand même — mais pour une raison différente
        // ("non configuré"), jamais pour le contrôle de type ajouté ici.
        var file = new MockMultipartFile("file", "justificatif.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.upload(file, "students/documents"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("configuré")
                .hasMessageNotContaining("image");
    }
}
