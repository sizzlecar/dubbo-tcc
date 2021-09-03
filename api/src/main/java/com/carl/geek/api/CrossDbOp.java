package com.carl.geek.api;

import org.dromara.hmily.annotation.Hmily;

/**
 * @author carl.che
 */
public interface CrossDbOp {

    /**
     * 跨db转账
     */
    @Hmily
    boolean crossDbOp(CrossDatabaseBean crossDatabaseBean);

}
