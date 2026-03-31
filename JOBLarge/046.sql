SELECT cn_1.name AS producing_company_1,
       miidx_1.info AS rating_1,
       t_1.title AS movie_1,
       cn_2.name AS producing_company_2,
       miidx_2.info AS rating_2,
       t_2.title AS movie_2
FROM company_name AS cn_1,
     company_name AS cn_2,
     company_type AS ct_1,
     company_type AS ct_2,
     info_type AS it2_1,
     info_type AS it2_2,
     info_type AS it_1,
     info_type AS it_2,
     kind_type AS kt_1,
     kind_type AS kt_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_info_idx AS miidx_1,
     movie_info_idx AS miidx_2,
     title AS t_1,
     title AS t_2
WHERE cn_1.country_code ='[us]'
  AND ct_1.kind ='production companies'
  AND it_1.info ='rating'
  AND it2_1.info ='release dates'
  AND kt_1.kind ='movie'
  AND mi_1.movie_id = t_1.id
  AND it2_1.id = mi_1.info_type_id
  AND kt_1.id = t_1.kind_id
  AND mc_1.movie_id = t_1.id
  AND cn_1.id = mc_1.company_id
  AND ct_1.id = mc_1.company_type_id
  AND miidx_1.movie_id = t_1.id
  AND it_1.id = miidx_1.info_type_id
  AND mi_1.movie_id = miidx_1.movie_id
  AND mi_1.movie_id = mc_1.movie_id
  AND miidx_1.movie_id = mc_1.movie_id
  AND cn_2.country_code ='[us]'
  AND ct_2.kind ='production companies'
  AND it_2.info ='rating'
  AND it2_2.info ='release dates'
  AND kt_2.kind ='movie'
  AND mi_2.movie_id = t_2.id
  AND it2_2.id = mi_2.info_type_id
  AND kt_2.id = t_2.kind_id
  AND mc_2.movie_id = t_2.id
  AND cn_2.id = mc_2.company_id
  AND ct_2.id = mc_2.company_type_id
  AND miidx_2.movie_id = t_2.id
  AND it_2.id = miidx_2.info_type_id
  AND mi_2.movie_id = miidx_2.movie_id
  AND mi_2.movie_id = mc_2.movie_id
  AND miidx_2.movie_id = mc_2.movie_id
  AND cn_1.name = cn_2.name
GROUP BY cn_1.name,
         cn_2.name,
         miidx_1.info,
         miidx_2.info,
         t_1.title,
         t_2.title;