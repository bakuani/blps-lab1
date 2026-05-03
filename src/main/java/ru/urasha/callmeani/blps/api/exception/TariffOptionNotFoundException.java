package ru.urasha.callmeani.blps.api.exception;

public class TariffOptionNotFoundException extends NotFoundException {

    public TariffOptionNotFoundException(Long id) {
        super("Tariff option not found: " + id);
    }
}

