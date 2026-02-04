CREATE TABLE call_center (
    cc_call_center_sk integer,
    cc_call_center_id varchar,
    cc_rec_start_date date,
    cc_rec_end_date date,
    cc_closed_date_sk integer,
    cc_open_date_sk integer,
    cc_name varchar,
    cc_class varchar,
    cc_employees integer,
    cc_sq_ft integer,
    cc_hours varchar,
    cc_manager varchar,
    cc_mkt_id integer,
    cc_mkt_class varchar,
    cc_mkt_desc varchar,
    cc_market_manager varchar,
    cc_division integer,
    cc_division_name varchar,
    cc_company integer,
    cc_company_name varchar,
    cc_street_number varchar,
    cc_street_name varchar,
    cc_street_type varchar,
    cc_suite_number varchar,
    cc_city varchar,
    cc_county varchar,
    cc_state varchar,
    cc_zip varchar,
    cc_country varchar,
    cc_gmt_offset integer,
    cc_tax_percentage varchar,
    PRIMARY KEY (cc_call_center_sk)
) WITH (
        'cardinality' = '24'
);

CREATE TABLE catalog_page (
    cp_catalog_page_sk integer,
    cp_catalog_page_id varchar,
    cp_start_date_sk integer,
    cp_end_date_sk integer,
    cp_department varchar,
    cp_catalog_number integer,
    cp_catalog_page_number integer,
    cp_description varchar,
    cp_type varchar,
    PRIMARY KEY (cp_catalog_page_sk)
) WITH (
        'cardinality' = '12000'
);

CREATE TABLE catalog_returns (
    cr_returned_date_sk integer,
    cr_returned_time_sk integer,
    cr_item_sk integer,
    cr_refunded_customer_sk integer,
    cr_refunded_cdemo_sk integer,
    cr_refunded_hdemo_sk integer,
    cr_refunded_addr_sk integer,
    cr_returning_customer_sk integer,
    cr_returning_cdemo_sk integer,
    cr_returning_hdemo_sk integer,
    cr_returning_addr_sk integer,
    cr_call_center_sk integer,
    cr_catalog_page_sk integer,
    cr_ship_mode_sk integer,
    cr_warehouse_sk integer,
    cr_reason_sk integer,
    cr_order_number integer,
    cr_return_quantity integer,
    cr_return_amount decimal,
    cr_return_tax decimal,
    cr_return_amt_inc_tax decimal,
    cr_fee decimal,
    cr_return_ship_cost decimal,
    cr_refunded_cash decimal,
    cr_reversed_charge decimal,
    cr_store_credit decimal,
    cr_net_loss decimal,
    PRIMARY KEY (cr_item_sk, cr_order_number)
) WITH (
        'cardinality' = '2158260'
);

CREATE TABLE catalog_sales (
    cs_sold_date_sk integer,
    cs_sold_time_sk integer,
    cs_ship_date_sk integer,
    cs_bill_customer_sk integer,
    cs_bill_cdemo_sk integer,
    cs_bill_hdemo_sk integer,
    cs_bill_addr_sk integer,
    cs_ship_customer_sk integer,
    cs_ship_cdemo_sk integer,
    cs_ship_hdemo_sk integer,
    cs_ship_addr_sk integer,
    cs_call_center_sk integer,
    cs_catalog_page_sk integer,
    cs_ship_mode_sk integer,
    cs_warehouse_sk integer,
    cs_item_sk integer,
    cs_promo_sk integer,
    cs_order_number integer,
    cs_quantity integer,
    cs_wholesale_cost decimal,
    cs_list_price decimal,
    cs_sales_price decimal,
    cs_ext_discount_amt decimal,
    cs_ext_sales_price decimal,
    cs_ext_wholesale_cost decimal,
    cs_ext_list_price decimal,
    cs_ext_tax decimal,
    cs_coupon_amt decimal,
    cs_ext_ship_cost decimal,
    cs_net_paid decimal,
    cs_net_paid_inc_tax decimal,
    cs_net_paid_inc_ship decimal,
    cs_net_paid_inc_ship_tax decimal,
    cs_net_profit decimal,
    PRIMARY KEY (cs_item_sk, cs_order_number)
) WITH (
        'cardinality' = '14397492'
);

