package com.integration.demos.resources;

import com.integration.demos.model.Benefitiary;
import com.integration.demos.service.BenefitiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.atomic.AtomicLong;

@RestController
public class BenefitsResource {

    public static final String template= "Hello, %s";
    private final AtomicLong    counter = new AtomicLong();

    @Autowired
    private BenefitiaryService benefitiaryService;

    @RequestMapping(value = "/benefitiary", method = RequestMethod.GET)
    public Benefitiary getBenefitiaryInfo(@RequestParam(value="beneId") String beneId){

        return benefitiaryService.getBenefitiaryByBeneId(beneId);
    }

    @RequestMapping(value = "/benefitiary", method = RequestMethod.POST)
    public Benefitiary saveBenefitiaryInfo(@RequestBody Benefitiary benefitiary){
        return benefitiaryService.saveBenefitiary(benefitiary);
    }


    @RequestMapping(value = "/enrollment", method = RequestMethod.POST)
    public Benefitiary saveEnrollment(@RequestBody Benefitiary benefitiary){
        return benefitiaryService.saveBenefitiary(benefitiary);
    }


}

