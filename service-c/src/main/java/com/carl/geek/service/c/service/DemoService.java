package com.carl.geek.service.c.service;

import com.carl.geek.api.AccountOperate;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DemoService {

    @DubboReference(version = "1.0.0")
    private  AccountOperate accountOperate;

    public void op(){

    }

}
