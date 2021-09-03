package com.carl.geek.service.c.service;

import com.carl.geek.api.CrossDatabaseBean;
import com.carl.geek.api.CrossDbLocalOpBean;
import com.carl.geek.api.CrossDbOp;
import com.carl.geek.api.ServiceAccountOperate;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.hmily.annotation.HmilyTCC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author carl.che
 */
@Service
@Slf4j
public class SomeService implements CrossDbOp {

    @DubboReference(version = "1.0.0", group = "a")
    private ServiceAccountOperate aService;

    @DubboReference(version = "1.0.0", group = "b")
    private ServiceAccountOperate bService;


    @Override
    @HmilyTCC(confirmMethod = "crossDbOpConfirm", cancelMethod = "crossDbOpCancel")
    public boolean crossDbOp(CrossDatabaseBean crossDatabaseBean){
        // 转账中
        String fromUserId = crossDatabaseBean.getFromUserId();
        String toUserId = crossDatabaseBean.getToUserId();
        Integer accountType = crossDatabaseBean.getAccountType();
        BigDecimal amount = crossDatabaseBean.getAmount();

        CrossDbLocalOpBean aOpBean = new CrossDbLocalOpBean();
        aOpBean.setTargetUserId(fromUserId);
        aOpBean.setAccountType(accountType);
        aOpBean.setAmount(amount.negate());
        aService.crossDbLocalOp(aOpBean);

        CrossDbLocalOpBean bOpBean = new CrossDbLocalOpBean();
        bOpBean.setTargetUserId(toUserId);
        bOpBean.setAccountType(accountType);
        bOpBean.setAmount(amount);
        bService.crossDbLocalOp(bOpBean);
        return true;
    }

    public boolean crossDbOpConfirm(CrossDatabaseBean crossDatabaseBean){
        //转账成功
        return true;
    }

    public boolean crossDbOpCancel(CrossDatabaseBean crossDatabaseBean){
        //转账失败
        return true;
    }




}
