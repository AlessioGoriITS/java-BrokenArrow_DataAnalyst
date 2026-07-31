package it.alessiogori.battledebrief.auth.security;

public interface JwtService {

    IssuedJwt issueToken(AuthenticatedUser user);

    String parseSubject(String token);
}
