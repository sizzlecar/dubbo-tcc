package com.carl.geek.service.c.service;

import com.carl.geek.api.*;
import com.carl.geek.service.c.dao.TransferLogMapperExt;
import com.carl.geek.service.c.model.TransferLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.hmily.annotation.HmilyTCC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author carl.che
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SomeService implements CrossDbOp {

    @DubboReference(version = "1.0.0", group = "a")
    private ServiceAAccountOperate aService;

    @DubboReference(version = "1.0.0", group = "b")
    private ServiceBAccountOperate bService;

    private final TransferLogMapperExt transferLogMapperExt;

    @Override
    @HmilyTCC(confirmMethod = "crossDbOpConfirm", cancelMethod = "crossDbOpCancel")
    @Transactional(rollbackFor = Exception.class)
    public boolean crossDbOp(CrossDatabaseBean crossDatabaseBean){
        TransferLog insertModel = new TransferLog();
        insertModel.setFromUserId(crossDatabaseBean.getFromUserId());
        insertModel.setToUserId(crossDatabaseBean.getToUserId());
        insertModel.setAmount(crossDatabaseBean.getAmount());
        insertModel.setCreateTime(new Date());
        insertModel.setUpdateTime(new Date());
        // 转账中
        insertModel.setStatus(Byte.parseByte("0"));
        transferLogMapperExt.insert(insertModel);
        crossDatabaseBean.setLogId(insertModel.getId());
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
        if( 1 == 1 )  throw new RuntimeException("11111");
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean crossDbOpConfirm(CrossDatabaseBean crossDatabaseBean){
        log.info("crossDbOpConfirm执行中");
        Integer logId = crossDatabaseBean.getLogId();
        if(logId != null){
            TransferLog updateModel = new TransferLog();
            updateModel.setUpdateTime(new Date());
            updateModel.setId(crossDatabaseBean.getLogId());
            // 转账成功
            updateModel.setStatus(Byte.parseByte("1"));
            transferLogMapperExt.updateByPrimaryKeySelective(updateModel);
        }
        //转账成功
        return true;
    }



    @Transactional(rollbackFor = Exception.class)
    public boolean crossDbOpCancel(CrossDatabaseBean crossDatabaseBean){
        log.info("crossDbOpCancel执行中");
        Integer logId = crossDatabaseBean.getLogId();
        if(logId != null){
            TransferLog updateModel = new TransferLog();
            updateModel.setUpdateTime(new Date());
            updateModel.setId(crossDatabaseBean.getLogId());
            // 转账失败
            updateModel.setStatus(Byte.parseByte("2"));
            transferLogMapperExt.updateByPrimaryKeySelective(updateModel);
        }
        return true;
    }




}
