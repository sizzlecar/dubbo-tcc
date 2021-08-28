package com.carl.geek.service.a.dao;

import com.carl.geek.service.a.model.UserAccount;
import org.springframework.stereotype.Repository;

/**
 * @author Administrator
 */
@Repository
public interface UserAccountMapperExt extends UserAccountMapper{

    UserAccount selectOneForUpdate(UserAccount model);
}