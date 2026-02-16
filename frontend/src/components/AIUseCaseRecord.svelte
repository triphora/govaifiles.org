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

	const sectionTwoRequirements = result.stage_of_development !== 'Retired'
	const sectionThreeRequirements = ['Unknown', 'Pilot', 'Deployed'].includes(result.stage_of_development)
</script>

<details style="margin:1em 0">
	<summary>
		{result.use_case_name}<br><br>
		{#if result.bureau_component}{result.bureau_component},&nbsp;{/if}{result.agency}
	</summary>

	<h4>Use Case Identifiers</h4>

	<!--Supposed to always be filled out-->
	<p class:blank={use_case_id === '[blank]'}>
		<b>Internal ID:</b> {use_case_id}
	</p>

	<!--Supposed to always be filled out-->
	<p class:blank={stage_of_development === 'Unknown'}>
		<b>Stage of Development:</b>
		<abbr title={stage_of_development_raw}>{stage_of_development}</abbr>
	</p>

	<!--Supposed to always be filled out-->
	<p class:blank={is_high_impact === '[blank]'}>
		<b>Is the AI use case high-impact?:</b> {is_high_impact}
	</p>

	<!--Supposed to filled out if not high impact-->
	{#if result.is_high_impact === 'Not high-impact'}
		<p class="sub-1" class:blank={justification === '[blank]'}>
			<b>Justification:</b> {justification}
		</p>
	{/if}

	<!--Start of section 2-->
	{#if sectionTwoRequirements}
		<br>
		<h4>Use Case Summary</h4>
	{/if}

	{#if sectionTwoRequirements || use_case_topic_area !== '[blank]'}
		<p class:blank={use_case_topic_area === '[blank]'}><b>Use Case Topic Area:</b> {result.use_case_topic_area || '[blank]'}</p>
	{/if}
	{#if sectionTwoRequirements || ai_classification !== '[blank]'}
		<p class:blank={ai_classification === '[blank]'}><b>AI Classification:</b> {result.ai_classification || '[blank]'}</p>
	{/if}
	{#if sectionTwoRequirements || problem_statement !== '[blank]'}
		<p class:blank={problem_statement === '[blank]'}><b>What problem is the AI intended to solve?:</b> {result.problem_statement || '[blank]'}</p>
	{/if}
	{#if sectionTwoRequirements || expected_benefits !== '[blank]'}
		<p class:blank={expected_benefits === '[blank]'}><b>What are the expected benefits and positive outcomes from the AI for an agency's mission and/or the general public?:</b> {result.expected_benefits || '[blank]'}</p>
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
		<!--TODO unfinished-->
	{/if}

	{#if result.stage_of_development !== 'Unknown'}
		<ul>
			{#if result.training_and_evaluation_data}
				<li><b>Describe any data used to train, fine-tune, and/or evaluate performance of the model(s) used in this use case:</b> {result.training_and_evaluation_data}</li>
			{/if}
			{#if result.federal_data_catalog_link}
				<li><b>If the data is required to be publicly disclosed as an open government data asset, provide a link to the entry on the Federal Data Catalog:</b> {result.federal_data_catalog_link}</li>
			{/if}
			{#if result.involves_pii}
				<li><b>Does this AI use case involve personally identifiable information (PII) that is maintained by the agency?:</b> {result.involves_pii}</li>
			{/if}
			{#if result.pia_link}
				<li><b>If publicly available, provide the link to the AI use case's associated Privacy Impact Assessment (PIA):</b>
					{#if result.pia_link.substring(0,4)==='http'}
						<a href={result.pia_link} target="_blank">{result.pia_link}</a>
					{:else}
						{result.pia_link}
					{/if}
				</li>
			{/if}
			{#if result.demographic_variables_used}
				<li><b>Which, if any, demographic variables does the AI use case explicitly use as model features?:</b> {result.demographic_variables_used}</li>
			{/if}
			{#if result.includes_custom_code}
				<li><b>Does this project include custom-developed code?:</b> {result.includes_custom_code}</li>
			{/if}
			{#if result.open_source_code_link}
				<li><b>If the code is open source, provide the link for the publicly available source code:</b>
					{#if result.open_source_code_link.substring(0,4)==='http'}
						<a href={result.open_source_code_link} target="_blank">{result.open_source_code_link}</a>
						{:else}
						{result.open_source_code_link}
					{/if}
				</li>
			{/if}
			{#if result.pre_deployment_testing_status}
				<li><b>Has pre-deployment testing been conducted?:</b> {result.pre_deployment_testing_status}</li>
			{/if}
			{#if result.ai_impact_assessment_status}
				<li><b>Has an AI impact assessment been completed?:</b>
					{#if result.ai_impact_assessment_status.substring(0,4)==='http'}
						<a href={result.ai_impact_assessment_status} target="_blank">{result.ai_impact_assessment_status}</a>
					{:else}
						{result.ai_impact_assessment_status}
					{/if}
				</li>
			{/if}
			{#if result.potential_impacts_description}
				<li><b>What are the potential impacts of using the AI for this particular use case and how were they identified?:</b> {result.potential_impacts_description}</li>
			{/if}
			{#if result.independent_review_status}
				<li><b>Has an independent review of the AI use case been conducted?:</b> {result.independent_review_status}</li>
			{/if}
			{#if result.ongoing_monitoring_process}
				<li><b>Is there a process to conduct ongoing monitoring to identify any adverse impacts to the performance and security of the AI functionality, as well as to privacy, civil rights, and civil liberties?:</b> {result.ongoing_monitoring_process}</li>
			{/if}
			{#if result.operator_training_status}
				<li><b>Has the agency established sufficient and periodic training for operators of the AI to interpret and act on the its output and managed associated risks?:</b> {result.operator_training_status}</li>
			{/if}
			{#if result.fail_safe_status}
				<li><b>Does this AI use case have an appropriate fail-safe that minimizes the risk of significant harm?:</b> {result.fail_safe_status}</li>
			{/if}
			{#if result.appeal_process_status}
				<li><b>Is there an established appeal process in the event that an impacted individual would like to appeal or contest the AI system's outcome?:</b> {result.appeal_process_status}</li>
			{/if}
			{#if result.public_and_user_feedback}
				<li><b>What steps has the agency taken to consult and incorporate feedback from end users of this AI use case and the public?:</b> {result.public_and_user_feedback}</li>
			{/if}
		</ul>
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
		margin-bottom: 0;
	}

	details > p:last-child {
		margin-bottom: .5em;
	}

	.sub-1 {
		margin-left: 1rem;
	}

	.blank {
		color: #EE0000;
	}
</style>