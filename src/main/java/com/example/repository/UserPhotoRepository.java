package com.example.photo;

import com.example.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPhotoRepository extends JpaRepository<UserPhoto, Long> {
	boolean existsByUserAndFileName(User user, String fileName);
	long countByUser(User user);
	Page<UserPhoto> findByUserId(Long userId, Pageable pageable);
}
