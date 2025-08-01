SELECT mi_idx_1.info AS rating_1,
       t_1.title AS movie_title_1,
       mi_idx_2.info AS rating_2,
       t_2.title AS movie_title_2
FROM info_type AS it_1,
     info_type AS it_2,
     keyword AS k_1,
     keyword AS k_2,
     movie_info_idx AS mi_idx_1,
     movie_info_idx AS mi_idx_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     title AS t_1,
     title AS t_2
WHERE it_1.info ='rating'
  AND k_1.keyword LIKE '%sequel%'
  AND mi_idx_1.info > '9.0'
  AND t_1.production_year > 2010
  AND t_1.id = mi_idx_1.movie_id
  AND t_1.id = mk_1.movie_id
  AND mk_1.movie_id = mi_idx_1.movie_id
  AND k_1.id = mk_1.keyword_id
  AND it_1.id = mi_idx_1.info_type_id
  AND it_2.info ='rating'
  AND k_2.keyword LIKE '%sequel%'
  AND mi_idx_2.info > '9.0'
  AND t_2.production_year > 2010
  AND t_2.id = mi_idx_2.movie_id
  AND t_2.id = mk_2.movie_id
  AND mk_2.movie_id = mi_idx_2.movie_id
  AND k_2.id = mk_2.keyword_id
  AND it_2.id = mi_idx_2.info_type_id
  AND mi_idx_1.info = mi_idx_2.info
GROUP BY mi_idx_1.info,
         mi_idx_2.info,
         t_1.title,
         t_2.title;