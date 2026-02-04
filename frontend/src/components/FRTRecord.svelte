<script>
	export let result
</script>

<details style="margin:1em 0">
	<summary>
		{result.use_case_name}<br><br>
		{#if result.bureau_component}{result.bureau_component},&nbsp;{/if}{result.agency}
	</summary>

	{#if result.stage_of_development === 'Unknown'}
		{#if result.use_case_topic_area}
			<p><b>Topic Area:</b> {result.use_case_topic_area}</p>
		{:else}
			<p>No further information was provided.</p>
		{/if}
	{:else}
		<p>Details:</p>
		<ul>
			<li><b>Topic Area:</b> {result.use_case_topic_area}</li>
			<li><b>Classification:</b> {result.ai_classification}</li>
			<li><b>Stage of Development:</b> {result.stage_of_development}</li>
			<li><b>Impact:</b> {result.is_high_impact}</li>
			<li><b>Expected Benefits:</b> {result.expected_benefits}</li>
			<li><b>System Outputs:</b> {result.system_outputs}</li>
			<li><b>Development Source:</b> {result.development_source}</li>
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