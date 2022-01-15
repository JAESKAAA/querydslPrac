package study.querydslprac;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydslprac.dto.MemberDto;
import study.querydslprac.dto.QMemberDto;
import study.querydslprac.dto.UserDto;
import study.querydslprac.entity.Member;
import study.querydslprac.entity.QMember;
import study.querydslprac.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydslprac.entity.QMember.member;
import static study.querydslprac.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    //개별 테스트 전에 데이터 주입을 해주는 어노테이션
    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
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
    }

    @Test
    public void startJPQL(){
        //member1을 찾아라
        String qlString =
                "select m from Member m " +
                "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){
        //테스트 전 gradle 태스크에서 compileQuerydsl을 수행하여 각 엔티티의 Q객체를 생성해줘야함

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        results.getTotal();
//        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 이름 올림 차순
     *
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        //유저네임이 오름차순에 널값을 마지막으로 받도록 했으니 member5 -> member6 -> null순으로 예상함
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //앞에 몇개를 스킵할건지 (0부터시작_ 즉, 여기서는 2번째 값부터 2개를 꺼내오라는 의미)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }
    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //앞에 몇개를 스킵할건지 (0부터시작_ 즉, 여기서는 2번째 값부터 2개를 꺼내오라는 의미)
                .limit(2)
                .fetchResults();

        
        //쿼리결과의 토탈 갯수가 4개인지? -> beforeEach에서 값을 4개만 넣어줬으니 4가 나와야함
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        //쿼리결과에서 결과값의 사이즈가 2인지?
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구하라
     *
     * select
     */

    @Test
    public void group(){
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원 찾기
     */
    @Test
    public void join1(){
        List<Member> fetch = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(fetch)
                .extracting("username")
                .containsExactly("member1","member2");
    }

    //연관관계 없는 필드값으로 조인

    /**
     * 세타조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //member와 team테이블을 다 가져와서 조인시켜버림
                .where(member.username.eq(team.name))
                .fetch();

        result.forEach(System.out::println);

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

    /**
     * ex. 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     *
     * JPQL = select m,t from Member m left join m.team t on t.name = 'teamA';
     *
     */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        result.forEach(System.out::println);
    }

    /**
     * 연관 관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        //기존 세타조인은 left조인이 안되서 하기와 같이 on 활용하여 조인함
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                //원래같은경우 leftJoin(member.team, team)으로 조인함 이러면 join on절에 조인 대상이 id값으로 매칭되게 됨
                //하지만 하기와 같이 작성하면 id값으로 매칭되지 않고 team.name으로만 매칭이 되게됨
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        result.forEach(System.out::println);
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println(member1);

        //member1에 team이 로딩됐는지 안됐는지 알려주는 로직
        //현재 Member에 Team은 LAZY 로딩 상태이므로 false가 나와야 정상임
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());

        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();

        Member member1 = queryFactory
                .selectFrom(member)
                //하기와 같이 fetchJoin()을 넣어주면 member를 조회할때 연관된 테이블정보를 한쿼리로 가져옴
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println(member1);

        //fetch조인이 적용되었기 때문에 true가 나와야 정상임
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());

        assertThat(loaded).as("패치 조인 적용").isTrue();
    }


    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {

        //서브쿼리시 alias가 겹치기 때문에 Q멤버를 하나 더 정의해주기
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq( //where절에서 서브쿼리 삽입
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age").containsExactly(40);
    }
    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe(){

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        result.forEach(System.out::println);
        assertThat(result).extracting("age").containsExactly(30,40);
    }

    /**
     * 서브쿼리시 in 사용 예제
     */

    @Test
    public void subQueryIn(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20,30,40);
    }

    @Test
    public void selectSubquery(){
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        //JPAExpressions static import처리
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21살 ~ 30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        result.forEach(System.out::println );
    }

    @Test
    public void concat(){

        //[username_age]의 형식으로 데이터 출력하고 싶음
        //concat은 문자만 가능한데, 나이는 문자타입이 아닌 문제가 있음
        //따라서 stringValue()메소드를 입력시켜 문자로 변환해서 적용해야함
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        fetch.forEach(System.out::println);
    }

    @Test
    public void simpleProjection(){
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        System.out.println(fetch);
    }

    @Test
    public void tupleProjection(){
        List<Tuple> fetch = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        //fetch.forEach(System.out::println);
        for(Tuple tuple : fetch){
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username => "+username);
            System.out.println("age => "+age);
        }
    }

    @Test
    public void findDtoJPQL(){
        //MemberDto타입을 받길 원할때 new operation방식 사용 예시
        List<MemberDto> resultList = em.createQuery("select new study.querydslprac.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        resultList.forEach(System.out::println);
    }

    //프로젝션 DTO 결과 조회 - setter 접근 방식
    //주의! dto에 기본생성자가 없으면 newinstance 관련 에러가 발생함
    //생성자로 호출하고 setter로 넣어줘야하는데, 생성자가 없기 때문에 예외가 발생하는 것!
    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    //프로젝션 DTO 결과 조회 - 필드값 접근 방식
    @Test
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    //프로젝션 DTO 결과 조회 - 생성자 접근 방식
    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    //프로젝션 DTO 결과 조회 - 변수명이 다를때
    @Test
    public void findUserDto(){
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        //as로 별칭 지정을 안해주면, 매칭되는 field값이 없어 name부분에 null이 들어가게 됨
        result.forEach(System.out::println  );
    }

    //프로젝션 DTO 결과 조회 - 서브쿼리에 alias 적용
    @Test
    public void findUserDtoSub(){
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        result.forEach(System.out::println  );
    }

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    //동적쿼리 - BooleanBuilder
    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    //param의 값이 null이냐 아니냐에 따라 결과가 동적으로 바뀌어야하는 것을 검증하기 위함
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    //동적쿼리 - where 다중 파라미터
    @Test
    public void dynamicQuery_WhereParam(){

        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);

    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                //.where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    //where에서 and조건으로 연결되므로, null이 올 경우 그냥 무시됨
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate() {
        queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        //벌크연산 진행 하면 하기와 같이 플러시와 클리어를 해줘야 영속성 컨텍스트가 클리어되어 다시 DB값을 받아오게됨
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        //비회원으로 바뀌지 않은 데이터가 출력됨 (DB에는 비회원으로 저장되어있음)
        result.forEach(s -> System.out.println("member = "+s));
    }

    @Test
    public void bulkAdd(){

        //UPDATE member SET age = age+?
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) //뺄셈은 없어서 그냥 -1을 더해주는 방향으로 생각하기
                .execute();
    }

    @Test
    public void bulkDelete(){

        //DELETE FROM member WHERE age > 18
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction(){

        //member에서 member라는 단어를 M으로 바꿔서 조회회
       List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0},{1},{2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();
       result.forEach(System.out::println);
    }

    @Test
    public void sqlFunciton2(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .fetch();

        //상기 쿼리와 똑같은 기능
        List<String> result2 = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();



        result.forEach(System.out::println);
        result2.forEach(System.out::println);

    }
}
