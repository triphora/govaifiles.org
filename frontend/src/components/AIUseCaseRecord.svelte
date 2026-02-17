<script>
	export let result

	const use_case_id = result.use_case_id || '[blank]'
	const stage_of_development = result.stage_of_development
	const stage_of_development_raw = result.stage_of_development_raw || '[blank]'
	const is_high_impact = result.is_high_impact || '[blank]'
	const justification = result.justification || '[blank]'
	const use_case_topic_area = result.use_case_topic_area || '[blank]'
	const ai_classification = result.ai_classification || '[blank]'
	const problem_statement = result.problem_statement || '[blank]'
	const expected_benefits = result.expected_benefits || '[blank]'
	const system_outputs = result.system_outputs || '[blank]'
	const operational_start_date = result.operational_start_date || '[blank]'
	const development_source = result.development_source || '[blank]'
	const vendor_name = result.vendor_name || '[blank]'
	const has_ato = result.has_ato || '[blank]'
	const systems_name = result.systems_name || '[blank]'
	const training_and_evaluation_data = result.training_and_evaluation_data || '[blank]'
	const federal_data_catalog_link = result.federal_data_catalog_link || '[blank]'
	const involves_pii = result.involves_pii || '[blank]'
	const pia_link = result.pia_link || '[blank]'
	const demographic_variables_used = result.demographic_variables_used || '[blank]'
	const includes_custom_code = result.includes_custom_code || '[blank]'
	const open_source_code_link = result.open_source_code_link || '[blank]'
	const pre_deployment_testing_status = result.pre_deployment_testing_status || '[blank]'
	const ai_impact_assessment_status = result.ai_impact_assessment_status || '[blank]'
	const potential_impacts_description = result.potential_impacts_description || '[blank]'
	const independent_review_status = result.independent_review_status || '[blank]'
	const ongoing_monitoring_process = result.ongoing_monitoring_process || '[blank]'
	const operator_training_status = result.operator_training_status || '[blank]'
	const fail_safe_status = result.fail_safe_status || '[blank]'
	const appeal_process_status = result.appeal_process_status || '[blank]'
	const public_and_user_feedback = result.public_and_user_feedback || '[blank]'

	const sectionTwoRequirements = stage_of_development !== 'Retired'
	const sectionThreeRequirements = ['Unknown', 'Pilot', 'Deployed'].includes(stage_of_development)
	const sectionFiveRequirements = ['Unknown', 'Deployed'].includes(stage_of_development) && ['High-impact', '[blank]'].includes(is_high_impact)
</script>

