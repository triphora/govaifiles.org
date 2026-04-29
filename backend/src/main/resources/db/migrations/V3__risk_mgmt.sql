ALTER TABLE ai_use_cases ADD COLUMN risk_management_compliance_score INT DEFAULT 0;

UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE pre_deployment_testing_conducted = true;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE ai_impact_assessment_completed = true;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE independent_review_conducted = true;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE ongoing_monitoring_established = true;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE operator_training_established = true;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE failsafe_in_place = true;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE appeal_process_available = true;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE potential_impacts_identified IS NOT NULL;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE user_feedback_steps IS NOT NULL;

UPDATE ai_use_cases SET risk_management_compliance_score = -1
WHERE stage_of_development != 'Deployed' OR high_impact_status != 'high_impact';