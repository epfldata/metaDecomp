SELECT t_1.title AS movie_title_1,
       t_2.title AS movie_title_2
FROM keyword AS k_1,
     keyword AS k_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     title AS t_1,
     title AS t_2
WHERE k_1.keyword LIKE '%sequel%'
  AND mi_1.info IN ('Bulgaria')
  AND t_1.production_year > 2010
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mk_1.movie_id
  AND mk_1.movie_id = mi_1.movie_id
  AND k_1.id = mk_1.keyword_id
  AND k_2.keyword LIKE '%sequel%'
  AND mi_2.info IN ('Bulgaria')
  AND t_2.production_year > 2010
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mk_2.movie_id
  AND mk_2.movie_id = mi_2.movie_id
  AND k_2.id = mk_2.keyword_id
  AND t_1.title = t_2.title
GROUP BY t_1.title,
         t_2.title;