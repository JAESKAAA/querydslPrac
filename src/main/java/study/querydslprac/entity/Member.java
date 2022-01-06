package study.querydslprac.entity;

import lombok.*;

import javax.persistence.*;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter @Setter
@Entity
@ToString(of = {"id","username","age"}) //무한참조 방지를위해 연관관계에는 toString설정 지양
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;


    public Member (String username){
        this(username,0);
    }

    public Member (String username, int age){
        this(username,age,null);
    }

    public Member(String username, int age, Team team){
        this.username = username;
        this.age = age;
        if(team != null){
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
