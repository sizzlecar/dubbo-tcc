package com.carl.geek.api;

import lombok.Data;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 跨库交易参数model
 * @author carl.che
 */
@Data
public class CrossDatabaseBean {

    /**
     * 本地用户
     */
    @NotBlank(message = "fromUserId不能为空")
    private String fromUserId;

    /**
     * 远程用户
     */
    @NotBlank(message = "toUserId不能为空")
    private String toUserId;

    /**
     * 账号类型
     */
    @NotNull(message = "账号类型不能为空")
    private Integer accountType;

    /**
     * 转账金额
     */
    @NotNull(message = "转账金额不能为空")
    @Digits(integer = 10, fraction = 2, message = "转账金额整数部分不能超过{integer}位,小数部分不能超过{fraction}位")
    private BigDecimal amount;

    /**
     * 冻结表id
     */
    private Integer userAccountFreezeId;

    private Integer logId;
}
