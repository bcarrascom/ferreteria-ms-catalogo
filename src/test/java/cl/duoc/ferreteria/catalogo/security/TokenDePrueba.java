package cl.duoc.ferreteria.catalogo.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Genera tokens con el MISMO formato que ms-usuarios, firmados con el secreto de prueba de
 * src/test/resources/application.properties. Permite probar 401/403 sin levantar ms-usuarios.
 */
public final class TokenDePrueba {

    private static final SecretKey CLAVE = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
            "Y2xhdmUtZGUtcHJ1ZWJhLXNvbG8tcGFyYS10ZXN0cy1uby1lcy1sYS1yZWFs"));

    private static final SecretKey OTRA_CLAVE = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
            "b3RyYS1jbGF2ZS1xdWUtbm8tZXMtbGEtZGUtbG9zLXNlcnZpY2lvcy1kZS1wcnVlYmE="));

    private TokenDePrueba() { }

    public static String access(String rol) {
        return crear(CLAVE, JwtUtil.EMISOR, rol, "access", 15);
    }

    public static String expirado(String rol) {
        return crear(CLAVE, JwtUtil.EMISOR, rol, "access", -5);
    }

    public static String refresh() {
        return crear(CLAVE, JwtUtil.EMISOR, "CLIENTE", "refresh", 60);
    }

    public static String firmadoConOtraClave(String rol) {
        return crear(OTRA_CLAVE, JwtUtil.EMISOR, rol, "access", 15);
    }

    public static String deOtroEmisor(String rol) {
        return crear(CLAVE, "emisor-desconocido", rol, "access", 15);
    }

    private static String crear(SecretKey clave, String emisor, String rol, String tipo, long minutos) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(emisor)
                .subject("1")
                .claim("correo", "prueba@ferreteria.cl")
                .claim("nombre", "Usuario Prueba")
                .claim("rol", rol)
                .claim("tipo", tipo)
                .issuedAt(Date.from(ahora.minus(10, ChronoUnit.MINUTES)))
                .expiration(Date.from(ahora.plus(minutos, ChronoUnit.MINUTES)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }
}
