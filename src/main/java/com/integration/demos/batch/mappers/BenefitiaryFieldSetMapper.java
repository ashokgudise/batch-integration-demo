package com.integration.demos.batch.mappers;

import com.integration.demos.model.Benefitiary;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;


public class BenefitiaryFieldSetMapper implements FieldSetMapper<Benefitiary> {

    public Benefitiary mapFieldSet(FieldSet fieldSet) throws BindException {

        final Benefitiary benefitiary = new Benefitiary(fieldSet.readString("beneId").trim(),fieldSet.readString("phoneNumber").trim(),fieldSet.readString("name").trim());

        return benefitiary;
    }
}