<details style="margin:1em 0">
	<summary>
		{result.use_case_name}<br><br>
		{#if result.bureau_component}{result.bureau_component},&nbsp;{/if}{result.agency}
	</summary>

	<!--Start of section 1-->
	<h4>Use Case Identifiers</h4>

	<p class:blank={use_case_id === '[blank]'}><b>Internal ID:</b> {use_case_id}</p>
	<p class:blank={stage_of_development === 'Unknown'}><b>Stage of Development:</b> <abbr title={stage_of_development_raw}>{stage_of_development}</abbr></p>
	<p class:blank={is_high_impact === '[blank]'}><b>Is the AI use case high-impact?:</b> {is_high_impact}</p>

	<!--Supposed to filled out if not high impact-->
	{#if result.is_high_impact === 'Not high-impact'}
		<p class="sub-1" class:blank={justification === '[blank]'}><b>Justification:</b> {justification}</p>
	{/if}

	<!--Start of section 2-->
	{#if sectionTwoRequirements}
		<br>
		<h4>Use Case Summary</h4>
	{/if}

	{#if sectionTwoRequirements || use_case_topic_area !== '[blank]'}
		<p class:blank={use_case_topic_area === '[blank]'}><b>Use Case Topic Area:</b> {use_case_topic_area}</p>
	{/if}
	{#if sectionTwoRequirements || ai_classification !== '[blank]'}
		<p class:blank={ai_classification === '[blank]'}><b>AI Classification:</b> {ai_classification}</p>
	{/if}
	{#if sectionTwoRequirements || problem_statement !== '[blank]'}
		<p class:blank={problem_statement === '[blank]'}><b>What problem is the AI intended to solve?:</b> {problem_statement}</p>
	{/if}
	{#if sectionTwoRequirements || expected_benefits !== '[blank]'}
		<p class:blank={expected_benefits === '[blank]'}><b>What are the expected benefits and positive outcomes from the AI for an agency's mission and/or the general public?:</b> {expected_benefits}</p>
	{/if}
	{#if sectionTwoRequirements || system_outputs !== '[blank]'}
		<p class:blank={system_outputs === '[blank]'}><b>Describe the AI system’s outputs:</b> {system_outputs}</p>
	{/if}

	<!--Start of section 3-->
	{#if sectionThreeRequirements}
		<br>
		<h4>Documentation</h4>
	{/if}

	{#if sectionThreeRequirements || operational_start_date !== '[blank]'}
		<p class:blank={operational_start_date === '[blank]'}><b>Date when AI use case became operational or the pilot's start date:</b> {operational_start_date}</p>
	{/if}
	{#if sectionThreeRequirements || development_source !== '[blank]'}
		<p class:blank={development_source === '[blank]'}><b>Was the system involved in this use case purchased from a vendor or developed under contract(s) or in-house?:</b> {development_source}</p>
	{/if}
	{#if sectionThreeRequirements || vendor_name !== '[blank]'}
		<p class="sub-1" class:blank={vendor_name === '[blank]'}><b>Vendor(s) Name:</b> {vendor_name}</p>
	{/if}
	{#if sectionThreeRequirements || has_ato !== '[blank]'}
		<p class:blank={has_ato === '[blank]'}><b>Does this AI use case have an associated Authorization to Operate (ATO)?:</b> {has_ato}</p>
	{/if}
	{#if sectionThreeRequirements || systems_name !== '[blank]'}
		<p class:blank={systems_name === '[blank]'}><b>System(s) Name:</b> {systems_name}</p>
	{/if}

	<!--Start of section 4-->
	{#if sectionThreeRequirements}
		<br>
		<h4>Data and Code</h4>
	{/if}

	{#if sectionThreeRequirements || training_and_evaluation_data !== '[blank]'}
		<p class:blank={training_and_evaluation_data === '[blank]'}><b>Describe any data used to train, fine-tune, and/or evaluate performance of the model(s) used in this use case:</b> {training_and_evaluation_data}</p>
	{/if}
	{#if sectionThreeRequirements || federal_data_catalog_link !== '[blank]'}
		<p class:blank={federal_data_catalog_link === '[blank]'}><b>If the data is required to be publicly disclosed as an open government data asset, provide a link to the entry on the Federal Data Catalog:</b> {federal_data_catalog_link}</p>
	{/if}
	{#if sectionThreeRequirements || involves_pii !== '[blank]'}
		<p class:blank={involves_pii === '[blank]'}><b>Does this AI use case involve personally identifiable information (PII) that is maintained by the agency?:</b> {involves_pii}</p>
	{/if}
	{#if sectionThreeRequirements || pia_link !== '[blank]'}
		<p class:blank={pia_link === '[blank]'}><b>If publicly available, provide the link to the AI use case's associated Privacy Impact Assessment (PIA):</b> {pia_link}</p>
	{/if}
	{#if sectionThreeRequirements || demographic_variables_used !== '[blank]'}
		<p class:blank={demographic_variables_used === '[blank]'}><b>Which, if any, demographic variables does the AI use case explicitly use as model features?:</b> {demographic_variables_used}</p>
	{/if}
	{#if sectionThreeRequirements || includes_custom_code !== '[blank]'}
		<p class:blank={includes_custom_code === '[blank]'}><b>Does this project include custom-developed code?:</b> {includes_custom_code}</p>
	{/if}
	{#if sectionThreeRequirements || open_source_code_link !== '[blank]'}
		<p class:blank={open_source_code_link === '[blank]'}><b>If the code is open source, provide the link for the publicly available source code:</b> {open_source_code_link}</p>
	{/if}

	<!--Start of section 5-->
	{#if sectionFiveRequirements}
		<br>
		<h4>Risk Management</h4>
	{/if}

	{#if sectionFiveRequirements || pre_deployment_testing_status !== '[blank]'}
		<p class:blank={pre_deployment_testing_status === '[blank]'}><b>Has pre-deployment testing been conducted?:</b> {pre_deployment_testing_status}</p>
	{/if}
	{#if sectionFiveRequirements || ai_impact_assessment_status !== '[blank]'}
		<p class:blank={ai_impact_assessment_status === '[blank]'}><b>Has an AI impact assessment been completed?:</b> {ai_impact_assessment_status}</p>
	{/if}
	{#if sectionFiveRequirements || potential_impacts_description !== '[blank]'}
		<p class:blank={potential_impacts_description === '[blank]'}><b>What are the potential impacts of using the AI for this particular use case and how were they identified?:</b> {potential_impacts_description}</p>
	{/if}
	{#if sectionFiveRequirements || independent_review_status !== '[blank]'}
		<p class:blank={independent_review_status === '[blank]'}><b>Has an independent review of the AI use case been conducted?:</b> {independent_review_status}</p>
	{/if}
	{#if sectionFiveRequirements || ongoing_monitoring_process !== '[blank]'}
		<p class:blank={ongoing_monitoring_process === '[blank]'}><b>Is there a process to conduct ongoing monitoring to identify any adverse impacts to the performance and security of the AI functionality, as well as to privacy, civil rights, and civil liberties?:</b> {ongoing_monitoring_process}</p>
	{/if}
	{#if sectionFiveRequirements || operator_training_status !== '[blank]'}
		<p class:blank={operator_training_status === '[blank]'}><b>Has the agency established sufficient and periodic training for operators of the AI to interpret and act on the its output and managed associated risks?:</b> {operator_training_status}</p>
	{/if}
	{#if sectionFiveRequirements || fail_safe_status !== '[blank]'}
		<p class:blank={fail_safe_status === '[blank]'}><b>Does this AI use case have an appropriate fail-safe that minimizes the risk of significant harm?:</b> {fail_safe_status}</p>
	{/if}
	{#if sectionFiveRequirements || appeal_process_status !== '[blank]'}
		<p class:blank={appeal_process_status === '[blank]'}><b>Is there an established appeal process in the event that an impacted individual would like to appeal or contest the AI system's outcome?:</b> {appeal_process_status}</p>
	{/if}
	{#if sectionFiveRequirements || public_and_user_feedback !== '[blank]'}
		<p class:blank={public_and_user_feedback === '[blank]'}><b>What steps has the agency taken to consult and incorporate feedback from end users of this AI use case and the public?:</b> {public_and_user_feedback}</p>
	{/if}
</details>


<style>
	details {
		border: 2px solid #ddd;
		border-radius: 4px;
		padding: .5em .5em 0;
	}

	summary {
		font-weight: bold;
		margin: -.5em -.5em 0;
		padding: .5em;
		background-color: #ddd;
	}

	details[open] summary {
		border-bottom: 2px solid #ddd;
		margin-bottom: .5em;
	}

	details > p {
		margin-bottom: .25em;
	}

	details > p:last-child {
		margin-bottom: .5em;
	}

	.sub-1 {
		margin-left: 2rem;
	}

	.blank {
		color: #EE0000;
	}
</style>