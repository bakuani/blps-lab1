package ru.urasha.callmeani.blps.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.urasha.callmeani.blps.api.dto.billing.BillingTransactionDto;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;

@Mapper(componentModel = "spring")
public interface BillingMapper {
    @Mapping(target = "type", source = "type")
    BillingTransactionDto toBillingTransactionDto(BillingTransaction transaction);
}
