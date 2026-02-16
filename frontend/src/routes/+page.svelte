<script>
	import { PUBLIC_BACKEND_URL } from '$env/static/public';
	import AiUseCaseRecord from "../components/AIUseCaseRecord.svelte";

	let query = "";

	let filters = {
		agency: "",
		stage: "",
		impact: "",
	}

	let results = null;
	let error = null;

	$: if (query || filters) {
		search();
	} else {
		results = null;
		error = null;
	}

	async function search() {
		let queryString = `${PUBLIC_BACKEND_URL}/ai-use-case-2025/${query || "*"}?`

		if (filters.agency) {
			queryString += `agency=${filters.agency}&`
		}

		if (filters.stage) {
			queryString += `stage_of_development=${filters.stage}&`
		}

		if (filters.impact) {
			queryString += `is_high_impact==${filters.impact}&`
		}

		try {
			const res = await fetch(queryString);
			if (!res.ok) {
				throw new Error(`HTTP ${res.status}`);
			}
			results = await res.json();
		} catch (e) {
			error = e.message;
		}
	}
</script>

<h1>AI Use Case Inventory, 2025</h1>

<p>A dashboard to explore the documents published by the United States Federal Government detailing usage of Artificial Intelligence. <a href={"/about"}>Learn more </a></p>

<div class="flexbox">
<fieldset class="flex-content">
	<legend>Stage of Development</legend>
	<div>
		<input name="stage" type="radio" id="anystage" on:click={filters.stage = ""} checked />
		<label for="anystage">Any stage</label>
	</div>
	<div>
		<input name="stage" type="radio" id="predeployment" on:click={filters.stage = "Pre-deployment"} />
		<label for="predeployment">Pre-deployment</label>
	</div>
	<div>
		<input name="stage" type="radio" id="pilot" on:click={filters.stage = "Pilot"} />
		<label for="pilot">Pilot</label>
	</div>
	<div>
		<input name="stage" type="radio" id="deployed" on:click={filters.stage = "Deployed"} />
		<label for="deployed">Deployed</label>
	</div>
	<div>
		<input name="stage" type="radio" id="retired" on:click={filters.stage = "Retired"} />
		<label for="retired">Retired</label>
	</div>
</fieldset>

<fieldset class="flex-content">
	<legend>Impact</legend>
	<div>
		<input name="impact" type="radio" id="anyimpact" on:click={filters.impact = ""} checked />
		<label for="anyimpact">Any impact</label>
	</div>
	<div>
		<input name="impact" type="radio" id="high" on:click={filters.impact = "High-impact"} />
		<label for="high">High impact</label>
	</div>
	<div>
		<input name="impact" type="radio" id="nothigh" on:click={filters.impact = "Not high-impact"} />
		<label for="nothigh">Not high-impact</label>
	</div>
</fieldset>
</div>

<select bind:value={filters.agency} name="agency" id="agency-select">
	<option value="" selected>All Agencies</option>
	<option value="Department of">All Cabinet Agencies</option>
	<option>&nbsp;&nbsp;Department of Agriculture</option>
	<option>&nbsp;&nbsp;Department of Commerce</option>
	<option>&nbsp;&nbsp;Department of Defense</option>
	<option>&nbsp;&nbsp;Department of Education</option>
	<option>&nbsp;&nbsp;Department of Energy</option>
	<option>&nbsp;&nbsp;Department of Health and Human Services</option>
	<option>&nbsp;&nbsp;Department of Homeland Security</option>
	<option>&nbsp;&nbsp;Department of Housing and Urban Development</option>
	<option>&nbsp;&nbsp;Department of Justice</option>
	<option>&nbsp;&nbsp;Department of Labor</option>
	<option>&nbsp;&nbsp;Department of State</option>
	<option>&nbsp;&nbsp;Department of the Interior</option>
	<option>&nbsp;&nbsp;Department of the Treasury</option>
	<option>&nbsp;&nbsp;Department of Transportation</option>
	<option>&nbsp;&nbsp;Department of Veterans Affairs</option>
	<option>Commodity Futures Trading Commission</option>
	<option>Consumer Financial Protection Bureau</option>
	<option>Election Assistance Commission</option>
	<option>Environmental Protection Agency</option>
	<option>Equal Employment Opportunity Commission</option>
	<option>Federal Deposit Insurance Corporation</option>
	<option>Federal Energy Regulatory Commission</option>
	<option>Federal Housing Finance Agency</option>
	<option>Federal Reserve Board of Governors</option>
	<option>Federal Trade Commission</option>
	<option>General Services Administration</option>
	<option>National Aeronautics and Space Administration</option>
	<option>National Science Foundation</option>
	<option>National Archives and Records Administration</option>
	<option>National Credit Union Administration</option>
	<option>Office of Personnel Management</option>
	<option>National Transportation Safety Board</option>
	<option>Pension Benefit Guaranty Corporation</option>
	<option>Securities and Exchange Commission</option>
	<option>Social Security Administration</option>
	<option>Tennessee Valley Authority</option>
	<option>United States Agency for International Development</option>
	<option>United States Trade and Development Agency</option>
</select>

<input type="text" bind:value={query} placeholder="Search..." />

<button on:click={search}>
	Search
</button>

{#if error}
	<p style="color: red;">{error}</p>
{/if}

{#if results}
	<p>{results.length}{#if results.length === 250}+{/if} results</p>

	{#if results.length === 0}
		{#if filters.agency.trim() === 'Department of Defense'}
			<p>The Department of Defense is exempt from submitting an AI Use Case Inventory for intelligence reasons.</p>
		{:else if filters.agency.trim() === 'Consumer Financial Protection Bureau'}
			<p>The Consumer Financial Protection Bureau has no reported AI use cases in 2025.</p>
		{:else if filters.agency.trim() === 'National Transportation Safety Board'}
			<p>The National Transportation Safety Board has no reported AI use cases in 2025.</p>
		{:else if filters.agency.trim() === 'Social Security Administration'}
			<p>The Social Security Administration has not yet published its AI Use Case Inventory (as of 2/13/26).</p>
		{:else if filters.agency.trim() === 'United States Agency for International Development'}
			<p>The United States Agency for International Development has not yet published its AI Use Case Inventory (as of 2/13/26). The agency is defunct as of February 2025.</p>
		{:else if filters.agency.trim() === 'Postal Regulatory Commission'}
			<p>The Postal Regulatory Commission has not yet published its AI Use Case Inventory (as of 2/13/26).</p>
		{:else if filters.agency.trim() === 'Presidio Trust'}
			<p>The Presidio Trust has not yet published its AI Use Case Inventory (as of 2/13/26).</p>
		{:else if filters.agency.trim() === 'United States Agency for Global Media'}
			<p>The United States Agency for Global Media has not yet published its AI Use Case Inventory (as of 2/13/26).</p>
		{:else if filters.agency.trim() === 'United States Commission on Civil Rights'}
			<p>The United States Commission on Civil Rights has not yet published its AI Use Case Inventory (as of 2/13/26).</p>
		{:else}
			<p>If you think you should be getting results, check your query and filters!</p>
		{/if}
	{/if}
{/if}

{#each results as result (result.id)}
	<AiUseCaseRecord {result} />
{/each}

<style>
	.flexbox {
		display: flex;
		flex-wrap: wrap;
		gap: 1em;
	}

	.flex-content {
		flex: 1 1 calc(50% - 1em);
		box-sizing: border-box;
	}

	fieldset {
		margin-bottom: 1em;
	}
</style>