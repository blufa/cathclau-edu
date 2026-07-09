package gn.lacaeducation.announcements.application.service;

import gn.lacaeducation.announcements.application.port.in.AnnouncementUseCase.CreateAnnouncementCommand;
import gn.lacaeducation.announcements.application.port.out.AnnouncementRepository;
import gn.lacaeducation.announcements.domain.Announcement;
import gn.lacaeducation.announcements.domain.AnnouncementScope;
import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock AnnouncementRepository repo;
    @InjectMocks AnnouncementService service;

    @Test
    void create_general_setsPublishedAtAndClearsTargets() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Announcement a = service.create(new CreateAnnouncementCommand(
                1L, "Rentrée", "Le 15 septembre", AnnouncementScope.GENERAL, 5L, 9L, 7L, "Directeur"));

        assertThat(a.getScope()).isEqualTo(AnnouncementScope.GENERAL);
        assertThat(a.getClassId()).isNull();   // ciblage ignoré pour une annonce générale
        assertThat(a.getStudentId()).isNull();
        assertThat(a.getPublishedAt()).isNotNull();
    }

    @Test
    void create_blankTitle_throws() {
        assertThatThrownBy(() -> service.create(new CreateAnnouncementCommand(
                1L, "  ", "x", AnnouncementScope.GENERAL, null, null, 7L, "D")))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_classScopeWithoutClassId_throws() {
        assertThatThrownBy(() -> service.create(new CreateAnnouncementCommand(
                1L, "Sortie", "x", AnnouncementScope.CLASS, null, null, 7L, "D")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_studentScope_keepsStudentId() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Announcement a = service.create(new CreateAnnouncementCommand(
                1L, "Convocation", "x", AnnouncementScope.STUDENT, null, 9L, 7L, "D"));

        assertThat(a.getStudentId()).isEqualTo(9L);
        assertThat(a.getClassId()).isNull();
    }
}
