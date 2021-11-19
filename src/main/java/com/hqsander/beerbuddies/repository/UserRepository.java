package com.hqsander.beerbuddies.repository;

import com.hqsander.beerbuddies.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    UserEntity findUserByUsername(String username);

    UserEntity findUserByEmail(String email);
}
