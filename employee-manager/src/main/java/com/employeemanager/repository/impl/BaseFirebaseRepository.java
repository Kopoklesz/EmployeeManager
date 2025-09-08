package com.employeemanager.repository.impl;

import com.employeemanager.repository.interfaces.BaseRepository;
import com.google.cloud.firestore.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseFirebaseRepository<T> implements BaseRepository<T, String> {
    protected final Firestore firestore;
    protected final String collectionName;
    protected final Class<T> entityClass;

    protected BaseFirebaseRepository(Firestore firestore, String collectionName, Class<T> entityClass) {
        this.firestore = firestore;
        this.collectionName = collectionName;
        this.entityClass = entityClass;
    }

    @Override
    public T save(T entity) throws ExecutionException, InterruptedException {
        String id = getEntityId(entity);
        DocumentReference docRef;

        if (id == null || id.isEmpty()) {
            // Új entitás - Firebase generálja az ID-t
            docRef = firestore.collection(collectionName).document();
            setEntityId(entity, docRef.getId());
            log.debug("Generated new ID: {} for collection: {}", docRef.getId(), collectionName);
        } else {
            // Meglévő entitás
            docRef = firestore.collection(collectionName).document(id);
            log.debug("Updating existing entity with ID: {} in collection: {}", id, collectionName);
        }

        // Entitás konvertálása Map-re a mentéshez
        Map<String, Object> data = convertToMap(entity);
        docRef.set(data).get();

        return entity;
    }

    @Override
    public List<T> saveAll(List<T> entities) throws ExecutionException, InterruptedException {
        WriteBatch batch = firestore.batch();
        List<T> savedEntities = new ArrayList<>();

        for (T entity : entities) {
            String id = getEntityId(entity);
            DocumentReference docRef;

            if (id == null || id.isEmpty()) {
                docRef = firestore.collection(collectionName).document();
                setEntityId(entity, docRef.getId());
            } else {
                docRef = firestore.collection(collectionName).document(id);
            }

            Map<String, Object> data = convertToMap(entity);
            batch.set(docRef, data);
            savedEntities.add(entity);
        }

        batch.commit().get();
        log.debug("Batch saved {} entities to collection: {}", savedEntities.size(), collectionName);
        return savedEntities;
    }

    @Override
    public Optional<T> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = firestore.collection(collectionName)
                .document(id)
                .get()
                .get();

        if (document.exists()) {
            Map<String, Object> data = document.getData();
            if (data != null) {
                data.put("id", document.getId()); // Ensure ID is included
                return Optional.ofNullable(convertFromMap(data));
            }
        }

        return Optional.empty();
    }

    @Override
    public List<T> findAll() throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(collectionName).get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    {
                        data.put("id", doc.getId()); // Ensure ID is included
                        return convertFromMap(data);
                    }
                })
                .filter(entity -> entity != null)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        firestore.collection(collectionName)
                .document(id)
                .delete()
                .get();
        log.debug("Deleted entity with ID: {} from collection: {}", id, collectionName);
    }

    /**
     * Get entity ID - to be implemented by subclasses
     */
    protected abstract String getEntityId(T entity);

    /**
     * Set entity ID - to be implemented by subclasses
     */
    protected abstract void setEntityId(T entity, String id);

    /**
     * Convert entity to Map for Firebase storage
     */
    protected abstract Map<String, Object> convertToMap(T entity);

    /**
     * Convert Map from Firebase to entity
     */
    protected abstract T convertFromMap(Map<String, Object> data);
}