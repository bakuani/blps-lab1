package ru.urasha.callmeani.blps.api.exception;

public class TariffChangeRequestNotFoundException extends NotFoundException {
    public TariffChangeRequestNotFoundException(Long id) {
        super("Tariff change request with id " + id + " not found");
    }
}
