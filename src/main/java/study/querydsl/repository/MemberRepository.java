package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {
    List<Member> findByUsername(String username);
}

/**
 * 스프링 데이터 정렬(Sort)에 관해...
 * 스프링 데이터 JPA는 자신의 정렬(Sort)을 Querydsl의 정렬(OrderSpecifier)로 편리하게 변경하는 기능을 제공한다.
 * 이 부분은 뒤에 스프링 데이터 JPA가 제공하는 Querydsl 기능에서 살펴보겠다.
 * 스프링 데이터의 정렬을 Querydsl의 정렬로 직접 전환하는 방법은 다음 코드를 참고하자.
 *
 * 스프링 데이터 Sort를 Querydsl의 OrderSpecifier로 변환 예시)
 *     JPAQuery<Member> query = queryFactory
 *             .selectFrom(member);
 *
 *     for (Sort.Order o : pageable.getSort()) {
 *         PathBuilder pathBuilder = new PathBuilder(member.getType(), member.getMetadata());
 *         query.orderBy(new OrderSpecifier(
 *              o.isAscending() ? Order.ASC : Order.DESC,
 *              pathBuilder.get(o.getProperty())
 *         ));
 *     }
 *
 *     List<Member> result = query.fetch();
 *
 * 참고: 정렬( Sort )은 조건이 조금만 복잡해져도 Pageable 의 Sort 기능을 사용하기 어렵다.
 * 루트 엔티티 범위를 넘어가는 동적 정렬 기능이 필요하면 스프링 데이터 페이징이 제공하는 Sort 를 사용하기 보다는,
 * 파라미터를 받아서 직접 처리하는 것을 권장한다.
 */