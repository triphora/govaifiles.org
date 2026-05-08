CREATE TYPE public_reporting_status AS ENUM ('public', 'withheld', 'withheld_disclosure_risk',
    'withheld_prohibited', 'withheld_other', 'partially_withheld', 'unknown');
CREATE TYPE stage_of_deployment AS ENUM ('Deployed', 'Retired', 'Pre-deployment', 'Pilot');
CREATE TYPE high_impact_status AS ENUM ('high_impact', 'not_high_impact', 'presumed_high_impact_not_high_impact');
CREATE TYPE rights_safety_impact_type AS ENUM ('both', 'neither', 'rights_impacting', 'safety_impacting');
CREATE TYPE development_source_type AS ENUM ('contract', 'in_house', 'vendor', 'mixed');
CREATE TYPE high_impact_field AS ENUM ('yes', 'not_applicable', 'in_progress', 'caio_waived', 'no');
-- TODO messaged Emma about - should not have `not_applicable`
CREATE TYPE independent_review_conducted AS ENUM ('yes', 'yes_internal', 'yes_oversight_board', 'yes_caio',
    'in_progress', 'caio_waived', 'no', 'not_applicable');
-- TODO messaged Emma about - should not have `yes_oversight_board`
CREATE TYPE ongoing_monitoring_established AS ENUM ('yes', 'yes_oversight_board', 'in_progress', 'caio_waived', 'no');
CREATE TYPE appeal_process_available AS ENUM ('yes', 'not_applicable', 'in_progress', 'precluded_by_law', 'caio_waived', 'no');
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
    hi_impact_justification TEXT,
    rights_safety_impact_type rights_safety_impact_type,
    agency_name TEXT,
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
    pre_deployment_testing_conducted high_impact_field,
    ai_impact_assessment_completed high_impact_field,
    potential_impacts_identified TEXT,
    independent_review_conducted independent_review_conducted,
    ongoing_monitoring_established ongoing_monitoring_established,
    operator_training_established high_impact_field,
    failsafe_in_place high_impact_field,
    appeal_process_available appeal_process_available,
    user_feedback_steps TEXT,
    validation_status validation_status,
    validation_notes TEXT,
    needs_manual_review BOOL
);

COPY ai_use_cases FROM '/var/lib/postgresql/import/ai_inventory_2025_20260430_142018.csv' DELIMITER ',' CSV HEADER;
COPY ai_use_cases FROM '/var/lib/postgresql/import/ai_inventory_2024_20260505_100358.csv' DELIMITER ',' CSV HEADER;