package com.englishplatform.repository;

import com.englishplatform.entity.Role;
import com.englishplatform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findAllWithFilters(@Param("role") Role role,
                                   @Param("search") String search,
                                   Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND u.role = 'STUDENT'")
    List<User> searchStudents(@Param("search") String search);
}
