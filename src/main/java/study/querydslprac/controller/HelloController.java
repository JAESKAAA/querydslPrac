package study.querydslprac.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import study.querydslprac.entity.Member;
import study.querydslprac.repository.MemberRepository;


@RestController
public class HelloController {

    private final MemberRepository memberRepository;

    public HelloController (MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping("/hello")
    public String hello(){
        return "Hello!";
    }

    @PostMapping("/member")
    public String member(Member member) {
        Member result = memberRepository.save(member);
        return result != null ? "성공" : "실패";
    }
}
