package com.carl.geek.service.b.controller;

import com.carl.geek.api.AccountOperateBean;
import com.carl.geek.api.ServiceBAccountOperate;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author carl.che
 */
@RestController
@RequestMapping("/api/service-b/")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceBAccountOperate accountOperate;


    @PostMapping("/operate")
    public boolean operate(@Validated @RequestBody AccountOperateBean request) {
        return accountOperate.operate(request);
    }




}
