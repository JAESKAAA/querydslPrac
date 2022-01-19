package study.querydslprac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydslprac.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
