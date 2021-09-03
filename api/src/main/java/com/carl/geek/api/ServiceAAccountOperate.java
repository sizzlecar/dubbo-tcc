package com.carl.geek.api;

import org.dromara.hmily.annotation.Hmily;

/**
 * @author carl.che
 */
public interface ServiceAAccountOperate {

    /**
     * 账号操作接口(账号余额的增加，减少)
     * @param accountOperateBean 参数model
     * @return true 成功，false 失败
     */
    boolean operate(AccountOperateBean accountOperateBean);

    /**
     * 跨库交易
     */
    @Hmily
    boolean crossDbLocalOp(CrossDbLocalOpBean crossDbLocalOpBean);



}
