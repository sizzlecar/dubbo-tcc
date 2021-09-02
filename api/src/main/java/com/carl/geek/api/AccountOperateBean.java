package com.carl.geek.api;

import lombok.Data;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author carl.che
 */
@Data
public class AccountOperateBean implements Serializable {

    /**
     * 发起用户
     */
    @NotBlank(message = "fromUserId不能为空")
    private String fromUserId;

    /**
     * 接受用户
     */
    @NotBlank(message = "toUserId不能为空")
    private String toUserId;


    /**
     * 账号类型
     */
    @NotNull(message = "账号类型不能为空")
    private Integer accountType;

    /**
     * 金额
     */
    @Digits(integer = 10, fraction = 2, message = "转账金额整数部分不能超过{integer}位,小数部分不能超过{fraction}位")
    private BigDecimal money;




}
