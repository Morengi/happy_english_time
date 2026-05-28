package com.englishplatform.repository;

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

    // Pass role as String to avoid PostgreSQL lower(bytea) error on null enum params
    @Query("SELECT u FROM User u WHERE " +
           "(:roleStr IS NULL OR CAST(u.role AS string) = :roleStr) AND " +
           "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findAllWithFilters(@Param("roleStr") String roleStr,
                                   @Param("search") String search,
                                   Pageable pageable);

    // Legacy — kept for backward compatibility (used by group member search)
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND u.role = 'STUDENT'")
    List<User> searchStudents(@Param("search") String search);

    // Search across ALL roles — used for messaging so any user can find any other user
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<User> searchAllUsers(@Param("search") String search);
}
