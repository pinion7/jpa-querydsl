package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component
@RequiredArgsConstructor
public class initMember {

    private final InitMemberService initMemberService;

    /**
     * 그냥 static 클래스를 추가로 안만들고 아래처럼 하나에 init 메서드에 관련 로직을 다 넣으면 안되나? 라고 생각할 수 있는데
     * 2가지 애노테이션 - @PostConstruct, @Transactional이 라이프 사이클 상 같이 넣으면 문제가 생겨서 분리해야함.
     */
//    private final EntityManager em;
//
//    @PostConstruct
//    @Transactional
//    public void init() {
//        Team teamA = new Team("teamA");
//        Team teamB = new Team("teamB");
//        em.persist(teamA);
//        em.persist(teamB);
//
//        for (int i = 0; i < 100; i++) {
//            Team selectedTeam = i % 2 == 0 ? teamA : teamB;
//            em.persist(new Member("member" + i, i, selectedTeam));
//        }
//    }

    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {
        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }
    }
}
