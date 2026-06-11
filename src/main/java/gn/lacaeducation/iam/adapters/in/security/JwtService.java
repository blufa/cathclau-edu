package gn.lacaeducation.iam.adapters.in.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import gn.lacaeducation.iam.application.port.out.TokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtService implements TokenGenerator {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    @Override
    public String generate(Long userId, Long tenantId, List<String> roles) {
        try {
            Date now = new Date();
            Date exp = new Date(now.getTime() + expiration * 1000);

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("roles", roles)
                    .issueTime(now)
                    .expirationTime(exp);

            if (tenantId != null) {
                builder.claim("tenant_id", tenantId);
            }

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT generation failed", e);
        }
    }
}
