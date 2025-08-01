SELECT k_1.keyword AS movie_keyword_1,
       n_1.name AS actor_name_1,
       t_1.title AS marvel_movie_1,
       k_2.keyword AS movie_keyword_2,
       n_2.name AS actor_name_2,
       t_2.title AS marvel_movie_2
FROM cast_info AS ci_1,
     cast_info AS ci_2,
     keyword AS k_1,
     keyword AS k_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     name AS n_1,
     name AS n_2,
     title AS t_1,
     title AS t_2
WHERE k_1.keyword = 'marvel-cinematic-universe'
  AND n_1.name LIKE '%Downey%Robert%'
  AND t_1.production_year > 2014
  AND k_1.id = mk_1.keyword_id
  AND t_1.id = mk_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND ci_1.movie_id = mk_1.movie_id
  AND n_1.id = ci_1.person_id
  AND k_2.keyword = 'marvel-cinematic-universe'
  AND n_2.name LIKE '%Downey%Robert%'
  AND t_2.production_year > 2014
  AND k_2.id = mk_2.keyword_id
  AND t_2.id = mk_2.movie_id
  AND t_2.id = ci_2.movie_id
  AND ci_2.movie_id = mk_2.movie_id
  AND n_2.id = ci_2.person_id
  AND k_1.keyword = k_2.keyword
GROUP BY k_1.keyword,
         k_2.keyword,
         n_1.name,
         n_2.name,
         t_1.title,
         t_2.title;