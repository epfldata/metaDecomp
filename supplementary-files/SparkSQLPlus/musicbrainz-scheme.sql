CREATE TABLE alternative_medium (
    id integer,
    medium integer,
    alternative_release integer,
    name varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE alternative_medium_track (
    alternative_medium integer,
    track integer,
    alternative_track integer,
    PRIMARY KEY (alternative_medium)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE alternative_release (
    id integer,
    gid varchar,
    `release` integer,
    name varchar,
    artist_credit integer,
    type integer,
    `language` integer,
    script integer,
    `comment` varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE alternative_release_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '3'
);

CREATE TABLE alternative_track (
    id integer,
    name varchar,
    artist_credit integer,
    ref_count integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE annotation (
    id integer,
    editor integer,
    text varchar,
    changelog varchar,
    created varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE application (
    id integer,
    owner integer,
    name varchar,
    oauth_id varchar,
    oauth_secret varchar,
    oauth_redirect_uri varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE area (
    id integer,
    gid varchar,
    name varchar,
    type integer,
    edits_pending integer,
    last_updated varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    ended varchar,
    `comment` varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '119715'
);

CREATE TABLE area_alias (
    id integer,
    area integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '67737'
);

CREATE TABLE area_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '3'
);

CREATE TABLE area_annotation (
    area integer,
    annotation integer,
    PRIMARY KEY (area)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE area_attribute (
    id integer,
    area integer,
    area_attribute_type integer,
    area_attribute_type_allowed_value integer,
    area_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE area_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE area_attribute_type_allowed_value (
    id integer,
    area_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE area_containment (
    descendant integer,
    parent integer,
    depth integer,
    PRIMARY KEY (descendant)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE area_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '190'
);

CREATE TABLE area_tag (
    area integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (area)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE area_tag_raw (
    area integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (area)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE area_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '9'
);

CREATE TABLE artist (
    id integer,
    gid varchar,
    name varchar,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    type integer,
    area integer,
    gender integer,
    `comment` varchar,
    edits_pending integer,
    last_updated varchar,
    ended varchar,
    begin_area integer,
    end_area integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2691353'
);

CREATE TABLE artist_alias (
    id integer,
    artist integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '476446'
);

CREATE TABLE artist_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '3'
);

CREATE TABLE artist_annotation (
    artist integer,
    annotation integer,
    PRIMARY KEY (artist)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_attribute (
    id integer,
    artist integer,
    artist_attribute_type integer,
    artist_attribute_type_allowed_value integer,
    artist_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_attribute_type_allowed_value (
    id integer,
    artist_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_credit (
    id integer,
    name varchar,
    artist_count integer,
    ref_count integer,
    created varchar,
    edits_pending integer,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '3391404'
);

CREATE TABLE artist_credit_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '54771'
);

CREATE TABLE artist_credit_name (
    artist_credit integer,
    `position` integer,
    artist integer,
    name varchar,
    join_phrase varchar,
    PRIMARY KEY (artist_credit)
) WITH (
        'cardinality' = '6248051'
);

CREATE TABLE artist_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '82724'
);

CREATE TABLE artist_ipi (
    artist integer,
    ipi varchar,
    edits_pending integer,
    created varchar,
    PRIMARY KEY (artist)
) WITH (
        'cardinality' = '104072'
);

CREATE TABLE artist_isni (
    artist integer,
    isni varchar,
    edits_pending integer,
    created varchar,
    PRIMARY KEY (artist)
) WITH (
        'cardinality' = '136548'
);

CREATE TABLE artist_meta (
    id integer,
    rating integer,
    rating_count integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_rating_raw (
    artist integer,
    editor integer,
    rating integer,
    PRIMARY KEY (artist)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_release_group_nonva (
    is_track_artist varchar,
    artist integer,
    unofficial varchar,
    primary_type_child_order integer,
    primary_type integer,
    secondary_type_child_orders varchar,
    secondary_types varchar,
    first_release_date integer,
    name varchar,
    release_group integer,
    PRIMARY KEY (is_track_artist)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_release_group_pending_update (
    release_group integer,
    PRIMARY KEY (release_group)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_release_group_va (
    is_track_artist varchar,
    artist integer,
    unofficial varchar,
    primary_type_child_order integer,
    primary_type integer,
    secondary_type_child_orders varchar,
    secondary_types varchar,
    first_release_date integer,
    name varchar,
    release_group integer,
    PRIMARY KEY (is_track_artist)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_release_nonva (
    is_track_artist varchar,
    artist integer,
    first_release_date integer,
    catalog_numbers varchar,
    country_code varchar,
    barcode integer,
    name varchar,
    `release` integer,
    PRIMARY KEY (is_track_artist)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_release_pending_update (
    `release` integer,
    PRIMARY KEY (`release`)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_release_va (
    is_track_artist varchar,
    artist integer,
    first_release_date integer,
    catalog_numbers varchar,
    country_code varchar,
    barcode integer,
    name varchar,
    `release` integer,
    PRIMARY KEY (is_track_artist)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_series (
    artist integer,
    series integer,
    relationship integer,
    link_order integer,
    link integer,
    text_value varchar,
    PRIMARY KEY (artist)
) WITH (
        'cardinality' = '37441'
);

CREATE TABLE artist_tag (
    artist integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (artist)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_tag_raw (
    artist integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (artist)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE artist_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '6'
);

CREATE TABLE autoeditor_election (
    id integer,
    candidate integer,
    proposer integer,
    seconder_1 integer,
    seconder_2 integer,
    status integer,
    yes_votes integer,
    no_votes integer,
    propose_time varchar,
    open_time varchar,
    close_time varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE autoeditor_election_vote (
    id integer,
    autoeditor_election integer,
    voter integer,
    vote integer,
    vote_time varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE cdtoc (
    id integer,
    discid varchar,
    freedb_id varchar,
    track_count integer,
    leadout_offset integer,
    track_offset varchar,
    created varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1144278'
);

CREATE TABLE cdtoc_raw (
    id integer,
    `release` integer,
    discid varchar,
    track_count integer,
    leadout_offset integer,
    track_offset varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE country_area (
    area integer,
    PRIMARY KEY (area)
) WITH (
        'cardinality' = '258'
);

CREATE TABLE deleted_entity (
    gid varchar,
    data varchar,
    deleted_at varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit (
    id integer,
    editor integer,
    type integer,
    status integer,
    autoedit integer,
    open_time varchar,
    close_time varchar,
    expire_time varchar,
    `language` integer,
    quality integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_area (
    edit integer,
    area integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_artist (
    edit integer,
    artist integer,
    status integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_data (
    edit integer,
    data varchar,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_event (
    edit integer,
    event integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_genre (
    edit integer,
    genre integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_instrument (
    edit integer,
    instrument integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_label (
    edit integer,
    label integer,
    status integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_mood (
    edit integer,
    mood integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_note (
    id integer,
    editor integer,
    edit integer,
    text varchar,
    post_time varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_note_change (
    id integer,
    status varchar,
    edit_note integer,
    change_editor integer,
    change_time varchar,
    old_note varchar,
    new_note varchar,
    reason varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_note_recipient (
    recipient integer,
    edit_note integer,
    PRIMARY KEY (recipient)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_place (
    edit integer,
    place integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_recording (
    edit integer,
    recording integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_release (
    edit integer,
    `release` integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_release_group (
    edit integer,
    release_group integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_series (
    edit integer,
    series integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_url (
    edit integer,
    url integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE edit_work (
    edit integer,
    work integer,
    PRIMARY KEY (edit)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor (
    id integer,
    name varchar,
    privs integer,
    email varchar,
    website varchar,
    bio varchar,
    member_since varchar,
    email_confirm_date varchar,
    last_login_date varchar,
    last_updated varchar,
    birth_date varchar,
    gender integer,
    area integer,
    password varchar,
    ha1 varchar,
    deleted varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection (
    id integer,
    gid varchar,
    editor integer,
    name varchar,
    public varchar,
    description varchar,
    type integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_area (
    collection integer,
    area integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_artist (
    collection integer,
    artist integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_collaborator (
    collection integer,
    editor integer,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_deleted_entity (
    collection integer,
    gid varchar,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_event (
    collection integer,
    event integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_genre (
    collection integer,
    genre integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_instrument (
    collection integer,
    instrument integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_label (
    collection integer,
    label integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_place (
    collection integer,
    place integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_recording (
    collection integer,
    recording integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_release (
    collection integer,
    `release` integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_release_group (
    collection integer,
    release_group integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_series (
    collection integer,
    series integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_collection_type (
    id integer,
    name varchar,
    entity_type varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '17'
);

CREATE TABLE editor_collection_work (
    collection integer,
    work integer,
    added varchar,
    `position` integer,
    `comment` varchar,
    PRIMARY KEY (collection)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_language (
    editor integer,
    `language` integer,
    fluency varchar,
    PRIMARY KEY (editor)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_oauth_token (
    id integer,
    editor integer,
    application integer,
    authorization_code varchar,
    refresh_token varchar,
    access_token varchar,
    expire_time varchar,
    `scope` integer,
    granted varchar,
    code_challenge varchar,
    code_challenge_method varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_preference (
    id integer,
    editor integer,
    name varchar,
    `value` varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_subscribe_artist (
    id integer,
    editor integer,
    artist integer,
    last_edit_sent integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_subscribe_artist_deleted (
    editor integer,
    gid varchar,
    deleted_by integer,
    PRIMARY KEY (editor)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_subscribe_collection (
    id integer,
    editor integer,
    collection integer,
    last_edit_sent integer,
    available varchar,
    last_seen_name varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_subscribe_editor (
    id integer,
    editor integer,
    subscribed_editor integer,
    last_edit_sent integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_subscribe_label (
    id integer,
    editor integer,
    label integer,
    last_edit_sent integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_subscribe_label_deleted (
    editor integer,
    gid varchar,
    deleted_by integer,
    PRIMARY KEY (editor)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_subscribe_series (
    id integer,
    editor integer,
    series integer,
    last_edit_sent integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE editor_subscribe_series_deleted (
    editor integer,
    gid varchar,
    deleted_by integer,
    PRIMARY KEY (editor)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE event (
    id integer,
    gid varchar,
    name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    `time` varchar,
    type integer,
    cancelled varchar,
    setlist varchar,
    `comment` varchar,
    edits_pending integer,
    last_updated varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '104088'
);

CREATE TABLE event_alias (
    id integer,
    event integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2559'
);

CREATE TABLE event_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE event_annotation (
    event integer,
    annotation integer,
    PRIMARY KEY (event)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE event_attribute (
    id integer,
    event integer,
    event_attribute_type integer,
    event_attribute_type_allowed_value integer,
    event_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE event_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE event_attribute_type_allowed_value (
    id integer,
    event_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE event_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '788'
);

CREATE TABLE event_meta (
    id integer,
    rating integer,
    rating_count integer,
    event_art_presence varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE event_rating_raw (
    event integer,
    editor integer,
    rating integer,
    PRIMARY KEY (event)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE event_series (
    event integer,
    series integer,
    relationship integer,
    link_order integer,
    link integer,
    text_value varchar,
    PRIMARY KEY (event)
) WITH (
        'cardinality' = '52592'
);

CREATE TABLE event_tag (
    event integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (event)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE event_tag_raw (
    event integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (event)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE event_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '8'
);

CREATE TABLE gender (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '5'
);

CREATE TABLE genre (
    id integer,
    gid varchar,
    name varchar,
    `comment` varchar,
    edits_pending integer,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2069'
);

CREATE TABLE genre_alias (
    id integer,
    genre integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '920'
);

CREATE TABLE genre_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE genre_annotation (
    genre integer,
    annotation integer,
    PRIMARY KEY (genre)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE instrument (
    id integer,
    gid varchar,
    name varchar,
    type integer,
    edits_pending integer,
    last_updated varchar,
    `comment` varchar,
    description varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1054'
);

CREATE TABLE instrument_alias (
    id integer,
    instrument integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '11311'
);

CREATE TABLE instrument_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '3'
);

CREATE TABLE instrument_annotation (
    instrument integer,
    annotation integer,
    PRIMARY KEY (instrument)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE instrument_attribute (
    id integer,
    instrument integer,
    instrument_attribute_type integer,
    instrument_attribute_type_allowed_value integer,
    instrument_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE instrument_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE instrument_attribute_type_allowed_value (
    id integer,
    instrument_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE instrument_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '25'
);

CREATE TABLE instrument_tag (
    instrument integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (instrument)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE instrument_tag_raw (
    instrument integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (instrument)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE instrument_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '7'
);

CREATE TABLE iso_3166_1 (
    area integer,
    code varchar,
    PRIMARY KEY (area)
) WITH (
        'cardinality' = '258'
);

CREATE TABLE iso_3166_2 (
    area integer,
    code varchar,
    PRIMARY KEY (area)
) WITH (
        'cardinality' = '4839'
);

CREATE TABLE iso_3166_3 (
    area integer,
    code varchar,
    PRIMARY KEY (area)
) WITH (
        'cardinality' = '9'
);

CREATE TABLE isrc (
    id integer,
    recording integer,
    isrc varchar,
    source integer,
    edits_pending integer,
    created varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '4991512'
);

CREATE TABLE iswc (
    id integer,
    work integer,
    iswc varchar,
    source integer,
    edits_pending integer,
    created varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '475759'
);

CREATE TABLE l_area_area (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '119644'
);

CREATE TABLE l_area_artist (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_area_event (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '6661'
);

CREATE TABLE l_area_genre (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1245'
);

CREATE TABLE l_area_instrument (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '622'
);

CREATE TABLE l_area_label (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '155'
);

CREATE TABLE l_area_mood (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_area_place (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_area_recording (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '183325'
);

CREATE TABLE l_area_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '45210'
);

CREATE TABLE l_area_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_area_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '616'
);

CREATE TABLE l_area_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '268126'
);

CREATE TABLE l_area_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '7373'
);

CREATE TABLE l_artist_artist (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '777785'
);

CREATE TABLE l_artist_event (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '287385'
);

CREATE TABLE l_artist_genre (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1'
);

CREATE TABLE l_artist_instrument (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '100'
);

CREATE TABLE l_artist_label (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '59066'
);

CREATE TABLE l_artist_mood (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_artist_place (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '45625'
);

CREATE TABLE l_artist_recording (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '17162149'
);

CREATE TABLE l_artist_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1452044'
);

CREATE TABLE l_artist_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '15490'
);

CREATE TABLE l_artist_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '42126'
);

CREATE TABLE l_artist_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '5530584'
);

CREATE TABLE l_artist_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '4139151'
);

CREATE TABLE l_event_event (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '23367'
);

CREATE TABLE l_event_genre (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_event_instrument (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_event_label (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '751'
);

CREATE TABLE l_event_mood (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_event_place (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '92051'
);

CREATE TABLE l_event_recording (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '174561'
);

CREATE TABLE l_event_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '16783'
);

CREATE TABLE l_event_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '972'
);

CREATE TABLE l_event_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '52592'
);

CREATE TABLE l_event_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '71915'
);

CREATE TABLE l_event_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2848'
);

CREATE TABLE l_genre_genre (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '3283'
);

CREATE TABLE l_genre_instrument (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '238'
);

CREATE TABLE l_genre_label (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE l_genre_mood (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_genre_place (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1'
);

CREATE TABLE l_genre_recording (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_genre_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_genre_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '7'
);

CREATE TABLE l_genre_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_genre_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '4287'
);

CREATE TABLE l_genre_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_instrument_instrument (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1358'
);

CREATE TABLE l_instrument_label (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '5'
);

CREATE TABLE l_instrument_mood (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_instrument_place (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_instrument_recording (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_instrument_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_instrument_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_instrument_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_instrument_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1610'
);

CREATE TABLE l_instrument_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_label_label (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '31433'
);

CREATE TABLE l_label_mood (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_label_place (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '944'
);

CREATE TABLE l_label_recording (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '811378'
);

CREATE TABLE l_label_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '569819'
);

CREATE TABLE l_label_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '193'
);

CREATE TABLE l_label_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '6396'
);

CREATE TABLE l_label_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '350445'
);

CREATE TABLE l_label_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '554338'
);

CREATE TABLE l_mood_mood (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_mood_place (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_mood_recording (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_mood_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_mood_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_mood_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_mood_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_mood_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_place_place (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '5817'
);

CREATE TABLE l_place_recording (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2334985'
);

CREATE TABLE l_place_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '113821'
);

CREATE TABLE l_place_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_place_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '442'
);

CREATE TABLE l_place_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '102430'
);

CREATE TABLE l_place_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '4764'
);

CREATE TABLE l_recording_recording (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '300648'
);

CREATE TABLE l_recording_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '759'
);

CREATE TABLE l_recording_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_recording_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '34410'
);

CREATE TABLE l_recording_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1437955'
);

CREATE TABLE l_recording_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '7019297'
);

CREATE TABLE l_release_group_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '131758'
);

CREATE TABLE l_release_group_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '161453'
);

CREATE TABLE l_release_group_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '927557'
);

CREATE TABLE l_release_group_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '41'
);

CREATE TABLE l_release_release (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '31000'
);

CREATE TABLE l_release_release_group (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_release_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '57254'
);

CREATE TABLE l_release_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '8123041'
);

CREATE TABLE l_release_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_series_series (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '5421'
);

CREATE TABLE l_series_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '25102'
);

CREATE TABLE l_series_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '49052'
);

CREATE TABLE l_url_url (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE l_url_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '380954'
);

CREATE TABLE l_work_work (
    id integer,
    link integer,
    entity0 integer,
    entity1 integer,
    edits_pending integer,
    last_updated varchar,
    link_order integer,
    entity0_credit varchar,
    entity1_credit varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '458160'
);

CREATE TABLE label (
    id integer,
    gid varchar,
    name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    label_code integer,
    type integer,
    area integer,
    `comment` varchar,
    edits_pending integer,
    last_updated varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '315586'
);

CREATE TABLE label_alias (
    id integer,
    label integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '30436'
);

CREATE TABLE label_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE label_annotation (
    label integer,
    annotation integer,
    PRIMARY KEY (label)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE label_attribute (
    id integer,
    label integer,
    label_attribute_type integer,
    label_attribute_type_allowed_value integer,
    label_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE label_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE label_attribute_type_allowed_value (
    id integer,
    label_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE label_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '8223'
);

CREATE TABLE label_ipi (
    label integer,
    ipi varchar,
    edits_pending integer,
    created varchar,
    PRIMARY KEY (label)
) WITH (
        'cardinality' = '16075'
);

CREATE TABLE label_isni (
    label integer,
    isni varchar,
    edits_pending integer,
    created varchar,
    PRIMARY KEY (label)
) WITH (
        'cardinality' = '406'
);

CREATE TABLE label_meta (
    id integer,
    rating integer,
    rating_count integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE label_rating_raw (
    label integer,
    editor integer,
    rating integer,
    PRIMARY KEY (label)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE label_tag (
    label integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (label)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE label_tag_raw (
    label integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (label)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE label_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '11'
);

CREATE TABLE `language` (
    id integer,
    iso_code_2t varchar,
    iso_code_2b varchar,
    iso_code_1 varchar,
    name varchar,
    frequency integer,
    iso_code_3 varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '7844'
);

CREATE TABLE link (
    id integer,
    link_type integer,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    attribute_count integer,
    created varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1064287'
);

CREATE TABLE link_attribute (
    link integer,
    attribute_type integer,
    created varchar,
    PRIMARY KEY (link)
) WITH (
        'cardinality' = '855395'
);

CREATE TABLE link_attribute_credit (
    link integer,
    attribute_type integer,
    credited_as varchar,
    PRIMARY KEY (link)
) WITH (
        'cardinality' = '129872'
);

CREATE TABLE link_attribute_text_value (
    link integer,
    attribute_type integer,
    text_value varchar,
    PRIMARY KEY (link)
) WITH (
        'cardinality' = '85803'
);

CREATE TABLE link_attribute_type (
    id integer,
    parent integer,
    root integer,
    child_order integer,
    gid varchar,
    name varchar,
    description varchar,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1164'
);

CREATE TABLE link_creditable_attribute_type (
    attribute_type integer,
    PRIMARY KEY (attribute_type)
) WITH (
        'cardinality' = '1103'
);

CREATE TABLE link_text_attribute_type (
    attribute_type integer,
    PRIMARY KEY (attribute_type)
) WITH (
        'cardinality' = '4'
);

CREATE TABLE link_type (
    id integer,
    parent integer,
    child_order integer,
    gid varchar,
    entity_type0 varchar,
    entity_type1 varchar,
    name varchar,
    description varchar,
    link_phrase varchar,
    reverse_link_phrase varchar,
    long_link_phrase varchar,
    last_updated varchar,
    is_deprecated varchar,
    has_dates varchar,
    entity0_cardinality integer,
    entity1_cardinality integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '681'
);

CREATE TABLE link_type_attribute_type (
    link_type integer,
    attribute_type integer,
    `min` integer,
    `max` integer,
    last_updated varchar,
    PRIMARY KEY (link_type)
) WITH (
        'cardinality' = '525'
);

CREATE TABLE medium (
    id integer,
    `release` integer,
    `position` integer,
    format integer,
    name varchar,
    edits_pending integer,
    last_updated varchar,
    track_count integer,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '5491539'
);

CREATE TABLE medium_attribute (
    id integer,
    medium integer,
    medium_attribute_type integer,
    medium_attribute_type_allowed_value integer,
    medium_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE medium_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE medium_attribute_type_allowed_format (
    medium_format integer,
    medium_attribute_type integer,
    PRIMARY KEY (medium_format)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE medium_attribute_type_allowed_value (
    id integer,
    medium_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE medium_attribute_type_allowed_value_allowed_format (
    medium_format integer,
    medium_attribute_type_allowed_value integer,
    PRIMARY KEY (medium_format)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE medium_cdtoc (
    id integer,
    medium integer,
    cdtoc integer,
    edits_pending integer,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1337124'
);

CREATE TABLE medium_format (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    `year` integer,
    has_discids varchar,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '114'
);

CREATE TABLE medium_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '3175'
);

CREATE TABLE medium_index (
    medium integer,
    toc varchar,
    PRIMARY KEY (medium)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE medium_track_durations (
    medium integer,
    pregap_length varchar,
    cdtoc_track_lengths varchar,
    data_track_lengths varchar,
    PRIMARY KEY (medium)
) WITH (
        'cardinality' = '5483114'
);

CREATE TABLE mood (
    id integer,
    gid varchar,
    name varchar,
    `comment` varchar,
    edits_pending integer,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE mood_alias (
    id integer,
    mood integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE mood_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE mood_annotation (
    mood integer,
    annotation integer,
    PRIMARY KEY (mood)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE old_editor_name (
    name varchar,
    PRIMARY KEY (name)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE orderable_link_type (
    link_type integer,
    direction integer,
    PRIMARY KEY (link_type)
) WITH (
        'cardinality' = '12'
);

CREATE TABLE place (
    id integer,
    gid varchar,
    name varchar,
    type integer,
    address varchar,
    area integer,
    coordinates varchar,
    `comment` varchar,
    edits_pending integer,
    last_updated varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '74840'
);

CREATE TABLE place_alias (
    id integer,
    place integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '17255'
);

CREATE TABLE place_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE place_annotation (
    place integer,
    annotation integer,
    PRIMARY KEY (place)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE place_attribute (
    id integer,
    place integer,
    place_attribute_type integer,
    place_attribute_type_allowed_value integer,
    place_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE place_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE place_attribute_type_allowed_value (
    id integer,
    place_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE place_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '1364'
);

CREATE TABLE place_meta (
    id integer,
    rating integer,
    rating_count integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE place_rating_raw (
    place integer,
    editor integer,
    rating integer,
    PRIMARY KEY (place)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE place_tag (
    place integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (place)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE place_tag_raw (
    place integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (place)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE place_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '13'
);

CREATE TABLE recording (
    id integer,
    gid varchar,
    name varchar,
    artist_credit integer,
    length integer,
    `comment` varchar,
    edits_pending integer,
    last_updated varchar,
    video varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '36329654'
);

CREATE TABLE recording_alias (
    id integer,
    recording integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '887646'
);

CREATE TABLE recording_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE recording_annotation (
    recording integer,
    annotation integer,
    PRIMARY KEY (recording)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE recording_attribute (
    id integer,
    recording integer,
    recording_attribute_type integer,
    recording_attribute_type_allowed_value integer,
    recording_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE recording_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE recording_attribute_type_allowed_value (
    id integer,
    recording_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE recording_first_release_date (
    recording integer,
    `year` integer,
    `month` integer,
    `day` integer,
    PRIMARY KEY (recording)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE recording_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '4589959'
);

CREATE TABLE recording_meta (
    id integer,
    rating integer,
    rating_count integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE recording_rating_raw (
    recording integer,
    editor integer,
    rating integer,
    PRIMARY KEY (recording)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE recording_series (
    recording integer,
    series integer,
    relationship integer,
    link_order integer,
    link integer,
    text_value varchar,
    PRIMARY KEY (recording)
) WITH (
        'cardinality' = '33636'
);

CREATE TABLE recording_tag (
    recording integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (recording)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE recording_tag_raw (
    recording integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (recording)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE `release` (
    id integer,
    gid varchar,
    name varchar,
    artist_credit integer,
    release_group integer,
    status integer,
    packaging integer,
    `language` integer,
    script integer,
    barcode varchar,
    `comment` varchar,
    edits_pending integer,
    quality integer,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '4972532'
);

CREATE TABLE release_alias (
    id integer,
    `release` integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '45317'
);

CREATE TABLE release_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE release_annotation (
    `release` integer,
    annotation integer,
    PRIMARY KEY (`release`)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_attribute (
    id integer,
    `release` integer,
    release_attribute_type integer,
    release_attribute_type_allowed_value integer,
    release_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_attribute_type_allowed_value (
    id integer,
    release_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_country (
    `release` integer,
    country integer,
    date_year integer,
    date_month integer,
    date_day integer,
    PRIMARY KEY (`release`)
) WITH (
        'cardinality' = '12867114'
);

CREATE TABLE release_event (
    `release` integer,
    date_year integer,
    date_month integer,
    date_day integer,
    country integer,
    PRIMARY KEY (`release`)
) WITH (
        'cardinality' = '13261844'
);

CREATE TABLE release_first_release_date (
    `release` integer,
    `year` integer,
    `month` integer,
    `day` integer,
    PRIMARY KEY (`release`)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '180502'
);

CREATE TABLE release_group (
    id integer,
    gid varchar,
    name varchar,
    artist_credit integer,
    type integer,
    `comment` varchar,
    edits_pending integer,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '3878574'
);

CREATE TABLE release_group_alias (
    id integer,
    release_group integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '58567'
);

CREATE TABLE release_group_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE release_group_annotation (
    release_group integer,
    annotation integer,
    PRIMARY KEY (release_group)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_group_attribute (
    id integer,
    release_group integer,
    release_group_attribute_type integer,
    release_group_attribute_type_allowed_value integer,
    release_group_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_group_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_group_attribute_type_allowed_value (
    id integer,
    release_group_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_group_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '219454'
);

CREATE TABLE release_group_meta (
    id integer,
    release_count integer,
    first_release_date_year integer,
    first_release_date_month integer,
    first_release_date_day integer,
    rating integer,
    rating_count integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_group_primary_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '5'
);

CREATE TABLE release_group_rating_raw (
    release_group integer,
    editor integer,
    rating integer,
    PRIMARY KEY (release_group)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_group_secondary_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '12'
);

CREATE TABLE release_group_secondary_type_join (
    release_group integer,
    secondary_type integer,
    created varchar,
    PRIMARY KEY (release_group)
) WITH (
        'cardinality' = '876422'
);

CREATE TABLE release_group_series (
    release_group integer,
    series integer,
    relationship integer,
    link_order integer,
    link integer,
    text_value varchar,
    PRIMARY KEY (release_group)
) WITH (
        'cardinality' = '160397'
);

CREATE TABLE release_group_tag (
    release_group integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (release_group)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_group_tag_raw (
    release_group integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (release_group)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_label (
    id integer,
    `release` integer,
    label integer,
    catalog_number varchar,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '4274369'
);

CREATE TABLE release_meta (
    id integer,
    date_added varchar,
    info_url varchar,
    amazon_asin varchar,
    cover_art_presence varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_packaging (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '24'
);

CREATE TABLE release_raw (
    id integer,
    title varchar,
    artist varchar,
    added varchar,
    last_modified varchar,
    lookup_count integer,
    modify_count integer,
    source integer,
    barcode varchar,
    `comment` varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_series (
    `release` integer,
    series integer,
    relationship integer,
    link_order integer,
    link integer,
    text_value varchar,
    PRIMARY KEY (`release`)
) WITH (
        'cardinality' = '57254'
);

CREATE TABLE release_status (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '7'
);

CREATE TABLE release_tag (
    `release` integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (`release`)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_tag_raw (
    `release` integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (`release`)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE release_unknown_country (
    `release` integer,
    date_year integer,
    date_month integer,
    date_day integer,
    PRIMARY KEY (`release`)
) WITH (
        'cardinality' = '394730'
);

CREATE TABLE replication_control (
    id integer,
    current_schema_sequence integer,
    current_replication_sequence integer,
    last_replication_date varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1'
);

CREATE TABLE script (
    id integer,
    iso_code varchar,
    iso_number varchar,
    name varchar,
    frequency integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '159'
);

CREATE TABLE series (
    id integer,
    gid varchar,
    name varchar,
    `comment` varchar,
    type integer,
    ordering_type integer,
    edits_pending integer,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '30808'
);

CREATE TABLE series_alias (
    id integer,
    series integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2996'
);

CREATE TABLE series_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE series_annotation (
    series integer,
    annotation integer,
    PRIMARY KEY (series)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE series_attribute (
    id integer,
    series integer,
    series_attribute_type integer,
    series_attribute_type_allowed_value integer,
    series_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE series_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE series_attribute_type_allowed_value (
    id integer,
    series_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE series_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '210'
);

CREATE TABLE series_ordering_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE series_tag (
    series integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (series)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE series_tag_raw (
    series integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (series)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE series_type (
    id integer,
    name varchar,
    entity_type varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '17'
);

CREATE TABLE tag (
    id integer,
    name varchar,
    ref_count integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE tag_relation (
    tag1 integer,
    tag2 integer,
    weight integer,
    last_updated varchar,
    PRIMARY KEY (tag1)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE track (
    id integer,
    gid varchar,
    recording integer,
    medium integer,
    `position` integer,
    number varchar,
    name varchar,
    artist_credit integer,
    length integer,
    edits_pending integer,
    last_updated varchar,
    is_data_track varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '51932564'
);

CREATE TABLE track_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '793800'
);

CREATE TABLE track_raw (
    id integer,
    `release` integer,
    title varchar,
    artist varchar,
    sequence integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE unreferenced_row_log (
    table_name varchar,
    row_id integer,
    inserted varchar,
    PRIMARY KEY (table_name)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE url (
    id integer,
    gid varchar,
    url varchar,
    edits_pending integer,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '15835071'
);

CREATE TABLE url_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '35941'
);

CREATE TABLE vote (
    id integer,
    editor integer,
    edit integer,
    vote integer,
    vote_time varchar,
    superseded varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE work (
    id integer,
    gid varchar,
    name varchar,
    type integer,
    `comment` varchar,
    edits_pending integer,
    last_updated varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2528545'
);

CREATE TABLE work_alias (
    id integer,
    work integer,
    name varchar,
    locale varchar,
    edits_pending integer,
    last_updated varchar,
    type integer,
    sort_name varchar,
    begin_date_year integer,
    begin_date_month integer,
    begin_date_day integer,
    end_date_year integer,
    end_date_month integer,
    end_date_day integer,
    primary_for_locale varchar,
    ended varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '151432'
);

CREATE TABLE work_alias_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2'
);

CREATE TABLE work_annotation (
    work integer,
    annotation integer,
    PRIMARY KEY (work)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE work_attribute (
    id integer,
    work integer,
    work_attribute_type integer,
    work_attribute_type_allowed_value integer,
    work_attribute_text varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '2108797'
);

CREATE TABLE work_attribute_type (
    id integer,
    name varchar,
    `comment` varchar,
    free_text varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '85'
);

CREATE TABLE work_attribute_type_allowed_value (
    id integer,
    work_attribute_type integer,
    `value` varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '1062'
);

CREATE TABLE work_gid_redirect (
    gid varchar,
    new_id integer,
    created varchar,
    PRIMARY KEY (gid)
) WITH (
        'cardinality' = '154178'
);

CREATE TABLE work_language (
    work integer,
    `language` integer,
    edits_pending integer,
    created varchar,
    PRIMARY KEY (work)
) WITH (
        'cardinality' = '1634316'
);

CREATE TABLE work_meta (
    id integer,
    rating integer,
    rating_count integer,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE work_rating_raw (
    work integer,
    editor integer,
    rating integer,
    PRIMARY KEY (work)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE work_series (
    work integer,
    series integer,
    relationship integer,
    link_order integer,
    link integer,
    text_value varchar,
    PRIMARY KEY (work)
) WITH (
        'cardinality' = '48915'
);

CREATE TABLE work_tag (
    work integer,
    tag integer,
    `count` integer,
    last_updated varchar,
    PRIMARY KEY (work)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE work_tag_raw (
    work integer,
    editor integer,
    tag integer,
    is_upvote varchar,
    PRIMARY KEY (work)
) WITH (
        'cardinality' = '0'
);

CREATE TABLE work_type (
    id integer,
    name varchar,
    parent integer,
    child_order integer,
    description varchar,
    gid varchar,
    PRIMARY KEY (id)
) WITH (
        'cardinality' = '29'
);
