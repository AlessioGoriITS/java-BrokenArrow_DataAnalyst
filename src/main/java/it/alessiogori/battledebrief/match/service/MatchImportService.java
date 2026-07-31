package it.alessiogori.battledebrief.match.service;

import it.alessiogori.battledebrief.match.dto.MatchImportRequest;
import it.alessiogori.battledebrief.match.dto.MatchImportResponse;
import jakarta.validation.Valid;

public interface MatchImportService {

    MatchImportResponse importMatches(@Valid MatchImportRequest request);
}
