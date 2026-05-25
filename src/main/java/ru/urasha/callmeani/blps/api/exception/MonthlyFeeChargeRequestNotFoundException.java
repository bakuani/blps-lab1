package ru.urasha.callmeani.blps.api.exception;

public class MonthlyFeeChargeRequestNotFoundException extends NotFoundException {
    public MonthlyFeeChargeRequestNotFoundException(Long id) {
        super("Monthly fee charge request with id " + id + " not found");
    }
}
