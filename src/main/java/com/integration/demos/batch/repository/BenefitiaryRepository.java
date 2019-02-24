package com.integration.demos.batch.repository;

import com.integration.demos.model.Benefitiary;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenefitiaryRepository extends PagingAndSortingRepository<Benefitiary, String> {
    Benefitiary findByName(String name);
    Benefitiary findByPhoneNumber(String phoneNumber);
    Benefitiary findByBeneId(String beneId);

}
