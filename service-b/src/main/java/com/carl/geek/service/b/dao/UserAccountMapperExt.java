package com.carl.geek.service.b.dao;

import com.carl.geek.service.b.model.UserAccount;
import org.springframework.stereotype.Repository;

/**
 * @author Administrator
 */
@Repository
public interface UserAccountMapperExt extends UserAccountMapper{

    UserAccount selectOneForUpdate(UserAccount model);
}