package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3= new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() {
        // given - member1을 찾아라.
        String qlString =
                "select m from Member m " +
                "where m.username = :username";

        // when
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {
        // given
//        QMember m = new QMember("m"); // 1. 별칭 직접 사용 방식
//        QMember m = QMember.member; // 2. 기본 인스턴스 사용

        // when
        // 결과적으로 아래 과정을 통해 jpql이 만들어지는 거라고 볼 수 있음. 다만 실질적으로 어떤 jpql이 만들어졌는지 확인하고 싶다면?
        // -> .yml 파일에 가서 'hibernate:' 아래에 'use_sql_comments: true' 라는 옵션을 추가해주면 됨!
        Member findMember = queryFactory
                .select(member) // 3. static import 사용
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    /**
     * 1. 기본 검색 쿼리
     *  - 검색조건은 .and(),.or()를 메서드 체인으로 연결할 수 있다.
     *  - 참고: select , from 을 selectFrom 으로 합칠 수 있음
     *
     * 2. JPQL이 제공하는 모든 검색 조건 제공
     * member.username.eq("member1") // username = 'member1'
     * member.username.ne("member1") //username != 'member1'
     * member.username.eq("member1").not() // username != 'member1'
     *
     * member.username.isNotNull() //이름이 is not null
     * member.age.in(10, 20) // age in (10,20)
     * member.age.notIn(10, 20) // age not in (10, 20)
     * member.age.between(10,30) //between 10, 30
     *
     * member.age.goe(30) // age >= 30
     * member.age.gt(30) // age > 30
     * member.age.loe(30) // age <= 30
     * member.age.lt(30) // age < 30
     *
     * member.username.like("member%") //like 검색
     * member.username.contains("member") // like ‘%member%’ 검색
     * member.username.startsWith("member") //like ‘member%’ 검색 ...
     */
    @Test
    void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // 3. AND 조건을 .and를 안쓰고 파라미터로 처리할 수도 있음
    // where() 에 파라미터로 검색조건을 추가하면 AND 조건이 추가되는 것과 같음
    // 이경우 null 값은 무시. 메서드 추출을 활용해서 동적쿼리를 깔끔하게 만들 수 있음
    @Test
    void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     * 4. 결과 조회
     * fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
     * fetchOne() : 단 건 조회
     *  - 결과가 없으면 : null
     *  - 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
     * fetchFirst() : limit(1).fetchOne()
     * fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
     * fetchCount() : count 쿼리로 변경해서 count 수만 조회
     */
    @Test
    void resultFetch() {
        // 1
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 2
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        // 3
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // 4
        QueryResults<Member> fetchResults = queryFactory
                .selectFrom(member)
                .fetchResults();
        // 아래 로직들을 통해 fetchResults가 토탈카운트와 데이터를 같이 가져온다는 걸 알 수 있음
        fetchResults.getTotal();
        List<Member> content = fetchResults.getResults();

        // 5
        long fetchCount = queryFactory
                .selectFrom(member)
                .fetchCount();
    }


}

