package com.carl.geek.api;

/**
 * @author carl.che
 */
public interface CrossDbOp {

    /**
     * 跨db转账
     */
    boolean crossDbOp(CrossDatabaseBean crossDatabaseBean);

}
