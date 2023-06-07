package ru.RSOI.Auth.Controller;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import io.jsonwebtoken.impl.crypto.DefaultJwtSignatureValidator;
import org.json.JSONObject;
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

    public class TokenRes
    {
        public String token;

        public TokenRes(String token)
        {
            this.token = token;
        }
    }

    public class IsValidRes
    {
        public String valid;

        public IsValidRes(String valid)
        {
            this.valid = valid;
        }
    }

    public class UserInfo
    {
        public String username;
        public String role;

        public UserInfo(String username, String role)
        {
            this.username = username;
            this.role = role;
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
    public TokenRes getAuthToken(@RequestParam String username, @RequestParam String email, @RequestParam String role)
    {
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
        claims.put("nickname", username);
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

        return new TokenRes(token);
    }

    @GetMapping("/check")
    public IsValidRes isValid(@RequestHeader(value = "Authorization", required = false) String token)
    {
        String valid = String.valueOf(isTokenValid(token));
        return new IsValidRes(valid);
    }

    @GetMapping("/info")
    public UserInfo getUserInfo(@RequestHeader(value = "Authorization", required = false) String token)
    {
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String dec = new String(decoder.decode(chunks[1]));
        System.out.println(dec);
        JSONObject obj = new JSONObject(dec);

        String username = obj.getString("nickname");
        String role = obj.getString("role");

        return new UserInfo(username, role);
    }

    public boolean isTokenValid(String token)
    {
        SecretKeySpec secretKeySpec = new SecretKeySpec(TextCodec.BASE64.decode(secret), sa.getJcaName());
        DefaultJwtSignatureValidator validator = new DefaultJwtSignatureValidator(sa, secretKeySpec);

        String[] chunks = token.split("\\.");
        String tokenWithoutSignature = chunks[0] + "." + chunks[1];
        String signature = chunks[2];
        return validator.isValid(tokenWithoutSignature, signature);
    }

}

