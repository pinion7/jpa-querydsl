package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 기본 생성자 생성
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection // 생성자에 이 애노테이션을 넣고 compileQuerydsl을 실행하면 -> dto도 Q파일 생성해줌!
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
