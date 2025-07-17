package com.onlineclass.backend.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.onlineclass.backend.entity.Users;
import com.onlineclass.backend.mapper.UsersMapper;
import com.onlineclass.backend.service.UsersService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author yikonsh
 * @since 2025-07-17
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users>  implements UsersService{

}
