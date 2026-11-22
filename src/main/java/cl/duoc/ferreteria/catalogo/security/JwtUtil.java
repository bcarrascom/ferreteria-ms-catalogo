package cl.duoc.ferreteria.catalogo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Valida los tokens que emite ms-usuarios. ms-catalogo no emite tokens ni llama a ms-usuarios:
 * le basta verificar la firma con el mismo secreto (JWT_SECRET) y exigir el mismo emisor.
 */
@Component
public class JwtUtil {

    public static final String EMISOR = "ferreteria-ms-usuarios";

    private final SecretKey clave;

    public JwtUtil(@Value("${jwt.secret}") String secretoBase64) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
    }

    /** Devuelve los claims si el token es válido, de tipo "access" y no expiró. Si no, lanza JwtException. */
    public Claims validarAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)          // firma HS256 con el secreto compartido
                .requireIssuer(EMISOR)      // solo tokens emitidos por ms-usuarios
                .build()
                .parseSignedClaims(token)   // también revisa la expiración (exp)
                .getPayload();
        if (!"access".equals(claims.get("tipo", String.class))) {
            throw new JwtException("El token no es de acceso");   // un refresh token no sirve aquí
        }
        return claims;
    }

    protected SecretKey getClave() {
        return clave;
    }
}
