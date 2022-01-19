package study.querydslprac.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import study.querydslprac.entity.Member;
import study.querydslprac.entity.Team;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원정보가 세이브 된다.")
    public void saveTest(){

        Member member = Member.builder()
                .username("테스트1번")
                .age(10)
                .team(new Team("teamA"))
                .build();

        Member result = memberRepository.save(member);

        assertThat(result.getUsername()).isEqualTo("테스트1번");
    }

}