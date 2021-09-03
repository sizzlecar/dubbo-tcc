package com.carl.geek.api;

import lombok.Data;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 跨库本地操作
 * @author carl.che
 */
@Data
public class CrossDbLocalOpBean {

    /**
     * 发起用户
     */
    @NotBlank(message = "targetUserId不能为空")
    private String targetUserId;

    /**
     * 账号类型
     */
    @NotNull(message = "账号类型不能为空")
    private Integer accountType;

    /**
     * 金额
     */
    @Digits(integer = 10, fraction = 2, message = "转账金额整数部分不能超过{integer}位,小数部分不能超过{fraction}位")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    /**
     * 冻结表id
     */
    private Integer userAccountFreezeId;
}
