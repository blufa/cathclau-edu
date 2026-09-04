package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.staff.application.service.TimetableProposalService;
import gn.lacaeducation.staff.application.service.TimetableProposalService.TimetableProposal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Timetable Generation", description = "Proposition automatique d'emploi du temps selon les disponibilités")
@RestController
@RequestMapping("/api/v1/staff/timetable")
@RequiredArgsConstructor
public class TimetableProposalController {

    private final TimetableProposalService service;

    public record ProposeRequest(List<Long> classIds) {}

    @Operation(summary = "Proposer un emploi du temps (sans persister) à partir des disponibilités enseignants")
    @PreAuthorize("hasAuthority('emploi:manage')")
    @PostMapping("/propose")
    public TimetableProposal propose(@RequestBody ProposeRequest r) {
        return service.propose(r.classIds() != null ? r.classIds() : List.of());
    }
}
