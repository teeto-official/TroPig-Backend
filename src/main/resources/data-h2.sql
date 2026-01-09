-- INSERT CONTENT
INSERT INTO content (
    alias, title, type, member_id, rule, genre,
    player_count_type, term_type, publishing_info,
    status, adult, published_at,
    free_content, non_free_content,
    price, level, search_text
) VALUES
('content-1', '테스트 콘텐츠 1', 'RESOURCE', 1, 'COC', 'ROMANCE', 'ONE_ON_ONE', 'SHORT_TERM',
 null, 'PUBLISHED', false, '2025-12-30 12:00:00',
 '무료 설명 1', '유료 설명 1', 0.0, 1, 'content-1'),

('content-2', '테스트 콘텐츠 2', 'RESOURCE', 1, 'FIASCO', 'ACTION', 'MULTI_PLAYER', 'LONG_TERM',
 null, 'DRAFT', false, null,
 '무료 설명 2', null, 5.99, 3, 'content-2'),

('content-3', '테스트 콘텐츠 3', 'RESOURCE', 2, 'COC', 'COMEDY', 'MULTI_PLAYER', 'LONG_TERM',
 null, 'PUBLISHED', true, '2025-12-31 12:00:00',
 null, '유료 설명 3', 12.99, 5, 'content-3'),

('content-4', '테스트 콘텐츠 4', 'RESOURCE', 2, 'FIASCO', 'ROMANCE', 'ONE_ON_ONE', 'SHORT_TERM',
 null, 'PUBLISHED', false, '2025-11-30 12:00:00',
 '무료 설명 4', '유료 설명 4', 2.99, 2, 'content-4'),

('content-5', '테스트 콘텐츠 5', 'RESOURCE', 3, 'COC', 'ACTION', 'MULTI_PLAYER', 'SHORT_TERM',
 null, 'DRAFT', true, null,
 null, null, 0.0, 1, 'content-5'),

('content-6', '테스트 콘텐츠 6', 'SCENARIO', 3, 'FIASCO', 'COMEDY', 'ONE_ON_ONE', 'LONG_TERM',
 null, 'PUBLISHED', false, CURRENT_TIMESTAMP,
 '무료 설명 6', null, 3.50, 4, 'content-6'),

('content-7', '테스트 콘텐츠 7', 'SCENARIO', 4, 'COC', 'ROMANCE', 'MULTI_PLAYER', 'LONG_TERM',
 null, 'DELETED', false, '2025-10-30 12:00:00',
 null, '유료 설명 7', 19.99, 7, 'content-7'),

('content-8', '테스트 콘텐츠 8', 'SCENARIO', 4, 'FIASCO', 'ACTION', 'ONE_ON_ONE', 'SHORT_TERM',
 null, 'DRAFT', false, null,
 '무료 설명 8', null, 1.99, 1, 'content-8'),

('content-9', '테스트 콘텐츠 9', 'SCENARIO', 5, 'COC', 'COMEDY', 'MULTI_PLAYER', 'LONG_TERM',
 null, 'PUBLISHED', true, '2025-12-01 12:00:00',
 null, '유료 설명 9', 9.99, 6, 'content-9'),

('content-10', '테스트 콘텐츠 10', 'SCENARIO', 5, 'FIASCO', 'ROMANCE', 'ONE_ON_ONE', 'LONG_TERM',
 null, 'PUBLISHED', false, '2025-12-01 12:00:00',
 '무료 설명 10', '유료 설명 10', 4.99, 3, 'content-10');

-- INSERT TAG
INSERT INTO tag (name, type) VALUES
('미스터리','CATEGORY'),
('공포','CATEGORY'),
('군부','CATEGORY'),
('그로테스크','CATEGORY'),
('드라마','CATEGORY'),
('순정','CATEGORY'),
('로맨스','CATEGORY'),
('로맨스 판타지','CATEGORY'),
('아포칼립스','CATEGORY'),
('무협','CATEGORY'),
('여행','CATEGORY'),
('백합','CATEGORY'),
('서스펜스','CATEGORY'),
('좀비','CATEGORY'),
('학교','CATEGORY'),
('스릴러','CATEGORY');

INSERT INTO tag (name, type) VALUES
('성장물','MOOD'),
('비극','MOOD'),
('슬픔','MOOD'),
('러브코미디','MOOD'),
('블랙코미디','MOOD'),
('힐링','MOOD'),
('피폐','MOOD'),
('잔잔','MOOD'),
('시리어스','MOOD'),
('개그','MOOD');

INSERT INTO tag (name, type) VALUES
('연애','SUBJECT'),
('가족','SUBJECT'),
('강아지','SUBJECT'),
('거꾸로 된 수레','SUBJECT'),
('게임','SUBJECT'),
('계약결혼','SUBJECT'),
('기억상실','SUBJECT'),
('꿈','SUBJECT'),
('동거','SUBJECT'),
('빙의','SUBJECT'),
('환생','SUBJECT'),
('후회','SUBJECT'),
('SNS','SUBJECT');

INSERT INTO tag (name, type) VALUES
('군대','PLACE'),
('고등학교','PLACE'),
('우주','PLACE'),
('한국','PLACE'),
('캠퍼스','PLACE'),
('궁정','PLACE'),
('카페','PLACE'),
('사무실','PLACE'),
('마계','PLACE'),
('저승','PLACE'),
('하숙집','PLACE'),
('체육관','PLACE'),
('병원','PLACE'),
('중학교','PLACE'),
('온천','PLACE'),
('관공서','PLACE'),
('유치원','PLACE'),
('감옥','PLACE');

INSERT INTO content_tag (content_id, tag_id) VALUES
(1, 4),
(1, 10),
(2, 34),
(2, 11),
(3, 1),
(4, 7),
(5, 9),
(5, 12),
(6, 24),
(6, 10),
(7, 24),
(8, 14),
(9, 2),
(9, 17),
(10, 6),
(10, 14);

INSERT INTO member (
  sns_id, sns_provider, email, nickname, role,
  adult, bio, marketing_at, deleted_at,
  favorite_genres, favorite_rules, profile,
  created_at, updated_at
) VALUES (
  'test_sns_id_001', 'GOOGLE', 'test1@example.com', '테스트유저1', 'CREATOR',
  TRUE, '소개입니다', NULL, NULL,
  '로맨스, 스릴러', '피폐 금지', NULL,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
), (
     'test_sns_id_002', 'GOOGLE', 'test2@example.com', '테스트유저2', 'CREATOR',
     TRUE, '소개입니다', NULL, NULL,
     '로맨스, 스릴러', '피폐 금지', NULL,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
   ), (
        'test_sns_id_003', 'GOOGLE', 'test3@example.com', '테스트유저3', 'CREATOR',
        TRUE, '소개입니다', NULL, NULL,
        '로맨스, 스릴러', '피폐 금지', NULL,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
      ), (
           'test_sns_id_004', 'GOOGLE', 'test4@example.com', '테스트유저4', 'CREATOR',
           TRUE, '소개입니다', NULL, NULL,
           '로맨스, 스릴러', '피폐 금지', NULL,
           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         ), (
              'test_sns_id_005', 'GOOGLE', 'test5@example.com', '테스트유저5', 'CREATOR',
              TRUE, '소개입니다', NULL, NULL,
              '로맨스, 스릴러', '피폐 금지', NULL,
              CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            );