package cl.duoc.ferreteria.catalogo.security;

/** Datos del usuario que vienen en el access token. Los controllers lo reciben con @AuthenticationPrincipal. */
public record UsuarioAutenticado(Long id, String correo, String nombre, String rol) { }
