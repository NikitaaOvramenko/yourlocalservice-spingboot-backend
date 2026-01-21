package com.nikita_ovramenko.sping_all_purpose_server.interfaces;

import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteDto;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;

public interface Mapper<E, D> {
    D toDto(E e);

    E toEntity(D d);

}
