package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
class MemberRepositoryTest {
    @Autowired
    EntityManager em;

    @Autowired MemberRepository memberRepository;

    @Test
    void basicTest() {
        // given
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        // when
        Member findMember = memberRepository.findById(member.getId()).get();
        List<Member> result1 = memberRepository.findAll();
        List<Member> result2 = memberRepository.findByUsername("member1");

        //then
        assertThat(findMember).isEqualTo(member);
        assertThat(result1).containsExactly(member);
        assertThat(result2).containsExactly(member);
    }

    @Test
    void searchTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberRepository.search(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    void searchPageSimpleTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();

        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);

        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }

    @Test
    void searchPageComplexTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();

        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<MemberTeamDto> result = memberRepository.searchPageComplex(condition, pageRequest);

        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }

    /**
     * 1. QuerydslPredicateExecutor의 한계점
     * 조인X (묵시적 조인은 가능하지만 left join이 불가능하다.)
     * 클라이언트가 Querydsl에 의존해야 한다. 서비스 클래스가 Querydsl이라는 구현 기술에 의존해야 한다.
     * 복잡한 실무환경에서 사용하기에는 한계가 명확하다.
     * > 참고: QuerydslPredicateExecutor 는 Pageable, Sort를 모두 지원하고 정상 동작한다.
     *
     *
     * 2. Querydsl Web 한계점 - 너무 쓸수가 없어서 샘플코드 그냥 안쓰고 추가로 설명하겠음
     * 단순한 조건만 가능
     * 조건을 커스텀하는 기능이 복잡하고 명시적이지 않음 컨트롤러가 Querydsl에 의존
     * 복잡한 실무환경에서 사용하기에는 한계가 명확
     *
     *
     * 3. 리포지토리 지원 - QuerydslRepositorySupport - 테스트 코드는 안만들었지만 MemberRepositoryImpl에 구현 코드는 있음. (참고하길)
     * 1) 장점
     * getQuerydsl().applyPagination() 스프링 데이터가 제공하는 페이징을 Querydsl로 편리하게 변환 가능(단! Sort는 오류발생)
     * from() 으로 시작 가능(최근에는 QueryFactory를 사용해서 select() 로 시작하는 것이 더 명시적) EntityManager 제공
     *
     * 2) 한계
     * Querydsl 3.x 버전을 대상으로 만듬
     * Querydsl 4.x에 나온 JPAQueryFactory로 시작할 수 없음
     * select로 시작할 수 없음 (from으로 시작해야함) QueryFactory 를 제공하지 않음
     * 스프링 데이터 Sort 기능이 정상 동작하지 않음
     */
    @Test
    void querydslPredicateExecutorTest() {
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

        Iterable<Member> result = memberRepository.findAll(member.age.between(10, 40).and(member.username.eq("member1")));
        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }

    /**

     */

}