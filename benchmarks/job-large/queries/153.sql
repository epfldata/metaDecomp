SELECT an_1.name AS alternative_name_1,
       chn_1.name AS voiced_char_name_1,
       n_1.name AS voicing_actress_1,
       t_1.title AS american_movie_1,
       an_2.name AS alternative_name_2,
       chn_2.name AS voiced_char_name_2,
       n_2.name AS voicing_actress_2,
       t_2.title AS american_movie_2
FROM aka_name AS an_1,
     aka_name AS an_2,
     cast_info AS ci_1,
     cast_info AS ci_2,
     char_name AS chn_1,
     char_name AS chn_2,
     company_name AS cn_1,
     company_name AS cn_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
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
  AND n_1.gender ='f'
  AND rt_1.role ='actress'
  AND ci_1.movie_id = t_1.id
  AND t_1.id = mc_1.movie_id
  AND ci_1.movie_id = mc_1.movie_id
  AND mc_1.company_id = cn_1.id
  AND ci_1.role_id = rt_1.id
  AND n_1.id = ci_1.person_id
  AND chn_1.id = ci_1.person_role_id
  AND an_1.person_id = n_1.id
  AND an_1.person_id = ci_1.person_id
  AND ci_2.note IN ('(voice)',
                    '(voice: Japanese version)',
                    '(voice) (uncredited)',
                    '(voice: English version)')
  AND cn_2.country_code ='[us]'
  AND n_2.gender ='f'
  AND rt_2.role ='actress'
  AND ci_2.movie_id = t_2.id
  AND t_2.id = mc_2.movie_id
  AND ci_2.movie_id = mc_2.movie_id
  AND mc_2.company_id = cn_2.id
  AND ci_2.role_id = rt_2.id
  AND n_2.id = ci_2.person_id
  AND chn_2.id = ci_2.person_role_id
  AND an_2.person_id = n_2.id
  AND an_2.person_id = ci_2.person_id
  AND an_1.name = an_2.name
GROUP BY an_1.name,
         an_2.name,
         chn_1.name,
         chn_2.name,
         n_1.name,
         n_2.name,
         t_1.title,
         t_2.title;