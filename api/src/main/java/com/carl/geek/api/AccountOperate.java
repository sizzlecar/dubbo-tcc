package com.carl.geek.api;

/**
 * @author carl.che
 */
public interface AccountOperate {

    /**
     * 账号操作接口(账号余额的增加，减少)
     * @param accountOperateBean 参数model
     * @return true 成功，false 失败
     */
    boolean operate(AccountOperateBean accountOperateBean);


    interface Constants{

        /**
         * 账户类型
         */
        interface AccountType{
            int CNY = 0;
            int USD = 1;
        }

        /**
         * 增减
         */
        interface OpType{
            int ADD = 0;
            int DELETE = 1;
        }
    }


}
