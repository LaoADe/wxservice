package com.laoade.wxservice.repository;


import com.laoade.wxservice.domain.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Integer> {
    Author findByOpenId(String openId);
    Author findBySkey(String skey);
}
