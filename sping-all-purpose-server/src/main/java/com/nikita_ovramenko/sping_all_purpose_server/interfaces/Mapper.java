package com.nikita_ovramenko.sping_all_purpose_server.interfaces;

public interface Mapper<E, D> {
    D toDto(E e);

    E toEntity(D d);
}
