package com.carl.geek.service.c.controller;

import com.carl.geek.service.c.service.SomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author carl.che
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/service-c/")
public class ServiceController {

    private final SomeService someService;


    @GetMapping("dubbo-op")
    public String dubboOp(@RequestParam("flag") Integer flag){
        someService.op();
        return "success";
    }


}
