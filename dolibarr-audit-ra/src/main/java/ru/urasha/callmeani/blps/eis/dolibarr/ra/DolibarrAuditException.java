package ru.urasha.callmeani.blps.eis.dolibarr.ra;

public class DolibarrAuditException extends RuntimeException {

    public DolibarrAuditException(String message) {
        super(message);
    }

    public DolibarrAuditException(String message, Throwable cause) {
        super(message, cause);
    }
}

