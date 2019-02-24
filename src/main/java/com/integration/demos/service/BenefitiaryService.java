package com.integration.demos.service;

import com.integration.demos.batch.repository.BenefitiaryRepository;
import com.integration.demos.model.Benefitiary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BenefitiaryService {

    @Autowired
    private BenefitiaryRepository benefitiaryRepository;


    public Benefitiary getBenefitiaryByName(String name){
        return benefitiaryRepository.findByName(name);
    }

    public Benefitiary getBenefitiaryByPhoneNumber(String phoneNumber){
        return benefitiaryRepository.findByPhoneNumber(phoneNumber);
    }

    public Benefitiary getBenefitiaryByBeneId(String beneId){
        return benefitiaryRepository.findByBeneId(beneId);
    }

    public Benefitiary saveBenefitiary(Benefitiary benefitiary){
        return benefitiaryRepository.save(benefitiary);
    }
}
