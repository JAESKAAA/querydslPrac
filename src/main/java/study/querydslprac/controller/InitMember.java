package study.querydslprac.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import study.querydslprac.entity.Member;
import study.querydslprac.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * 스프링 띄울때 데이터 미리 넣어두고 API로 조회만 테스트 할 수 있게 하기위한 설정임
 */
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    //하기 init메서드 부분을 여기에 두지 않는 이유는 스프링 라이프사이클에 의해 PostConstruct와 트랜잭션을 같이 쓸 수 없기 때문
    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService{
        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB ;
                em.persist(new Member("member"+i, i, selectedTeam));
            }
        }
    }
}
