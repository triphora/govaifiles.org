ALTER TABLE ai_use_cases ADD COLUMN risk_management_compliance_score INT DEFAULT 0;

-- TODO These will need to be changed later to have more specific meanings than just "is answered"
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE pre_deployment_testing_conducted IS NOT NULL;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE ai_impact_assessment_completed IS NOT NULL;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE independent_review_conducted IS NOT NULL;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE ongoing_monitoring_established IS NOT NULL;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE operator_training_established IS NOT NULL;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE failsafe_in_place IS NOT NULL;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE appeal_process_available IS NOT NULL;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE potential_impacts_identified IS NOT NULL;
UPDATE ai_use_cases SET risk_management_compliance_score = risk_management_compliance_score + 1 WHERE user_feedback_steps IS NOT NULL;

UPDATE ai_use_cases SET risk_management_compliance_score = -1
WHERE stage_of_development != 'Deployed' OR high_impact_status != 'high_impact';