SELECT chn_1.name AS character_name_1,
       mi_idx_1.info AS rating_1,
       t_1.title AS complete_hero_movie_1,
       chn_2.name AS character_name_2,
       mi_idx_2.info AS rating_2,
       t_2.title AS complete_hero_movie_2
FROM cast_info AS ci_1,
     cast_info AS ci_2,
     char_name AS chn_1,
     char_name AS chn_2,
     comp_cast_type AS cct1_1,
     comp_cast_type AS cct1_2,
     comp_cast_type AS cct2_1,
     comp_cast_type AS cct2_2,
     complete_cast AS cc_1,
     complete_cast AS cc_2,
     info_type AS it2_1,
     info_type AS it2_2,
     keyword AS k_1,
     keyword AS k_2,
     kind_type AS kt_1,
     kind_type AS kt_2,
     movie_info_idx AS mi_idx_1,
     movie_info_idx AS mi_idx_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     name AS n_1,
     name AS n_2,
     title AS t_1,
     title AS t_2
WHERE cct1_1.kind = 'cast'
  AND cct2_1.kind LIKE '%complete%'
  AND chn_1.name IS NOT NULL
  AND (chn_1.name LIKE '%man%'
       OR chn_1.name LIKE '%Man%')
  AND it2_1.info = 'rating'
  AND k_1.keyword IN ('superhero',
                      'marvel-comics',
                      'based-on-comic',
                      'fight')
  AND kt_1.kind = 'movie'
  AND mi_idx_1.info > '8.0'
  AND t_1.production_year > 2005
  AND kt_1.id = t_1.kind_id
  AND t_1.id = mk_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND t_1.id = cc_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND mk_1.movie_id = ci_1.movie_id
  AND mk_1.movie_id = cc_1.movie_id
  AND mk_1.movie_id = mi_idx_1.movie_id
  AND ci_1.movie_id = cc_1.movie_id
  AND ci_1.movie_id = mi_idx_1.movie_id
  AND cc_1.movie_id = mi_idx_1.movie_id
  AND chn_1.id = ci_1.person_role_id
  AND n_1.id = ci_1.person_id
  AND k_1.id = mk_1.keyword_id
  AND cct1_1.id = cc_1.subject_id
  AND cct2_1.id = cc_1.status_id
  AND it2_1.id = mi_idx_1.info_type_id
  AND cct1_2.kind = 'cast'
  AND cct2_2.kind LIKE '%complete%'
  AND chn_2.name IS NOT NULL
  AND (chn_2.name LIKE '%man%'
       OR chn_2.name LIKE '%Man%')
  AND it2_2.info = 'rating'
  AND k_2.keyword IN ('superhero',
                      'marvel-comics',
                      'based-on-comic',
                      'fight')
  AND kt_2.kind = 'movie'
  AND mi_idx_2.info > '8.0'
  AND t_2.production_year > 2005
  AND kt_2.id = t_2.kind_id
  AND t_2.id = mk_2.movie_id
  AND t_2.id = ci_2.movie_id
  AND t_2.id = cc_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND mk_2.movie_id = ci_2.movie_id
  AND mk_2.movie_id = cc_2.movie_id
  AND mk_2.movie_id = mi_idx_2.movie_id
  AND ci_2.movie_id = cc_2.movie_id
  AND ci_2.movie_id = mi_idx_2.movie_id
  AND cc_2.movie_id = mi_idx_2.movie_id
  AND chn_2.id = ci_2.person_role_id
  AND n_2.id = ci_2.person_id
  AND k_2.id = mk_2.keyword_id
  AND cct1_2.id = cc_2.subject_id
  AND cct2_2.id = cc_2.status_id
  AND it2_2.id = mi_idx_2.info_type_id
  AND chn_1.name = chn_2.name
GROUP BY chn_1.name,
         chn_2.name,
         mi_idx_1.info,
         mi_idx_2.info,
         t_1.title,
         t_2.title;