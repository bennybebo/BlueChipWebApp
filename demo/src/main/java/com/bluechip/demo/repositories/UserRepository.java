package com.bluechip.demo.repositories;

import com.bluechip.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Custom query method to find a user by username
    User findByUsername(String username);
}
