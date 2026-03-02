package com.tropig.backend.contents.enums

enum class PublishingType(val displayName: String) {
    PDF("PDF"),
    EXTERNAL_LINK("외부 링크"),
    SCENARIO_FILE(""),
    IMAGE("이미지"),
    BGM("BGM"),
    COCOFORIA("코코포리아"),
    ROLL20("롤20"),
    DOCX("글"),
    ETC("기타"),
    TOKEN("토큰"),
    NPC("NPC"),
    MAP("지도"),
    ITEM("아이템"),
    SUB_RULE("보조룰"),
    HOUSE_RULE("하우스룰"),
    SYSTEM_SUMMARY("시스템 요약"),
    BATTLE_RULE("전투 규칙"),
    CHARACTER_SHEET("캐릭터 시트"),
    FVTT("FVTT"),
    DISCORD_BOT("디스코드봇"),
}