CREATE TABLE customer (
    c_customer_sk integer,
    c_customer_id varchar,
    c_current_cdemo_sk integer,
    c_current_hdemo_sk integer,
    c_current_addr_sk integer,
    c_first_shipto_date_sk integer,
    c_first_sales_date_sk integer,
    c_salutation varchar,
    c_first_name varchar,
    c_last_name varchar,
    c_preferred_cust_flag varchar,
    c_birth_day integer,
    c_birth_month integer,
    c_birth_year integer,
    c_birth_country varchar,
    c_login varchar,
    c_email_address varchar,
    c_last_review_date_sk integer,
    PRIMARY KEY (c_customer_sk)
) WITH (
        'cardinality' = '500000'
);

CREATE TABLE customer_address (
    ca_address_sk integer,
    ca_address_id varchar,
    ca_street_number varchar,
    ca_street_name varchar,
    ca_street_type varchar,
    ca_suite_number varchar,
    ca_city varchar,
    ca_county varchar,
    ca_state varchar,
    ca_zip varchar,
    ca_country varchar,
    ca_gmt_offset integer,
    ca_location_type varchar,
    PRIMARY KEY (ca_address_sk)
) WITH (
        'cardinality' = '250000'
);

CREATE TABLE customer_demographics (
    cd_demo_sk integer,
    cd_gender varchar,
    cd_marital_status varchar,
    cd_education_status varchar,
    cd_purchase_estimate integer,
    cd_credit_rating varchar,
    cd_dep_count integer,
    cd_dep_employed_count integer,
    cd_dep_college_count integer,
    PRIMARY KEY (cd_demo_sk)
) WITH (
        'cardinality' = '1920800'
);

CREATE TABLE date_dim (
    d_date_sk integer,
    d_date_id varchar,
    d_date date,
    d_month_seq integer,
    d_week_seq integer,
    d_quarter_seq integer,
    d_year integer,
    d_dow integer,
    d_moy integer,
    d_dom integer,
    d_qoy integer,
    d_fy_year integer,
    d_fy_quarter_seq integer,
    d_fy_week_seq integer,
    d_day_name varchar,
    d_quarter_name varchar,
    d_holiday varchar,
    d_weekend varchar,
    d_following_holiday varchar,
    d_first_dom integer,
    d_last_dom integer,
    d_same_day_ly integer,
    d_same_day_lq integer,
    d_current_day varchar,
    d_current_week varchar,
    d_current_month varchar,
    d_current_quarter varchar,
    d_current_year varchar,
    PRIMARY KEY (d_date_sk)
) WITH (
        'cardinality' = '73049'
);

CREATE TABLE dbgen_version (
    dv_version varchar,
    dv_create_date date,
    dv_create_time time,
    dv_cmdline_args varchar
) WITH (
        'cardinality' = '0'
);

CREATE TABLE household_demographics (
    hd_demo_sk integer,
    hd_income_band_sk integer,
    hd_buy_potential varchar,
    hd_dep_count integer,
    hd_vehicle_count integer,
    PRIMARY KEY (hd_demo_sk)
) WITH (
        'cardinality' = '7200'
);

CREATE TABLE income_band (
    ib_income_band_sk integer,
    ib_lower_bound integer,
    ib_upper_bound integer,
    PRIMARY KEY (ib_income_band_sk)
) WITH (
        'cardinality' = '20'
);

CREATE TABLE inventory (
    inv_date_sk integer,
    inv_item_sk integer,
    inv_warehouse_sk integer,
    inv_quantity_on_hand integer,
    PRIMARY KEY (inv_date_sk, inv_item_sk, inv_warehouse_sk)
) WITH (
        'cardinality' = '133110000'
);

CREATE TABLE item (
    i_item_sk integer,
    i_item_id varchar,
    i_rec_start_date date,
    i_rec_end_date date,
    i_item_desc varchar,
    i_current_price varchar,
    i_wholesale_cost varchar,
    i_brand_id integer,
    i_brand varchar,
    i_class_id integer,
    i_class varchar,
    i_category_id integer,
    i_category varchar,
    i_manufact_id integer,
    i_manufact varchar,
    i_size varchar,
    i_formulation varchar,
    i_color varchar,
    i_units varchar,
    i_container varchar,
    i_manager_id integer,
    i_product_name varchar,
    PRIMARY KEY (i_item_sk)
) WITH (
        'cardinality' = '102000'
);

