package gn.lacaeducation.platform.adapters.in.web.dto;
public record UpdateTenantRequest(String schoolName, String logo, String brandColor,
        String address, String city, String country, String phone, String email,
        String website, String director, String accreditationNumber, String openingAuthorization,
        String payoutMerchantNumber, String payoutOperator, String payoutAccountName,
        String motto, String slogan, String ire, String dpeLabel, String sousPrefectureLabel) {}
