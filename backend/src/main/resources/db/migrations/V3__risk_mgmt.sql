ALTER TABLE ai_use_cases ADD COLUMN risk_management_compliance_score INT DEFAULT 0;

UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE pre_deployment_testing_conducted IN ('yes', 'not_applicable');
UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE ai_impact_assessment_completed IN ('yes', 'not_applicable');
UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE independent_review_conducted IN ('yes', 'yes_internal', 'yes_oversight_board', 'yes_caio', 'caio_waived', 'not_applicable');
UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE ongoing_monitoring_established IN ('yes', 'yes_oversight_board', 'caio_waived');
UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE operator_training_established IN ('yes', 'not_applicable');
UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE failsafe_in_place IN ('yes', 'not_applicable');
UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE appeal_process_available in ('yes', 'not_applicable', 'precluded_by_law', 'caio_waived');
UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE potential_impacts_identified !~ '[Pp]rogress';
UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE user_feedback_steps !~ 'progress';

UPDATE ai_use_cases SET risk_management_compliance_score = -1
WHERE stage_of_development != 'Deployed' OR high_impact_status != 'high_impact' OR data_year = '2024';