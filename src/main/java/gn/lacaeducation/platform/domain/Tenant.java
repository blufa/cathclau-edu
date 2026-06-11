package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {
    private Long id;
    private String schoolName;
    private String logo;
    private String brandColor;
    private String address;
    private String city;
    private String country;
    private String phone;
    private String email;
    private String website;
    private String director;
    private String currentAcademicYear;
    private TenantStatus status;
    private LocalDateTime createdAt;
}
