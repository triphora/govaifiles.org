<script>
	export let result
</script>

<details style="margin:1em 0">
	<summary>
		{result.use_case_name}<br><br>
		{#if result.bureau_component}{result.bureau_component},&nbsp;{/if}{result.agency}
	</summary>

	<p><b>Internal ID:</b> {result.use_case_id}</p>

	{#if result.stage_of_development === 'Unknown'}
		{#if result.use_case_topic_area}
			<p><b>Topic Area:</b> {result.use_case_topic_area}</p>
		{:else}
			<p>No further information was provided.</p>
		{/if}
	{:else}
		<ul>
			<li>
				<b>Stage of Development:</b>
				<abbr title={result.stage_of_development_raw}>{result.stage_of_development}</abbr>
			</li>
			{#if result.is_high_impact}
				<li><b>Impact:</b> {result.is_high_impact}</li>
			{/if}
			{#if result.justification}
				<li><b>Impact Rating Justification:</b> {result.justification}</li>
			{/if}
			{#if result.use_case_topic_area}
				<li><b>Topic Area:</b> {result.use_case_topic_area}</li>
			{/if}
			{#if result.ai_classification}
				<li><b>Classification:</b> {result.ai_classification}</li>
			{/if}
			{#if result.problem_statement}
				<li><b>What problem is the AI intended to solve?:</b> {result.problem_statement}</li>
			{/if}
			{#if result.expected_benefits}
				<li><b>What are the expected benefits and positive outcomes from the AI for an agency's mission and/or the general public?:</b> {result.expected_benefits}</li>
			{/if}
			{#if result.system_outputs}
				<li><b>AI Outputs:</b> {result.system_outputs}</li>
			{/if}
			{#if result.operational_start_date}
				<li><b>Date when AI use case became operational or the pilot's start date:</b> {result.operational_start_date}</li>
			{/if}
			{#if result.development_source}
				<li>
					<b>Development Source:</b> {result.development_source}
					{#if result.vendor_name}
						<ul>
							<li><b>Vendor:</b> {result.vendor_name}</li>
						</ul>
					{/if}
				</li>
			{/if}
			{#if result.has_ato}
				<li><b>Does this AI use case have an associated Authorization to Operate (ATO)?:</b> {result.has_ato}</li>
			{/if}
			{#if result.systems_name}
				<li><b>System(s) Name:</b> {result.systems_name}</li>
			{/if}
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
		border: 2px solid #ccc;
		border-radius: 4px;
		padding: .5em .5em 0;
	}

	summary {
		font-weight: bold;
		margin: -.5em -.5em 0;
		padding: .5em;
		background-color: #ccc;
	}

	details[open] summary {
		border-bottom: 2px solid #ccc;
		margin-bottom: .5em;
	}
</style>