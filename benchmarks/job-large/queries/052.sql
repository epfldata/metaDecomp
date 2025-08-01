SELECT lt_1.link AS link_type_1,
       t1_1.title AS first_movie_1,
       t2_1.title AS second_movie_1,
       lt_2.link AS link_type_2,
       t1_2.title AS first_movie_2,
       t2_2.title AS second_movie_2
FROM keyword AS k_1,
     keyword AS k_2,
     link_type AS lt_1,
     link_type AS lt_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     movie_link AS ml_1,
     movie_link AS ml_2,
     title AS t1_1,
     title AS t1_2,
     title AS t2_1,
     title AS t2_2
WHERE k_1.keyword ='10,000-mile-club'
  AND mk_1.keyword_id = k_1.id
  AND t1_1.id = mk_1.movie_id
  AND ml_1.movie_id = t1_1.id
  AND ml_1.linked_movie_id = t2_1.id
  AND lt_1.id = ml_1.link_type_id
  AND mk_1.movie_id = t1_1.id
  AND k_2.keyword ='10,000-mile-club'
  AND mk_2.keyword_id = k_2.id
  AND t1_2.id = mk_2.movie_id
  AND ml_2.movie_id = t1_2.id
  AND ml_2.linked_movie_id = t2_2.id
  AND lt_2.id = ml_2.link_type_id
  AND mk_2.movie_id = t1_2.id
  AND lt_1.link = lt_2.link
GROUP BY lt_1.link,
         lt_2.link,
         t1_1.title,
         t1_2.title,
         t2_1.title,
         t2_2.title;