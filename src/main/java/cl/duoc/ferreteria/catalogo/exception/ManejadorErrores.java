package cl.duoc.ferreteria.catalogo.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce toda excepción que sale de un controller al JSON de ErrorRespuesta. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Reglas de negocio: el código HTTP viene en la propia excepción. */
    @ExceptionHandler(NegocioException.class)
    ResponseEntity<ErrorRespuesta> negocio(NegocioException ex, HttpServletRequest req) {
        return responder(ex.getStatus(), ex.getMessage(), req, ex.getDetalles());
    }

    /** Bean Validation en un @RequestBody (@NotBlank, @Positive...): 400 con el error de cada campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud", req, detalles);
    }

    /** JSON roto, o un parámetro que no es del tipo esperado (por ejemplo ?categoria=abc). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorRespuesta> malFormada(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "La solicitud está mal formada", req, null);
    }

    /** Todo lo demás. Los errores propios de Spring MVC (404 de ruta, 405, 415...) conservan su código. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorRespuesta> otro(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse errorSpring) {
            HttpStatus status = HttpStatus.valueOf(errorSpring.getStatusCode().value());
            return responder(status, status.getReasonPhrase(), req, null);
        }
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null);
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus status, String mensaje,
                                                     HttpServletRequest req, Map<String, String> detalles) {
        return ResponseEntity.status(status).body(ErrorRespuesta.de(status, mensaje, req.getRequestURI(), detalles));
    }
}
