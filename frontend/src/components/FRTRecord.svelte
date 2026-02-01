<script>
	export let result
</script>

<details style="margin:1em 0">
	<summary>
		{result.frt_instance_id || "No ID"}
		({result.canonical_agency}{#if result.canonical_sub_agency}, {result.canonical_sub_agency}{/if})
	</summary>
	<p>Present in:</p>
	<ul>
		{#if result.in_ai_inventory}<li>AI Use Case Inventory</li>{/if}
		{#if result.in_sorns}<li>Systems of Records Notices</li>{/if}
		{#if result.in_pra}<li>Paperwork Reduction Act Documents</li>{/if}
	</ul>

	<p>Details:</p>
	<ul>
		<li><b>Use Case Category:</b> {result.use_case_category}</li>
		<li><b>FRT Category:</b> {result.frt_category}</li>
		{#if result.ai_description}
			<li><b>AI Inventory Description:</b> {result.ai_description}</li>
		{/if}
		{#if result.sorn_description}
			<li><b>SORN Description:</b> {result.sorn_description}</li>
		{/if}
		{#if result.pra_description}
			<li><b>PRA Description:</b> {result.pra_description}</li>
		{/if}
		{#if result.ai_inventory_url}
			<li><b>AI Inventory URL:</b> <a href="{result.ai_inventory_url}">{result.ai_inventory_url}</a></li>
		{/if}
		{#if result.sorn_federal_register_url}
			<li><b>SORN Federal Register URL:</b> <a href="{result.sorn_federal_register_url}">{result.sorn_federal_register_url}</a></li>
		{/if}
		{#if result.pra_reginfo_url}
			<li><b>PRA RegInfo URL:</b> <a href="{result.pra_reginfo_url}">{result.pra_reginfo_url}</a></li>
		{/if}
	</ul>
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

	/*details[open] p {
		margin-bottom: .25em;
	}*/
</style>