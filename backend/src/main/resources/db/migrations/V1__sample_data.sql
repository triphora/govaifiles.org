CREATE TYPE public_reporting_status AS ENUM ('public', 'withheld', 'unknown');
CREATE TYPE stage_of_deployment AS ENUM ('Deployed', 'Retired', 'Pre-deployment', 'Pilot');
CREATE TYPE high_impact_status AS ENUM ('high_impact', 'not_high_impact', 'presumed_high_impact_not_high_impact');
CREATE TYPE rights_safety_impact_type AS ENUM ('both', 'neither', 'rights_impacting', 'safety_impacting');
CREATE TYPE development_source_type AS ENUM ('contract', 'in_house', 'vendor', 'mixed');
CREATE TYPE validation_status AS ENUM ('ok', 'warning', 'error');

CREATE TABLE ai_use_cases (
    use_case_id TEXT,
    use_case_name TEXT,
    agency TEXT,
    bureau_component TEXT,
    canonical_agency TEXT,
    canonical_abbreviation TEXT,
    canonical_sub_agency TEXT,
    agency_match_confidence TEXT,
    source_file TEXT,
    data_year TEXT,
    mapping_version TEXT,
    schema_version TEXT,
    contact_email TEXT,
    public_reporting_status public_reporting_status,
    stage_of_development stage_of_deployment,
    high_impact_status high_impact_status,
    rights_safety_impact_type rights_safety_impact_type,
    use_case_topic_area TEXT,
    ai_classification TEXT,
    purpose_and_benefits TEXT,
    expected_benefits TEXT,
    system_outputs TEXT,
    operational_start_date DATE,
    development_source_type development_source_type,
    vendor_names TEXT,
    piids TEXT,
    has_ato BOOL,
    system_names TEXT,
    training_data_description TEXT,
    federal_data_catalog_link TEXT,
    uses_pii BOOL,
    pia_link TEXT,
    demographic_variables_used TEXT,
    has_custom_code BOOL,
    open_source_code_link TEXT,
    pre_deployment_testing_conducted BOOL,
    ai_impact_assessment_completed BOOL,
    potential_impacts_identified TEXT,
    independent_review_conducted BOOL,
    ongoing_monitoring_established BOOL,
    operator_training_established BOOL,
    failsafe_in_place BOOL,
    appeal_process_available BOOL,
    user_feedback_steps TEXT,
    validation_status validation_status,
    validation_notes TEXT,
    needs_manual_review BOOL
);

COPY ai_use_cases FROM '/var/lib/postgresql/import/ai_inventory_2025_20260413_101402.csv' DELIMITER ',' CSV HEADER;
--COPY ai_use_cases FROM '/var/lib/postgresql/import/ai_inventory_multi_20260413_101408.csv' DELIMITER ',' CSV HEADER;