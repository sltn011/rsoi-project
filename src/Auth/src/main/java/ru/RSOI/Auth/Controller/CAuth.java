package ru.RSOI.Auth.Controller;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import io.jsonwebtoken.impl.crypto.DefaultJwtSignatureValidator;
import org.springframework.web.bind.annotation.*;

import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/auth")
public class CAuth {

    private String secret;
    private String issuer;
    private SignatureAlgorithm sa;

    public class IsValidRes
    {
        public String valid;

        public IsValidRes(String valid)
        {
            this.valid = valid;
        }
    }

    public CAuth() {
        this.secret = "rsoi_auth";
        this.issuer = "http://authservice:9889/auth";
        this.sa = SignatureAlgorithm.HS256;
    }

    @GetMapping("/hello")
    public String healthcheck()
    {
        return "Hello from gateway!";
    }

    @GetMapping("/get")
    public String getAuthToken(@RequestBody Map<String, String> values)
    {
        String nickname = values.get("nickname");
        String email    = values.get("email");
        String role     = values.get("role");

        String id = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date exp = Date.from(LocalDateTime.now().plusMinutes(30)
                .atZone(ZoneId.systemDefault()).toInstant());

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", issuer);
        claims.put("jti", id);
        claims.put("iat", now);
        claims.put("nbf", now);
        claims.put("exp", exp);
        claims.put("nickname", nickname);
        claims.put("email", email);
        claims.put("role", role);

        String token = "";
        try {
            token = Jwts.builder()
                    .setClaims(claims)
                    .signWith(sa, secret)
                    .compact();
        } catch (JwtException e) {
            e.printStackTrace();
            //ignore
        }

        return token;
    }

    @GetMapping("/check")
    public IsValidRes isValid(@RequestBody Map<String, String> values)
    {
        String token = values.get("token");

        SecretKeySpec secretKeySpec = new SecretKeySpec(TextCodec.BASE64.decode(secret), sa.getJcaName());
        DefaultJwtSignatureValidator validator = new DefaultJwtSignatureValidator(sa, secretKeySpec);

        String[] chunks = token.split("\\.");
        String tokenWithoutSignature = chunks[0] + "." + chunks[1];
        String signature = chunks[2];
        String valid = String.valueOf(validator.isValid(tokenWithoutSignature, signature));
        return new IsValidRes(valid);
    }

}

