package it.alessiogori.battledebrief.auth.service;

import it.alessiogori.battledebrief.auth.dto.LoginRequest;
import it.alessiogori.battledebrief.auth.dto.LoginResponse;
import it.alessiogori.battledebrief.auth.dto.RegisterRequest;
import it.alessiogori.battledebrief.auth.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
