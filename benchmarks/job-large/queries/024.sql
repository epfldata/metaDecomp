SELECT kt_1.kind AS movie_kind_1,
       t_1.title AS complete_us_internet_movie_1,
       kt_2.kind AS movie_kind_2,
       t_2.title AS complete_us_internet_movie_2
FROM comp_cast_type AS cct1_1,
     comp_cast_type AS cct1_2,
     company_name AS cn_1,
     company_name AS cn_2,
     company_type AS ct_1,
     company_type AS ct_2,
     complete_cast AS cc_1,
     complete_cast AS cc_2,
     info_type AS it1_1,
     info_type AS it1_2,
     keyword AS k_1,
     keyword AS k_2,
     kind_type AS kt_1,
     kind_type AS kt_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     title AS t_1,
     title AS t_2
WHERE cct1_1.kind = 'complete+verified'
  AND cn_1.country_code = '[us]'
  AND it1_1.info = 'release dates'
  AND kt_1.kind IN ('movie',
                    'tv movie',
                    'video movie',
                    'video game')
  AND mi_1.note LIKE '%internet%'
  AND mi_1.info IS NOT NULL
  AND (mi_1.info LIKE 'USA:% 199%'
       OR mi_1.info LIKE 'USA:% 200%')
  AND t_1.production_year > 1990
  AND kt_1.id = t_1.kind_id
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mk_1.movie_id
  AND t_1.id = mc_1.movie_id
  AND t_1.id = cc_1.movie_id
  AND mk_1.movie_id = mi_1.movie_id
  AND mk_1.movie_id = mc_1.movie_id
  AND mk_1.movie_id = cc_1.movie_id
  AND mi_1.movie_id = mc_1.movie_id
  AND mi_1.movie_id = cc_1.movie_id
  AND mc_1.movie_id = cc_1.movie_id
  AND k_1.id = mk_1.keyword_id
  AND it1_1.id = mi_1.info_type_id
  AND cn_1.id = mc_1.company_id
  AND ct_1.id = mc_1.company_type_id
  AND cct1_1.id = cc_1.status_id
  AND cct1_2.kind = 'complete+verified'
  AND cn_2.country_code = '[us]'
  AND it1_2.info = 'release dates'
  AND kt_2.kind IN ('movie',
                    'tv movie',
                    'video movie',
                    'video game')
  AND mi_2.note LIKE '%internet%'
  AND mi_2.info IS NOT NULL
  AND (mi_2.info LIKE 'USA:% 199%'
       OR mi_2.info LIKE 'USA:% 200%')
  AND t_2.production_year > 1990
  AND kt_2.id = t_2.kind_id
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mk_2.movie_id
  AND t_2.id = mc_2.movie_id
  AND t_2.id = cc_2.movie_id
  AND mk_2.movie_id = mi_2.movie_id
  AND mk_2.movie_id = mc_2.movie_id
  AND mk_2.movie_id = cc_2.movie_id
  AND mi_2.movie_id = mc_2.movie_id
  AND mi_2.movie_id = cc_2.movie_id
  AND mc_2.movie_id = cc_2.movie_id
  AND k_2.id = mk_2.keyword_id
  AND it1_2.id = mi_2.info_type_id
  AND cn_2.id = mc_2.company_id
  AND ct_2.id = mc_2.company_type_id
  AND cct1_2.id = cc_2.status_id
  AND kt_1.kind = kt_2.kind
GROUP BY kt_1.kind,
         kt_2.kind,
         t_1.title,
         t_2.title;