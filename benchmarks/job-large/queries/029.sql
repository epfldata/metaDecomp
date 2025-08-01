SELECT t_1.title AS movie_title_1,
       t_2.title AS movie_title_2
FROM company_name AS cn_1,
     company_name AS cn_2,
     keyword AS k_1,
     keyword AS k_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     title AS t_1,
     title AS t_2
WHERE cn_1.country_code ='[sm]'
  AND k_1.keyword ='character-name-in-title'
  AND cn_1.id = mc_1.company_id
  AND mc_1.movie_id = t_1.id
  AND t_1.id = mk_1.movie_id
  AND mk_1.keyword_id = k_1.id
  AND mc_1.movie_id = mk_1.movie_id
  AND cn_2.country_code ='[sm]'
  AND k_2.keyword ='character-name-in-title'
  AND cn_2.id = mc_2.company_id
  AND mc_2.movie_id = t_2.id
  AND t_2.id = mk_2.movie_id
  AND mk_2.keyword_id = k_2.id
  AND mc_2.movie_id = mk_2.movie_id
  AND t_1.title = t_2.title
GROUP BY t_1.title,
         t_2.title;