package com.employeemanager.service;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.repository.CompanySettingsRepository;
import com.employeemanager.service.impl.BillingoInvoicingService;
import com.employeemanager.service.impl.NavExportInvoicingService;
import com.employeemanager.service.impl.SzamlazzHuInvoicingService;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory számlázási szolgáltatások létrehozására
 *
 * A CompanySettings alapján kiválasztja a megfelelő backend implementációt.
 */
@Slf4j
public class InvoicingServiceFactory {

    private final CompanySettingsRepository settingsRepository;
    private final NavExportInvoicingService navExportService;
    private final SzamlazzHuInvoicingService szamlazzHuService;
    private final BillingoInvoicingService billingoService;

    public InvoicingServiceFactory(
            CompanySettingsRepository settingsRepository,
            NavExportInvoicingService navExportService,
            SzamlazzHuInvoicingService szamlazzHuService,
            BillingoInvoicingService billingoService) {
        this.settingsRepository = settingsRepository;
        this.navExportService = navExportService;
        this.szamlazzHuService = szamlazzHuService;
        this.billingoService = billingoService;
    }

    /**
     * Aktuális számlázási szolgáltatás lekérdezése a beállítások alapján
     *
     * @return aktív számlázási szolgáltatás
     */
    public InvoicingService getCurrentService() {
        try {
            CompanySettings settings = settingsRepository.get();
            String backend = settings.getInvoicingBackend();

            InvoicingService.BackendType backendType =
                InvoicingService.BackendType.fromCode(backend);

            log.info("Selecting invoicing backend: {}", backendType.getDisplayName());

            return switch (backendType) {
                case SZAMLAZZ_HU -> szamlazzHuService;
                case BILLINGO -> billingoService;
                default -> navExportService;
            };

        } catch (Exception e) {
            log.error("Error selecting invoicing service, falling back to NAV export", e);
            return navExportService;
        }
    }

    /**
     * Specifikus backend szolgáltatás lekérdezése típus alapján
     *
     * @param backendType kívánt backend típus
     * @return számlázási szolgáltatás
     */
    public InvoicingService getService(InvoicingService.BackendType backendType) {
        return switch (backendType) {
            case SZAMLAZZ_HU -> szamlazzHuService;
            case BILLINGO -> billingoService;
            case NAV_EXPORT -> navExportService;
        };
    }
}
