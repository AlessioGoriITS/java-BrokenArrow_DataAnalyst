package it.alessiogori.battledebrief.unit.dataset;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.alessiogori.battledebrief.common.exception.ImportValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClasspathCatalogDatasetReader implements CatalogDatasetReader {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Resource datasetResource;

    public ClasspathCatalogDatasetReader(
            ObjectMapper objectMapper,
            Validator validator,
            @Value("${app.catalog.dataset:classpath:catalog/units.json}")
            Resource datasetResource
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.datasetResource = datasetResource;
    }

    @Override
    public CatalogDataset read() {
        try (var inputStream = datasetResource.getInputStream()) {
            CatalogDataset dataset = objectMapper.readValue(
                    inputStream,
                    CatalogDataset.class
            );
            validate(dataset);
            return dataset;
        } catch (JacksonException exception) {
            throw new ImportValidationException(
                    "Catalog dataset contains invalid JSON"
            );
        } catch (IOException exception) {
            throw new ImportValidationException(
                    "Catalog dataset could not be read"
            );
        }
    }

    private void validate(CatalogDataset dataset) {
        Set<ConstraintViolation<CatalogDataset>> violations =
                validator.validate(dataset);
        if (violations.isEmpty()) {
            return;
        }

        String details = violations.stream()
                .sorted(Comparator.comparing(violation ->
                        violation.getPropertyPath().toString()))
                .map(violation -> violation.getPropertyPath()
                        + " " + violation.getMessage())
                .collect(Collectors.joining(", "));
        throw new ImportValidationException(
                "Catalog dataset validation failed: " + details
        );
    }
}
