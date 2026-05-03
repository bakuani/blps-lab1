package ru.urasha.callmeani.blps.api.exception;

public class FeatureCategoryNotFoundException extends NotFoundException {

    public FeatureCategoryNotFoundException(Long id) {
        super("Feature category not found: " + id);
    }
}

