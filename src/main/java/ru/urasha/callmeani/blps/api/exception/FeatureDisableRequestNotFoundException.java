package ru.urasha.callmeani.blps.api.exception;

public class FeatureDisableRequestNotFoundException extends NotFoundException {
    public FeatureDisableRequestNotFoundException(Long id) {
        super("Feature disable request with id " + id + " not found");
    }
}
