package ru.urasha.callmeani.blps.api.exception;

public class TariffNotFoundException extends NotFoundException {

    public TariffNotFoundException(Long id) {
        super("Tariff not found: " + id);
    }
}

