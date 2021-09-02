package com.carl.geek.service.c.service;

import com.carl.geek.api.AccountOperate;
import com.carl.geek.api.AccountOperateBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author carl.che
 */
@Service
@Slf4j
public class SomeService {

    @DubboReference(version = "1.0.0", group = "a")
    private  AccountOperate accountOperate;

    public void op(){
        AccountOperateBean accountOperateBean = new AccountOperateBean();
        accountOperateBean.setAccountType(1);
        accountOperateBean.setFromUserId("zhangsan");
        accountOperateBean.setToUserId("lisi");
        accountOperateBean.setMoney(new BigDecimal("10"));
        boolean operate = accountOperate.operate(accountOperateBean);
    }

}
