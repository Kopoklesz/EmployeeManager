package com.employeemanager.repository.firebase;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.repository.CompanySettingsRepository;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

/**
 * Firebase CompanySettings Repository implementáció
 *
 * Singleton pattern: mindig csak egy beállítás dokumentum létezik (id = "default")
 */
@Slf4j
public class FirebaseCompanySettingsRepository extends BaseFirebaseRepository implements CompanySettingsRepository {

    private static final String COLLECTION_NAME = "company_settings";
    private static final String SETTINGS_ID = "default";

    public FirebaseCompanySettingsRepository(Firestore firestore) {
        super(firestore, COLLECTION_NAME);
    }

    @Override
    public CompanySettings get() throws ExecutionException, InterruptedException {
        DocumentReference docRef = getCollection().document(SETTINGS_ID);
        DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            return CompanySettings.fromMap(document.getData());
        } else {
            // Ha még nem létezik, létrehozzuk alapértelmezett értékekkel
            log.info("CompanySettings not found, creating default");
            return createDefault();
        }
    }

    @Override
    public CompanySettings save(CompanySettings settings) throws ExecutionException, InterruptedException {
        // Biztosítjuk, hogy az ID mindig "default" legyen
        settings.setId(SETTINGS_ID);

        DocumentReference docRef = getCollection().document(SETTINGS_ID);
        docRef.set(settings.toMap()).get();

        log.info("CompanySettings saved to Firebase");

        return get();
    }

    @Override
    public synchronized String generateNextInvoiceNumber() throws ExecutionException, InterruptedException {
        CompanySettings settings = get();

        String invoiceNumber = settings.generateNextInvoiceNumber();

        // Mentjük a frissített számlaszám számlálót
        save(settings);

        log.info("Generated invoice number: {}", invoiceNumber);

        return invoiceNumber;
    }

    /**
     * Alapértelmezett CompanySettings létrehozása
     */
    private CompanySettings createDefault() throws ExecutionException, InterruptedException {
        CompanySettings settings = CompanySettings.builder()
            .id(SETTINGS_ID)
            .companyName("Cégem Kft.")
            .invoicePrefix("INV")
            .invoiceNextNumber(1)
            .defaultPaymentDeadlineDays(8)
            .defaultCurrency("HUF")
            .defaultVatRate(27.0)
            .invoicingBackend("NAV_EXPORT")
            .navTestMode(true)
            .build();

        return save(settings);
    }
}
