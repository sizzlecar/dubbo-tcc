package com.carl.geek.service.a.service;

import com.carl.geek.api.AccountOperateBean;
import com.carl.geek.api.CrossDbLocalOpBean;
import com.carl.geek.api.ServiceAAccountOperate;
import com.carl.geek.service.a.dao.UserAccountFreezeMapperExt;
import com.carl.geek.service.a.dao.UserAccountMapperExt;
import com.carl.geek.service.a.model.UserAccount;
import com.carl.geek.service.a.model.UserAccountFreeze;
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
@DubboService(version = "1.0.0", group = "a")
public class AccountOperateImpl implements ServiceAAccountOperate {

    private final UserAccountMapperExt userAccountMapperExt;
    private final UserAccountFreezeMapperExt userAccountFreezeMapperExt;


    private final Cache<String, Object> accountLockCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .build();

    private final Object fromCacheLock = new Object();
    private final Object toCacheLock = new Object();


    /**
     * 业务逻辑：用户A给用户B转账，1. 检查用户A的余额是否够 2. 将用户A的余额扣掉转账金额 3. 将用户B的余额加上转账的金额
     * 如果不做任何处理会出现线程安全问题。比如：
     * 用户张三余额200，李四余额200，线程A，B同时发起请求 张三给李四转账200，第一步他们查到的张三的余额都是200，继续执行，最后
     * 张三的余额被改为0，李四的余额可能是200也可能是400
     * 解决办法：
     * 1. 将operate方法改为同步方法。这种方法还会有问题，比如：
     * 用户张三余额200，李四余额200，线程A，B同时发起请求 张三给李四转账200，假如A线程先拿到锁，查询张三的余额200，将张三的余额改为100，
     * 然后将李四的余额改为300，释放锁，提交事物。注意这里的释放锁和提交事物没有严格的先后关系，这就导致，线程A释放锁之后，线程B拿到锁之后
     * 查询A的余额很有可能是线程A提交事物的之前的结果。
     * 2. 显示的使用Mysql的排他锁，查询张三的余额的SQL使用select... for update,这样在当前事物提交之前其他事物都不能对这条数据进行查询
     * 和修改。这种方法带来的问题并发的压力会直接暴露给数据库
     * 3. 将operate方法改为同步方法同时使用for update。这种方式其实还有1个问题: AccountOperateImpl默认是单例的所有转账操作都变成串行了，
     * 这样效率低下，比如人张三给李四转账和王五给赵六转账两个操作是可以并行的，转账的操作需要加锁的资源其实只有转账双方的账户
     * 4. 继续优化，将加上operate方法上的锁去掉，建立一个锁的缓存，根据用户id来获取对应的锁，每次转账的时候只需要锁转账双方的账号就可以，同时
     * 使用for update 确保整体的线程安全，不过这样还会带来一个问题， 有可能造成死锁，比如张三给李四转账，同时李四又给张三 转账，A线程拿到了张三
     * 的锁，B线程拿到了李四的锁，两个线程都在等待对方的锁就造成了死锁，解决这个问题可以采用有顺序加锁的办法，比如加锁的时候按照账号的id排序加锁，
     * 这样不管是张三给李四转账还是李四给张三转账首先抢占的锁都是同一把锁，就不会出现死锁的问题
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean operate(AccountOperateBean accountOperateBean) {
        //用户应该从认证体系中获取，此处简化作为参数
        String userId = accountOperateBean.getFromUserId();
        Integer accountType = accountOperateBean.getAccountType();
        BigDecimal money = accountOperateBean.getMoney();
        String toUserId = accountOperateBean.getToUserId();
        if (money == null || BigDecimal.ZERO.compareTo(money) >= 0) {
            throw new RuntimeException("money必须大于等于0");
        }
        Object fromLock = accountLockCache.getIfPresent(userId);
        Object toLock = accountLockCache.getIfPresent(toUserId);
        if (fromLock == null) {
            synchronized (fromCacheLock) {
                if (accountLockCache.getIfPresent(userId) == null) {
                    Object fromLockObj = new Object();
                    accountLockCache.put(userId, fromLockObj);
                    fromLock = fromLockObj;
                }
                fromLock = fromLock == null ? accountLockCache.getIfPresent(userId) : fromLock;
            }
        }
        if (toLock == null) {
            synchronized (toCacheLock) {
                if (accountLockCache.getIfPresent(toUserId) == null) {
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
        synchronized (left) {
            //查询账号是否存在
            UserAccount paraModel = new UserAccount();
            paraModel.setType(accountType);
            paraModel.setUserId(leftUserId);
            UserAccount userAccount = userAccountMapperExt.selectOneForUpdate(paraModel);
            if (userAccount == null) {
                accountLockCache.invalidate(leftUserId);
                throw new RuntimeException("查询数据异常");
            }
            boolean fromUserFlag = leftUserId.equals(userId);
            UserAccount updateModel = new UserAccount();
            BigDecimal leftUpdateMoney = null;
            if (fromUserFlag) {
                BigDecimal balance = userAccount.getBalance();
                leftUpdateMoney = balance.subtract(money);
                if (BigDecimal.ZERO.compareTo(leftUpdateMoney) > 0) {
                    throw new RuntimeException("Insufficient balance!");
                }
            } else {
                BigDecimal balance = userAccount.getBalance();
                leftUpdateMoney = balance.add(money);
            }
            updateModel.setBalance(leftUpdateMoney);
            updateModel.setId(userAccount.getId());
            updateModel.setUpdateTime(new Date());
            userAccountMapperExt.updateByPrimaryKeySelective(updateModel);

            synchronized (right) {
                paraModel.setUserId(rightUserId);
                UserAccount rightUser = userAccountMapperExt.selectOneForUpdate(paraModel);
                BigDecimal rightUpdateMoney;
                if (fromUserFlag) {
                    BigDecimal toUserBalance = rightUser.getBalance();
                    rightUpdateMoney = toUserBalance.add(money);
                } else {
                    BigDecimal balance = rightUser.getBalance();
                    rightUpdateMoney = balance.subtract(money);
                    if (BigDecimal.ZERO.compareTo(rightUpdateMoney) > 0) {
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
        log.info("service-a try is running");
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
