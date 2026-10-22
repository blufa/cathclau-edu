package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.out.PrimaryTimetableSettingsRepository;
import gn.lacaeducation.academics.domain.PrimaryTimetableSettings;
import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrimaryTimetableSettingsServiceTest {

    @Mock PrimaryTimetableSettingsRepository repo;
    @InjectMocks PrimaryTimetableSettingsService service;

    @Test
    void getSettings_noneSaved_returnsDefaults() {
        when(repo.findByTenantId(10L)).thenReturn(Optional.empty());

        PrimaryTimetableSettings s = service.getSettings(10L);

        assertThat(s.getGridStartTimeOrDefault()).isEqualTo(LocalTime.of(8, 0));
        assertThat(s.getGridEndTimeOrDefault()).isEqualTo(LocalTime.of(18, 0));
        assertThat(s.getBandMinutesOrDefault()).isEqualTo(60);
    }

    @Test
    void updateGrid_startAfterEnd_throws() {
        assertThatThrownBy(() -> service.updateGrid(10L, LocalTime.of(14, 0), LocalTime.of(8, 0), 60))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateGrid_bandMinutesOutOfRange_throws() {
        assertThatThrownBy(() -> service.updateGrid(10L, LocalTime.of(7, 30), LocalTime.of(17, 0), 300))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateGrid_valid_savesAndReturns() {
        when(repo.findByTenantId(10L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        PrimaryTimetableSettings result = service.updateGrid(10L, LocalTime.of(7, 30), LocalTime.of(16, 30), 45);

        assertThat(result.getGridStartTime()).isEqualTo(LocalTime.of(7, 30));
        assertThat(result.getGridEndTime()).isEqualTo(LocalTime.of(16, 30));
        assertThat(result.getBandMinutes()).isEqualTo(45);
        verify(repo).save(any(PrimaryTimetableSettings.class));
    }
}
