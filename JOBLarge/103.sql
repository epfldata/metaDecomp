SELECT chn_1.name AS voiced_char_name_1,
       n_1.name AS voicing_actress_name_1,
       t_1.title AS voiced_action_movie_jap_eng_1,
       chn_2.name AS voiced_char_name_2,
       n_2.name AS voicing_actress_name_2,
       t_2.title AS voiced_action_movie_jap_eng_2
FROM aka_name AS an_1,
     aka_name AS an_2,
     cast_info AS ci_1,
     cast_info AS ci_2,
     char_name AS chn_1,
     char_name AS chn_2,
     company_name AS cn_1,
     company_name AS cn_2,
     info_type AS it_1,
     info_type AS it_2,
     keyword AS k_1,
     keyword AS k_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     name AS n_1,
     name AS n_2,
     role_type AS rt_1,
     role_type AS rt_2,
     title AS t_1,
     title AS t_2
WHERE ci_1.note IN ('(voice)',
                    '(voice: Japanese version)',
                    '(voice) (uncredited)',
                    '(voice: English version)')
  AND cn_1.country_code ='[us]'
  AND it_1.info = 'release dates'
  AND k_1.keyword IN ('hero',
                      'martial-arts',
                      'hand-to-hand-combat')
  AND mi_1.info IS NOT NULL
  AND (mi_1.info LIKE 'Japan:%201%'
       OR mi_1.info LIKE 'USA:%201%')
  AND n_1.gender ='f'
  AND n_1.name LIKE '%An%'
  AND rt_1.role ='actress'
  AND t_1.production_year > 2010
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mc_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND t_1.id = mk_1.movie_id
  AND mc_1.movie_id = ci_1.movie_id
  AND mc_1.movie_id = mi_1.movie_id
  AND mc_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = ci_1.movie_id
  AND mi_1.movie_id = mk_1.movie_id
  AND ci_1.movie_id = mk_1.movie_id
  AND cn_1.id = mc_1.company_id
  AND it_1.id = mi_1.info_type_id
  AND n_1.id = ci_1.person_id
  AND rt_1.id = ci_1.role_id
  AND n_1.id = an_1.person_id
  AND ci_1.person_id = an_1.person_id
  AND chn_1.id = ci_1.person_role_id
  AND k_1.id = mk_1.keyword_id
  AND ci_2.note IN ('(voice)',
                    '(voice: Japanese version)',
                    '(voice) (uncredited)',
                    '(voice: English version)')
  AND cn_2.country_code ='[us]'
  AND it_2.info = 'release dates'
  AND k_2.keyword IN ('hero',
                      'martial-arts',
                      'hand-to-hand-combat')
  AND mi_2.info IS NOT NULL
  AND (mi_2.info LIKE 'Japan:%201%'
       OR mi_2.info LIKE 'USA:%201%')
  AND n_2.gender ='f'
  AND n_2.name LIKE '%An%'
  AND rt_2.role ='actress'
  AND t_2.production_year > 2010
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mc_2.movie_id
  AND t_2.id = ci_2.movie_id
  AND t_2.id = mk_2.movie_id
  AND mc_2.movie_id = ci_2.movie_id
  AND mc_2.movie_id = mi_2.movie_id
  AND mc_2.movie_id = mk_2.movie_id
  AND mi_2.movie_id = ci_2.movie_id
  AND mi_2.movie_id = mk_2.movie_id
  AND ci_2.movie_id = mk_2.movie_id
  AND cn_2.id = mc_2.company_id
  AND it_2.id = mi_2.info_type_id
  AND n_2.id = ci_2.person_id
  AND rt_2.id = ci_2.role_id
  AND n_2.id = an_2.person_id
  AND ci_2.person_id = an_2.person_id
  AND chn_2.id = ci_2.person_role_id
  AND k_2.id = mk_2.keyword_id
  AND chn_1.name = chn_2.name
GROUP BY chn_1.name,
         chn_2.name,
         n_1.name,
         n_2.name,
         t_1.title,
         t_2.title;