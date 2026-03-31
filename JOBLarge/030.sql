SELECT an_1.name AS cool_actor_pseudonym_1,
       t_1.title AS series_named_after_char_1,
       an_2.name AS cool_actor_pseudonym_2,
       t_2.title AS series_named_after_char_2
FROM aka_name AS an_1,
     aka_name AS an_2,
     cast_info AS ci_1,
     cast_info AS ci_2,
     company_name AS cn_1,
     company_name AS cn_2,
     keyword AS k_1,
     keyword AS k_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     name AS n_1,
     name AS n_2,
     title AS t_1,
     title AS t_2
WHERE cn_1.country_code ='[us]'
  AND k_1.keyword ='character-name-in-title'
  AND t_1.episode_nr < 100
  AND an_1.person_id = n_1.id
  AND n_1.id = ci_1.person_id
  AND ci_1.movie_id = t_1.id
  AND t_1.id = mk_1.movie_id
  AND mk_1.keyword_id = k_1.id
  AND t_1.id = mc_1.movie_id
  AND mc_1.company_id = cn_1.id
  AND an_1.person_id = ci_1.person_id
  AND ci_1.movie_id = mc_1.movie_id
  AND ci_1.movie_id = mk_1.movie_id
  AND mc_1.movie_id = mk_1.movie_id
  AND cn_2.country_code ='[us]'
  AND k_2.keyword ='character-name-in-title'
  AND t_2.episode_nr < 100
  AND an_2.person_id = n_2.id
  AND n_2.id = ci_2.person_id
  AND ci_2.movie_id = t_2.id
  AND t_2.id = mk_2.movie_id
  AND mk_2.keyword_id = k_2.id
  AND t_2.id = mc_2.movie_id
  AND mc_2.company_id = cn_2.id
  AND an_2.person_id = ci_2.person_id
  AND ci_2.movie_id = mc_2.movie_id
  AND ci_2.movie_id = mk_2.movie_id
  AND mc_2.movie_id = mk_2.movie_id
  AND an_1.name = an_2.name
GROUP BY an_1.name,
         an_2.name,
         t_1.title,
         t_2.title;