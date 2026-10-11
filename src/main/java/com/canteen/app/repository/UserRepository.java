package com.canteen.app.repository;

import com.canteen.app.model.Role;
import com.canteen.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByRollNumber(String rollNumber);
    List<User> findByRole(Role role);
}
