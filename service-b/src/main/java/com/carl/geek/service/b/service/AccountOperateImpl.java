package com.carl.geek.service.b.service;

import com.carl.geek.api.AccountOperateBean;
import com.carl.geek.api.CrossDbLocalOpBean;
import com.carl.geek.api.ServiceBAccountOperate;
import com.carl.geek.service.b.dao.UserAccountFreezeMapperExt;
import com.carl.geek.service.b.dao.UserAccountMapperExt;
import com.carl.geek.service.b.model.UserAccount;
import com.carl.geek.service.b.model.UserAccountFreeze;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.hmily.annotation.HmilyTCC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author carl.che
 */
@Service
@Slf4j
@RequiredArgsConstructor
@DubboService(version = "1.0.0", group = "b")
public class AccountOperateImpl implements ServiceBAccountOperate {

    private final UserAccountMapperExt userAccountMapperExt;
    private final UserAccountFreezeMapperExt userAccountFreezeMapperExt;

    private final Cache<String, Object> accountLockCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .build();

    private final Object fromCacheLock = new Object();
    private final Object toCacheLock = new Object();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean operate(AccountOperateBean accountOperateBean) {
        //用户应该从认证体系中获取，此处简化作为参数
        String userId = accountOperateBean.getFromUserId();
        Integer accountType = accountOperateBean.getAccountType();
        BigDecimal money = accountOperateBean.getMoney();
        String toUserId = accountOperateBean.getToUserId();
        if(money == null || BigDecimal.ZERO.compareTo(money) >= 0){
            throw new RuntimeException("money必须大于等于0");
        }
        Object fromLock = accountLockCache.getIfPresent(userId);
        Object toLock = accountLockCache.getIfPresent(toUserId);
        if(fromLock == null){
            synchronized (fromCacheLock){
                if(accountLockCache.getIfPresent(userId) == null){
                    Object fromLockObj = new Object();
                    accountLockCache.put(userId, fromLockObj);
                    fromLock = fromLockObj;
                }
                fromLock = fromLock == null ? accountLockCache.getIfPresent(userId) : fromLock;
            }
        }
        if(toLock == null){
            synchronized (toCacheLock){
                if(accountLockCache.getIfPresent(toUserId) == null){
                    Object toLockObj = new Object();
                    accountLockCache.put(toUserId, toLockObj);
                    toLock = toLockObj;
                }
                toLock = toLock == null ? accountLockCache.getIfPresent(toUserId) : toLock;
            }
        }
        List<String> lockList = Stream.of(userId, toUserId).sorted().collect(Collectors.toList());
        String leftUserId = lockList.get(0);
        String rightUserId = lockList.get(1);
        Object left = leftUserId.equals(userId) ? fromLock : toLock;
        Object right = rightUserId.equals(userId) ? fromLock : toLock;
        synchronized (left){
            //查询账号是否存在
            UserAccount paraModel = new UserAccount();
            paraModel.setType(accountType);
            paraModel.setUserId(leftUserId);
            UserAccount userAccount = userAccountMapperExt.selectOneForUpdate(paraModel);
            if(userAccount == null){
                accountLockCache.invalidate(leftUserId);
                throw new RuntimeException("查询数据异常");
            }
            boolean fromUserFlag = leftUserId.equals(userId);
            UserAccount updateModel = new UserAccount();
            BigDecimal leftUpdateMoney = null;
            if(fromUserFlag){
                BigDecimal balance = userAccount.getBalance();
                leftUpdateMoney = balance.subtract(money);
                if(BigDecimal.ZERO.compareTo(leftUpdateMoney) > 0){
                    throw new RuntimeException("Insufficient balance!");
                }
            }else {
                BigDecimal balance = userAccount.getBalance();
                leftUpdateMoney = balance.add(money);
            }
            updateModel.setBalance(leftUpdateMoney);
            updateModel.setId(userAccount.getId());
            updateModel.setUpdateTime(new Date());
            userAccountMapperExt.updateByPrimaryKeySelective(updateModel);

            synchronized (right){
                paraModel.setUserId(rightUserId);
                UserAccount rightUser = userAccountMapperExt.selectOneForUpdate(paraModel);
                BigDecimal rightUpdateMoney;
                if(fromUserFlag){
                    BigDecimal toUserBalance = rightUser.getBalance();
                    rightUpdateMoney = toUserBalance.add(money);
                }else {
                    BigDecimal balance = rightUser.getBalance();
                    rightUpdateMoney = balance.subtract(money);
                    if(BigDecimal.ZERO.compareTo(rightUpdateMoney) > 0){
                        throw new RuntimeException("Insufficient balance!");
                    }
                }
                updateModel.setId(rightUser.getId());
                updateModel.setBalance(rightUpdateMoney);
                updateModel.setUpdateTime(new Date());
                userAccountMapperExt.updateByPrimaryKeySelective(updateModel);

                log.info("线程：{},{}向{}转账{}元,转账前，{}余额：{},{}余额：{},转账后,{}余额：{},{}余额：{}", Thread.currentThread().getName(),
                        userId, toUserId, money.toPlainString(), userId, fromUserFlag ? userAccount.getBalance().toPlainString() : rightUser.getBalance().toPlainString(),
                        toUserId, fromUserFlag ? rightUser.getBalance().toPlainString() : userAccount.getBalance().toPlainString(),
                        userId, fromUserFlag ? leftUpdateMoney : rightUpdateMoney,
                        toUserId, fromUserFlag ? rightUpdateMoney : leftUpdateMoney);
            }


        }
        return true;
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    @HmilyTCC(confirmMethod = "crossDbLocalOpConfirm", cancelMethod = "crossDbLocalOpCancel")
    public boolean crossDbLocalOp(CrossDbLocalOpBean accountOperateBean) {
        log.info("service-b try is running");
        Integer accountType = accountOperateBean.getAccountType();
        String localUserId = accountOperateBean.getTargetUserId();
        BigDecimal amount = accountOperateBean.getAmount();
        UserAccount paraAccount = new UserAccount();
        paraAccount.setType(accountType);
        paraAccount.setUserId(localUserId);
        Object accountLock = getUserAccountLock(localUserId);
        synchronized (accountLock) {
            UserAccount userAccount = userAccountMapperExt.selectOneForUpdate(paraAccount);
            if (userAccount == null) {
                throw new RuntimeException("查询数据异常");
            }
            BigDecimal balance = userAccount.getBalance();
            BigDecimal updateMoney = balance.add(amount);
            if (BigDecimal.ZERO.compareTo(updateMoney) > 0) {
                throw new RuntimeException("余额不足");
            }
            if (BigDecimal.ZERO.compareTo(amount) > 0) {
                //扣钱，需要将扣的钱冻结起来
                UserAccount updateModel = new UserAccount();
                updateModel.setId(userAccount.getId());
                updateModel.setUpdateTime(new Date());
                updateModel.setBalance(updateMoney);
                userAccountMapperExt.updateByPrimaryKeySelective(updateModel);
                UserAccountFreeze insertModel = new UserAccountFreeze();
                insertModel.setAmount(amount);
                insertModel.setCreateTime(new Date());
                insertModel.setUpdateTime(new Date());
                insertModel.setUserAccountId(userAccount.getId());
                userAccountFreezeMapperExt.insert(insertModel);
                accountOperateBean.setUserAccountFreezeId(insertModel.getId());
            }
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void crossDbLocalOpConfirm(CrossDbLocalOpBean crossDatabaseReq) {
        log.info("crossDbLocalOpConfirm执行中");
        String targetUserId = crossDatabaseReq.getTargetUserId();
        UserAccount paraAccount = new UserAccount();
        paraAccount.setType(crossDatabaseReq.getAccountType());
        paraAccount.setUserId(targetUserId);
        Object targetLock = getUserAccountLock(targetUserId);
        synchronized (targetLock) {
            UserAccount userAccount = userAccountMapperExt.selectOneForUpdate(paraAccount);
            if (userAccount == null) {
                throw new RuntimeException("查询数据异常");
            }
            BigDecimal balance = userAccount.getBalance();
            BigDecimal amount = crossDatabaseReq.getAmount();
            BigDecimal updateMoney = balance.add(amount);
            if (BigDecimal.ZERO.compareTo(updateMoney) > 0) {
                throw new RuntimeException("余额不足");
            }
            if (BigDecimal.ZERO.compareTo(amount) > 0) {
                //扣钱
                userAccountFreezeMapperExt.deleteByPrimaryKey(crossDatabaseReq.getUserAccountFreezeId());
            } else {
                UserAccount updateModel = new UserAccount();
                updateModel.setId(userAccount.getId());
                updateModel.setUpdateTime(new Date());
                updateModel.setBalance(updateMoney);
                userAccountMapperExt.updateByPrimaryKeySelective(updateModel);
            }
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void crossDbLocalOpCancel(CrossDbLocalOpBean crossDatabaseReq) {
        log.info("crossDbLocalOpCancel执行中");
        //分布式事物出现异常，删除冻结，同时将钱复原
        Integer userAccountFreezeId = crossDatabaseReq.getUserAccountFreezeId();
        String localUserId = crossDatabaseReq.getTargetUserId();
        Object targetLock = getUserAccountLock(localUserId);
        BigDecimal amount = crossDatabaseReq.getAmount();
        Integer accountType = crossDatabaseReq.getAccountType();
        UserAccount paraAccount = new UserAccount();
        paraAccount.setType(accountType);
        paraAccount.setUserId(localUserId);
        synchronized (targetLock) {
            UserAccount userAccount = userAccountMapperExt.selectOneForUpdate(paraAccount);
            if (userAccount == null) {
                throw new RuntimeException("查询数据异常");
            }
            if (userAccountFreezeId != null) {
                //扣钱，将冻结的钱返回至原账号
                UserAccount updateModel = new UserAccount();
                updateModel.setId(userAccount.getId());
                updateModel.setUpdateTime(new Date());
                updateModel.setBalance(amount.negate().add(userAccount.getBalance()));
                userAccountMapperExt.updateByPrimaryKeySelective(updateModel);
                userAccountFreezeMapperExt.deleteByPrimaryKey(userAccountFreezeId);
            } else {
                //加钱，需要将加的钱删除
                UserAccount updateModel = new UserAccount();
                updateModel.setId(userAccount.getId());
                updateModel.setUpdateTime(new Date());
                updateModel.setBalance(amount.negate().add(userAccount.getBalance()));
                userAccountMapperExt.updateByPrimaryKeySelective(updateModel);
            }
        }
    }

    /**
     * 获取用户账号对应的锁
     *
     * @param userId 用户id
     * @return 锁
     */
    private Object getUserAccountLock(String userId) {
        Object targetLock = accountLockCache.getIfPresent(userId);
        if (targetLock == null) {
            synchronized (fromCacheLock) {
                if (accountLockCache.getIfPresent(userId) == null) {
                    Object fromLockObj = new Object();
                    accountLockCache.put(userId, fromLockObj);
                    targetLock = fromLockObj;
                }
                targetLock = targetLock == null ? accountLockCache.getIfPresent(userId) : targetLock;
            }
        }
        return targetLock;
    }
}
