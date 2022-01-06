package study.querydslprac.entity;

import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
//@Commit // 앞으로 테스트를 위해 커밋은 잠시 주석처리
class MemberTest {
    @Autowired
    EntityManager em;

    @Test
    public void testEntity(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //초기화
        em.flush(); //영속성 컨텍스트에 있는 쿼리들을 DB에 날림
        em.clear(); //영속성 컨텍스트에 있는 모든 것들을 클리어해서 초기화해줌

        //확인
        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        for(Member member : members){
            System.out.println("member => "+member);
            System.out.println("member_team => "+member.getTeam());
        }
    }
}