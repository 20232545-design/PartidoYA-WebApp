package com.partidoya.platform.users.repositories;

import com.partidoya.platform.users.repositories.UserPersistenceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPersistenceRepository extends JpaRepository<UserPersistenceEntity, Long> {
    Optional<UserPersistenceEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
            select u from UserPersistenceEntity u
            where (:search is null or lower(u.fullName) like lower(concat('%', :search, '%'))
                or lower(u.email) like lower(concat('%', :search, '%')))
              and (:role is null or u.role = :role)
            order by u.fullName asc
            """)
    List<UserPersistenceEntity> search(@Param("search") String search, @Param("role") String role, Pageable pageable);

    @Query("""
            select count(u) from UserPersistenceEntity u
            where (:search is null or lower(u.fullName) like lower(concat('%', :search, '%'))
                or lower(u.email) like lower(concat('%', :search, '%')))
              and (:role is null or u.role = :role)
            """)
    long countSearch(@Param("search") String search, @Param("role") String role);
}