CREATE TABLE promotion (
    p_promo_sk integer,
    p_promo_id varchar,
    p_start_date_sk integer,
    p_end_date_sk integer,
    p_item_sk integer,
    p_cost varchar,
    p_response_target integer,
    p_promo_name varchar,
    p_channel_dmail varchar,
    p_channel_email varchar,
    p_channel_catalog varchar,
    p_channel_tv varchar,
    p_channel_radio varchar,
    p_channel_press varchar,
    p_channel_event varchar,
    p_channel_demo varchar,
    p_channel_details varchar,
    p_purpose varchar,
    p_discount_active varchar,
    PRIMARY KEY (p_promo_sk)
) WITH (
        'cardinality' = '500'
);

CREATE TABLE reason (
    r_reason_sk integer,
    r_reason_id varchar,
    r_reason_desc varchar,
    PRIMARY KEY (r_reason_sk)
) WITH (
        'cardinality' = '45'
);

CREATE TABLE ship_mode (
    sm_ship_mode_sk integer,
    sm_ship_mode_id varchar,
    sm_type varchar,
    sm_code varchar,
    sm_carrier varchar,
    sm_contract varchar,
    PRIMARY KEY (sm_ship_mode_sk)
) WITH (
        'cardinality' = '20'
);

CREATE TABLE store (
    s_store_sk integer,
    s_store_id varchar,
    s_rec_start_date date,
    s_rec_end_date date,
    s_closed_date_sk integer,
    s_store_name varchar,
    s_number_employees integer,
    s_floor_space integer,
    s_hours varchar,
    s_manager varchar,
    s_market_id integer,
    s_geography_class varchar,
    s_market_desc varchar,
    s_market_manager varchar,
    s_division_id integer,
    s_division_name varchar,
    s_company_id integer,
    s_company_name varchar,
    s_street_number varchar,
    s_street_name varchar,
    s_street_type varchar,
    s_suite_number varchar,
    s_city varchar,
    s_county varchar,
    s_state varchar,
    s_zip varchar,
    s_country varchar,
    s_gmt_offset integer,
    s_tax_precentage varchar,
    PRIMARY KEY (s_store_sk)
) WITH (
        'cardinality' = '102'
);

CREATE TABLE store_returns (
    sr_returned_date_sk integer,
    sr_return_time_sk integer,
    sr_item_sk integer,
    sr_customer_sk integer,
    sr_cdemo_sk integer,
    sr_hdemo_sk integer,
    sr_addr_sk integer,
    sr_store_sk integer,
    sr_reason_sk integer,
    sr_ticket_number integer,
    sr_return_quantity integer,
    sr_return_amt decimal,
    sr_return_tax decimal,
    sr_return_amt_inc_tax decimal,
    sr_fee decimal,
    sr_return_ship_cost decimal,
    sr_refunded_cash decimal,
    sr_reversed_charge decimal,
    sr_store_credit decimal,
    sr_net_loss decimal,
    PRIMARY KEY (sr_item_sk, sr_ticket_number)
) WITH (
        'cardinality' = '7198194'
);

CREATE TABLE store_sales (
    ss_sold_date_sk integer,
    ss_sold_time_sk integer,
    ss_item_sk integer,
    ss_customer_sk integer,
    ss_cdemo_sk integer,
    ss_hdemo_sk integer,
    ss_addr_sk integer,
    ss_store_sk integer,
    ss_promo_sk integer,
    ss_ticket_number integer,
    ss_quantity integer,
    ss_wholesale_cost decimal,
    ss_list_price decimal,
    ss_sales_price decimal,
    ss_ext_discount_amt decimal,
    ss_ext_sales_price decimal,
    ss_ext_wholesale_cost decimal,
    ss_ext_list_price decimal,
    ss_ext_tax decimal,
    ss_coupon_amt decimal,
    ss_net_paid decimal,
    ss_net_paid_inc_tax decimal,
    ss_net_profit decimal,
    PRIMARY KEY (ss_item_sk, ss_ticket_number)
) WITH (
        'cardinality' = '28800991'
);

CREATE TABLE time_dim (
    t_time_sk integer,
    t_time_id varchar,
    t_time time,
    t_hour integer,
    t_minute integer,
    t_second integer,
    t_am_pm varchar,
    t_shift varchar,
    t_sub_shift varchar,
    t_meal_time time,
    PRIMARY KEY (t_time_sk)
) WITH (
        'cardinality' = '86400'
);

