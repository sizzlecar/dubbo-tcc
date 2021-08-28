package com.carl.geek.service.a.service.test;


import com.carl.geek.api.AccountOperateBean;
import com.carl.geek.service.a.service.AccountOperateImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class AccountOperateImplTest {

    @Autowired
    private AccountOperateImpl accountOperate;


    @Test
    @Rollback(value = false)
    public void operateTest() throws InterruptedException {
        AccountOperateBean operateBean = new AccountOperateBean();
        operateBean.setFromUserId("zhangsan");
        operateBean.setToUserId("lisi");
        operateBean.setAccountType(0);
        operateBean.setMoney(BigDecimal.valueOf(100));

        CountDownLatch countDownLatch = new CountDownLatch(10);

        for (int i = 0 ; i < 5; i++){
            new Thread(() -> {
                try{
                    accountOperate.operate(operateBean);
                }catch (Exception e){
                    log.error("{}->{},{},操作失败", "zhangsan", "lisi", Thread.currentThread().getName(), e);
                }finally {
                    countDownLatch.countDown();
                }
            }).start();
        }


        AccountOperateBean operateBean2 = new AccountOperateBean();
        operateBean2.setFromUserId("lisi");
        operateBean2.setToUserId("wangwu");
        operateBean2.setAccountType(0);
        operateBean2.setMoney(BigDecimal.valueOf(100));
        for (int i = 0 ; i < 5; i++){
            new Thread(() -> {
                try{
                    accountOperate.operate(operateBean2);
                }catch (Exception e){
                    log.error("{}->{},{},操作失败", "lisi", "wangwu", Thread.currentThread().getName());
                }finally {
                    countDownLatch.countDown();
                }
            }).start();
        }

        countDownLatch.await();



    }




}
