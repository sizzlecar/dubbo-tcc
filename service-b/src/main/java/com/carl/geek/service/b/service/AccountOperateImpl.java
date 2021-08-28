package com.carl.geek.service.b.service;

import com.carl.geek.api.AccountOperate;
import com.carl.geek.api.AccountOperateBean;
import com.carl.geek.service.b.dao.UserAccountMapperExt;
import com.carl.geek.service.b.model.UserAccount;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class AccountOperateImpl implements AccountOperate {

    private final UserAccountMapperExt userAccountMapperExt;

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
}
