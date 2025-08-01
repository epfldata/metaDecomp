SELECT cn_1.name AS company_name_1,
       lt_1.link AS link_type_1,
       t_1.title AS german_follow_up_1,
       cn_2.name AS company_name_2,
       lt_2.link AS link_type_2,
       t_2.title AS german_follow_up_2
FROM company_name AS cn_1,
     company_name AS cn_2,
     company_type AS ct_1,
     company_type AS ct_2,
     keyword AS k_1,
     keyword AS k_2,
     link_type AS lt_1,
     link_type AS lt_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     movie_link AS ml_1,
     movie_link AS ml_2,
     title AS t_1,
     title AS t_2
WHERE cn_1.country_code !='[pl]'
  AND (cn_1.name LIKE '%Film%'
       OR cn_1.name LIKE '%Warner%')
  AND ct_1.kind ='production companies'
  AND k_1.keyword ='sequel'
  AND lt_1.link LIKE '%follow%'
  AND mc_1.note IS NULL
  AND mi_1.info IN ('Germany',
                    'German')
  AND t_1.production_year BETWEEN 2000 AND 2010
  AND lt_1.id = ml_1.link_type_id
  AND ml_1.movie_id = t_1.id
  AND t_1.id = mk_1.movie_id
  AND mk_1.keyword_id = k_1.id
  AND t_1.id = mc_1.movie_id
  AND mc_1.company_type_id = ct_1.id
  AND mc_1.company_id = cn_1.id
  AND mi_1.movie_id = t_1.id
  AND ml_1.movie_id = mk_1.movie_id
  AND ml_1.movie_id = mc_1.movie_id
  AND mk_1.movie_id = mc_1.movie_id
  AND ml_1.movie_id = mi_1.movie_id
  AND mk_1.movie_id = mi_1.movie_id
  AND mc_1.movie_id = mi_1.movie_id
  AND cn_2.country_code !='[pl]'
  AND (cn_2.name LIKE '%Film%'
       OR cn_2.name LIKE '%Warner%')
  AND ct_2.kind ='production companies'
  AND k_2.keyword ='sequel'
  AND lt_2.link LIKE '%follow%'
  AND mc_2.note IS NULL
  AND mi_2.info IN ('Germany',
                    'German')
  AND t_2.production_year BETWEEN 2000 AND 2010
  AND lt_2.id = ml_2.link_type_id
  AND ml_2.movie_id = t_2.id
  AND t_2.id = mk_2.movie_id
  AND mk_2.keyword_id = k_2.id
  AND t_2.id = mc_2.movie_id
  AND mc_2.company_type_id = ct_2.id
  AND mc_2.company_id = cn_2.id
  AND mi_2.movie_id = t_2.id
  AND ml_2.movie_id = mk_2.movie_id
  AND ml_2.movie_id = mc_2.movie_id
  AND mk_2.movie_id = mc_2.movie_id
  AND ml_2.movie_id = mi_2.movie_id
  AND mk_2.movie_id = mi_2.movie_id
  AND mc_2.movie_id = mi_2.movie_id
  AND cn_1.name = cn_2.name
GROUP BY cn_1.name,
         cn_2.name,
         lt_1.link,
         lt_2.link,
         t_1.title,
         t_2.title;