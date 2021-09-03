package com.carl.geek.service.a.controller;

import com.carl.geek.api.ServiceAccountOperate;
import com.carl.geek.api.AccountOperateBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author carl.che
 */
@RestController
@RequestMapping("/api/service-a/")
@RequiredArgsConstructor
@Slf4j
public class ServiceController implements ApplicationContextAware {

    private final ServiceAccountOperate accountOperate;


    @PostMapping("/operate")
    public boolean operate(@Validated @RequestBody AccountOperateBean request) {
        return accountOperate.operate(request);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        System.out.println(applicationContext.containsBean("serviceController"));
    }
}
