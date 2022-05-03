package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

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
        Member member3 = new Member("member3", 30, teamB);
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
     * 1-1. 기본 검색 쿼리
     * - 검색조건은 .and(),.or()를 메서드 체인으로 연결할 수 있다.
     * - 참고: select , from 을 selectFrom 으로 합칠 수 있음
     * <p>
     * 1-2. JPQL이 제공하는 모든 검색 조건 제공
     * member.username.eq("member1") // username = 'member1'
     * member.username.ne("member1") //username != 'member1'
     * member.username.eq("member1").not() // username != 'member1'
     * <p>
     * member.username.isNotNull() //이름이 is not null
     * member.age.in(10, 20) // age in (10,20)
     * member.age.notIn(10, 20) // age not in (10, 20)
     * member.age.between(10,30) //between 10, 30
     * <p>
     * member.age.goe(30) // age >= 30
     * member.age.gt(30) // age > 30
     * member.age.loe(30) // age <= 30
     * member.age.lt(30) // age < 30
     * <p>
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

    /**
     * 1-3. AND 조건을 .and를 안쓰고 파라미터로 처리할 수도 있음
     * where() 에 파라미터로 검색조건을 추가하면 AND 조건이 추가되는 것과 같음
     * 이 경우 null 값은 무시. 메서드 추출을 활용해서 동적쿼리를 깔끔하게 만들 수 있음
     */
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
     * 2. 결과 조회
     * fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
     * fetchOne() : 단 건 조회
     * - 결과가 없으면 : null
     * - 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
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


    /**
     * 3. 정렬
     *
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }


    /**
     * 4-1. 페이징 1
     */
    @Test
    void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 0부터 시작이라 1이면 하나를 스킵한다는 것
                .limit(2) // 이건 몇개가져올지
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    /**
     * 4-2. 페이징 2 (조건 쿼리 + 카운트 쿼리)
     * 그냥 쿼리랑 함께 카운트 쿼리도 같이 날라가서 페이징하기 편함
     * 단 실무에서는 이걸 안쓰고 따로 카운트 쿼리를 날려야하는 때도 있음.
     * 가령 where가 들어가면 count에도 다 붙어서 쿼리가 날라가 효율적이지 못하기 때문(count쿼리는 where가 필요 없으니까)
     */
    @Test
    void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 0부터 시작이라 1이면 하나를 스킵한다는 것
                .limit(2) // 이건 몇개가져올지
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4); //
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }


    /**
     * 5-1. 집합 함수
     */
    @Test
    void aggregation() {
        // 튜플을 써야할 때는 단일 타입으로 조회하는 게 아닌, 데이터 타입이 여러개 들어올 때 사용
        // 근데 사실 실무에서는 튜플보다는 dto를 많이 씀
        List<Tuple> result = queryFactory
                .select(member.count(), member.age.sum(), member.age.avg(), member.age.max(), member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 5-2. 집합 group 혹은 having 사용
     * 예시: 팀 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    void group() {
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
     * 6-1. 내부조인
     */
    // 팀 A에 소속된 모든 회원 찾기
    @Test
    void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 6-2. 외부조인
     */
    @Test
    void leftJoin() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 6-3. 세타 조인: 연관관계 없는 테이블 끼리 조인하는 것
     * 예시: 회원의 이름이 팀 이름과 같은 회원 조회
     * 다만 아우터 조인은 불가능!
     */
    @Test
    void thetaJoin() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // join()안에 .으로 연관관계 객체에 대한 접근을 하는 게 아니라, from()에 그냥 합칠 객체들을 나열
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 6-4. 조인 + on절 (조인 방식에 따라 on과 where절의 결과가 같을 때도 있음을 확인해보기)
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회!
     * JPQL: select m, t from Member m left join m.team t on t.name = "teamA"
     */
    @Test
    void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        // left outer 조인이라 member에 있는 모든 레코드는 다 가져옴
        // 다만 팀이름이 teamA에 해당하는 데이터는 정상적으로 들어오고, 그 밖의 데이터에는 null이 들어옴을 확인 가능)
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }


        List<Tuple> result2 = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
//                .on(team.name.eq("teamA")) // on절을 쓰나 where절을 쓰나 내부조인의 경우 결과가 같음.
                .where(team.name.eq("teamA")) // 즉, 내부조인이면 익숙한 where절을 사용하길 추천 (외부조인은 어쩔수없이 on으로 처리해야함)
                .fetch();

        // inner 조인이라 조건에 일치하는 member 데이터만 레코드에 남김옴
        // teamA에 해당하는 데이터만 조인되어 들어오고, 그 밖의 데이터에는 아예 없음을 확인 가능)
        for (Tuple tuple : result2) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 6-5. 연관관계 없는 엔티티 세타 외부 조인
     * 예시: 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team) // 이런식으로 셀렉은 하되
                .from(member) // from에 member 넣고 leftjoin에 .으로 시작되는 연관관계를 나열하는 게 아닌 조인 대상 객체를 그대로 기입
                .leftJoin(team).on(member.username.eq(team.name)) // 외부조인이라 where대신 on에 조건 삽입
                .fetch();
        // 위에서도 설명했지만 내부조인을 할거면 join문 없애고, 그냥 from에 합칠 객체 나열하고 where에 조건넣음 됨

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 6-6. 페치 조인
     * 페치 조인이 없을 때 vs 페치 조인이 있을 때 비교하며 공부해보기
     */
    @PersistenceUnit
    EntityManagerFactory emf; // em을 만드는 팩토리

    @Test
    void fetchJoinNo () {
        em.flush();
        em.clear();

        // fetch타입이 lazy이기 때문에 연관관계 테이블에 대한 쿼리 안날리고, 오직 member 데이터만 딱 불러옴
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    void fetchJoinUse () {
        em.flush();
        em.clear();

        // fetch타입이 lazy이므로 join을 활용하여 연관관계 가져오되 fetch조인을 적용해서 여러번 쿼리를 날라지 않고 한번만 나가게 해줌
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // 하나의 쿼리로 날아갈 수 있게 해줌!
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }


    /**
     * 7-1. 서브 쿼리
     * 1번째 예제: 나이가 가장 많은 회원 조회
     *
     * 참고
     *  - from 절의 서브쿼리 한계
     *      - JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
     *      - 당연히 Querydsl 도 지원하지 않는다.
     *
     *  - from 절의 서브쿼리 해결방안
     *      1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
     *      2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     *      3. nativeSQL을 사용한다.
     */
    @Test
    void subQuery() {
        QMember memberSub = new QMember("memberSub");
        
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 7-2. 나이가 평균 나이 이상인 회원
     */
    @Test
    void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * 7-3. 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10)) // 10살 초과
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    /**
     * 7-4. 셀렉트 절에서 서브쿼리 사용
     * 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. 그래서 Querydsl도 하이버네이트 구현체를 사용해 select 절의 서브쿼리를 지원한다.
     */
    @Test
    void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    /**
     * 8-1. CASE
     * select, 조건절(where), orderby에서 사용 가능
     * 가급적 사용안하는 게 맞다고 봄. db는 데이터를 끌어오는 거에 집중하는거고 그외의 처리는 서비스 로직에서 해주는 게 맞음.
     */
    @Test
    void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 8-2. 복잡한 조건
     */
    @Test
    void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 8.3 orderBy에서 Case 문 함께 사용하기 예제 (참고: 강의 이후 추가된 내용)
     * 예를 들어서 다음과 같은 임의의 순서로 회원을 출력하고 싶다면?
     *  1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
     *  2. 0 ~ 20살 회원 출력
     *  3. 21 ~ 30살 회원 출력
     *
     * Querydsl은 자바 코드로 작성하기 때문에 rankPath 처럼 복잡한 조건을 변수로 선언해서 select 절, orderBy 절에서 함께 사용할 수 있다.
     *  - 결과
     *  username = member4 age = 40 rank = 3
     *  username = member1 age = 10 rank = 2
     *  username = member2 age = 20 rank = 2
     *  username = member3 age = 30 rank = 1
     */
    @Test
    void rankPath() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);
        }
    }


    /**
     * 9-1. 상수, 문자 더하기
     * 상수가 필요하면 Expressions.constant(xxx) 사용
     * 참고: 예시 1번과 같이 최적화가 가능하면 SQL에 constant 값을 넘기지 않는다. 상수를 더하는 것처럼 최적화가 어려우면 SQL에 constant 값을 넘긴다.
     */
    @Test
    void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 9.2 문자 더하기 concat
     * 결과: member1_10
     * 참고: member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue() 로 문자로 변환할 수 있다.
     * 이 방법은 ENUM을 처리할 때도 자주 사용한다.
     */
    @Test
    void concat() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}

