package it.alessiogori.battledebrief.unit.dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "docker"})
@ConditionalOnProperty(
        name = "app.catalog.initialize",
        havingValue = "true",
        matchIfMissing = true
)
public class CatalogDatasetInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            CatalogDatasetInitializer.class
    );

    private final CatalogDatasetReader datasetReader;
    private final CatalogDatasetImportService importService;

    public CatalogDatasetInitializer(
            CatalogDatasetReader datasetReader,
            CatalogDatasetImportService importService
    ) {
        this.datasetReader = datasetReader;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        CatalogDataset dataset = datasetReader.read();
        CatalogImportResult result = importService.importDataset(dataset);
        LOGGER.info(
                "Catalog dataset {} loaded: {} specializations, {} units",
                dataset.version(),
                result.specializationsProcessed(),
                result.unitsProcessed()
        );
    }
}
