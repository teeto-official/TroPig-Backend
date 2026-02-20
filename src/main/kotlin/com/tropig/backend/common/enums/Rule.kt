package com.tropig.backend.common.enums

enum class Rule(val displayName: String) {
    DND("디앤디"),
    COC("크툴루의 부름"),
    FIASCO("피아스코"),
    PATH_FINDER("패스파인더"),
    STAR_FINDER("스타파인더"),
    VTM("뱀파이어: 마스커레이드"),
    SHADOW_RUN("섀도우런"),
    SWORD_WORLD("검과 마법의 나라"),
    FATE_CORE("페이트 코어"),
    DUNGEON_WORLD("던전 월드"),
    GURPS("GURPS"),
    SAVAGE_WORLDS("새비지 월드"),
    TRAVELLER("트래블러"),
    APOCALYPSE_WORLD("아포칼립스 월드"),
    AGE_OF_REBELLION("에이지 오브 리벨리온"),
    WRATH_GLORY("워해머 40K: 렉시엄"),
    CYBERPUNK_RED("사이버펑크 레드"),
    MIST("미스트"),
    LH("로그 호라이즌"),
    TENEBRIS("테네브리스"),
    AGE_13TH("13시대"),
    MHI("몬스터 헌터 인터내셔널"),
    MAGE("마법사: 어센션"),
    RAID("레이드"),
    INUYASHA("이누야샤 RPG"),
    AKINAIZER("아키나이저"),
    NUMENERA("누메네라"),
    GHOSTBUSTERS("고스트 버스터즈 TRPG"),
    DRAMATIC("드라마틱 RPG"),
    DOUBLE_CROSS("덥크"),
    INSANE("인세인"),
    MAGIKAROGIA("마기카로기아"),
    BLADE("어둠 속의 칼날"),
    SUNSET("저녁노을 어스름"),
    MOUSE_GUARD("마우스가드"),
    STELLA_KNIGHTS("은검의 스텔라나이츠"),
    TEAM_SHERLOCK("팀 셜록"),
    HOME_THEATER("안방극장 대모험"),
    NIGHT_WIZARD("나이트 위저드"),
    TOKYO_NIGHTMARE("토쿄 나이트메어"),
    ARIAN_ROAD("아리안로드 RPG"),
    NECRONICA("네크로니카"),
    RED_BLACK("적과흑"),
    BLOOD_PASS("블러드 패스 RPG"),
    UNSUNG_DUET("언성 듀엣"),
    RESOURCE("")
    ;

    companion object {
        fun fromList(rules: String?): List<Rule> {
            return rules?.split(",")
                ?.map { Rule.valueOf(it) }
                ?: emptyList()
        }
    }
}