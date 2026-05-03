package ru.urasha.callmeani.blps.api.exception;

public class TariffCategoryNotFoundException extends NotFoundException {

    public TariffCategoryNotFoundException(Long id) {
        super("Tariff category not found: " + id);
    }
}

