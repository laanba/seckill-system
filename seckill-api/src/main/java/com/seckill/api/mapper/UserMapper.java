package com.seckill.api.mapper;

import com.seckill.api.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

/**
 * Seckill User Mapper
 */
@org.apache.ibatis.annotations.Mapper
public interface UserMapper extends Mapper<User> {

    /**
     * Find user by username
     */
    @Select("SELECT * FROM seckill_user WHERE username = #{username} LIMIT 1")
    User findByUsername(@Param("username") String username);

    /**
     * Find user by ID
     */
    @Select("SELECT * FROM seckill_user WHERE id = #{id} LIMIT 1")
    User findById(@Param("id") Long id);
}
