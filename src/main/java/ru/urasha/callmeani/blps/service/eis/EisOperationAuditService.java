package ru.urasha.callmeani.blps.service.eis;

import ru.urasha.callmeani.blps.eis.model.EisOperationResult;

public interface EisOperationAuditService {
    void registerOperationResult(EisOperationResult result);
}
