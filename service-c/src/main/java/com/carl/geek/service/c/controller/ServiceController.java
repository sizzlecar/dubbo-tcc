package com.carl.geek.service.c.controller;

import com.carl.geek.api.CrossDatabaseBean;
import com.carl.geek.service.c.service.SomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author carl.che
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/service-c/")
public class ServiceController {

    private final SomeService someService;


    @PostMapping("cross-db-op")
    public String crossDbOp(@RequestBody @Validated CrossDatabaseBean crossDatabaseBean){
        someService.crossDbOp(crossDatabaseBean);
        return "success";
    }


}