CREATE TABLE warehouse (
    w_warehouse_sk integer,
    w_warehouse_id varchar,
    w_warehouse_name varchar,
    w_warehouse_sq_ft integer,
    w_street_number varchar,
    w_street_name varchar,
    w_street_type varchar,
    w_suite_number varchar,
    w_city varchar,
    w_county varchar,
    w_state varchar,
    w_zip varchar,
    w_country varchar,
    w_gmt_offset integer,
    PRIMARY KEY (w_warehouse_sk)
) WITH (
        'cardinality' = '10'
);

CREATE TABLE web_page (
    wp_web_page_sk integer,
    wp_web_page_id varchar,
    wp_rec_start_date date,
    wp_rec_end_date date,
    wp_creation_date_sk integer,
    wp_access_date_sk integer,
    wp_autogen_flag varchar,
    wp_customer_sk integer,
    wp_url varchar,
    wp_type varchar,
    wp_char_count integer,
    wp_link_count integer,
    wp_image_count integer,
    wp_max_ad_count integer,
    PRIMARY KEY (wp_web_page_sk)
) WITH (
        'cardinality' = '200'
);

CREATE TABLE web_returns (
    wr_returned_date_sk integer,
    wr_returned_time_sk integer,
    wr_item_sk integer,
    wr_refunded_customer_sk integer,
    wr_refunded_cdemo_sk integer,
    wr_refunded_hdemo_sk integer,
    wr_refunded_addr_sk integer,
    wr_returning_customer_sk integer,
    wr_returning_cdemo_sk integer,
    wr_returning_hdemo_sk integer,
    wr_returning_addr_sk integer,
    wr_web_page_sk integer,
    wr_reason_sk integer,
    wr_order_number integer,
    wr_return_quantity integer,
    wr_return_amt decimal,
    wr_return_tax decimal,
    wr_return_amt_inc_tax decimal,
    wr_fee decimal,
    wr_return_ship_cost decimal,
    wr_refunded_cash decimal,
    wr_reversed_charge decimal,
    wr_account_credit decimal,
    wr_net_loss decimal,
    PRIMARY KEY (wr_item_sk, wr_order_number)
) WITH (
        'cardinality' = '1440354'
);

CREATE TABLE web_sales (
    ws_sold_date_sk integer,
    ws_sold_time_sk integer,
    ws_ship_date_sk integer,
    ws_item_sk integer,
    ws_bill_customer_sk integer,
    ws_bill_cdemo_sk integer,
    ws_bill_hdemo_sk integer,
    ws_bill_addr_sk integer,
    ws_ship_customer_sk integer,
    ws_ship_cdemo_sk integer,
    ws_ship_hdemo_sk integer,
    ws_ship_addr_sk integer,
    ws_web_page_sk integer,
    ws_web_site_sk integer,
    ws_ship_mode_sk integer,
    ws_warehouse_sk integer,
    ws_promo_sk integer,
    ws_order_number integer,
    ws_quantity integer,
    ws_wholesale_cost decimal,
    ws_list_price decimal,
    ws_sales_price decimal,
    ws_ext_discount_amt decimal,
    ws_ext_sales_price decimal,
    ws_ext_wholesale_cost decimal,
    ws_ext_list_price decimal,
    ws_ext_tax decimal,
    ws_coupon_amt decimal,
    ws_ext_ship_cost decimal,
    ws_net_paid decimal,
    ws_net_paid_inc_tax decimal,
    ws_net_paid_inc_ship decimal,
    ws_net_paid_inc_ship_tax decimal,
    ws_net_profit decimal,
    PRIMARY KEY (ws_item_sk, ws_order_number)
) WITH (
        'cardinality' = '7197566'
);

CREATE TABLE web_site (
    web_site_sk integer,
    web_site_id varchar,
    web_rec_start_date date,
    web_rec_end_date date,
    web_name varchar,
    web_open_date_sk integer,
    web_close_date_sk integer,
    web_class varchar,
    web_manager varchar,
    web_mkt_id integer,
    web_mkt_class varchar,
    web_mkt_desc varchar,
    web_market_manager varchar,
    web_company_id integer,
    web_company_name varchar,
    web_street_number varchar,
    web_street_name varchar,
    web_street_type varchar,
    web_suite_number varchar,
    web_city varchar,
    web_county varchar,
    web_state varchar,
    web_zip varchar,
    web_country varchar,
    web_gmt_offset integer,
    web_tax_percentage varchar,
    PRIMARY KEY (web_site_sk)
) WITH (
        'cardinality' = '42'
);
