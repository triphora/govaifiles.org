<script>
	import { PUBLIC_BACKEND_URL } from '$env/static/public';
	import AiUseCaseRecord from "../components/AIUseCaseRecord.svelte";

	let query = "";

	let filters = {
		category: null,
		agency: "",
		aiUseCase: false,
		sorns: false,
		praDocs: false,
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

		if (filters.category) {
			queryString += `use_case_category=${filters.category}&`
		}

		if (filters.agency) {
			queryString += `agency=${filters.agency}&`
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

<!--<div class="flexbox">
<fieldset class="flex-content">
	<legend>Use Case Category</legend>
	<div>
		<input type="radio" id="all" on:click={filters.category = null} checked />
		<label for="all">All</label>
	</div>
	<div>
		<input type="radio" id="law_enforcement" on:click={filters.category = "law_enforcement"} />
		<label for="law_enforcement">Law Enforcement</label>
	</div>
	<div>
		<input type="radio" id="immigration" on:click={filters.category = "immigration"} />
		<label for="immigration">Immigration</label>
	</div>
	<div>
		<input type="radio" id="benefits" on:click={filters.category = "benefits"} />
		<label for="benefits">Benefits</label>
	</div>
	<div>
		<input type="radio" id="transportation" on:click={filters.category = "transportation"} />
		<label for="transportation">Transportation</label>
	</div>
	<div>
		<input type="radio" id="internal" on:click={filters.category = "internal"} />
		<label for="internal">Internal</label>
	</div>
	<div>
		<input type="radio" id="health" on:click={filters.category = "health"} />
		<label for="health">Health</label>
	</div>
	<div>
		<input type="radio" id="other" on:click={filters.category = "other"} />
		<label for="other">Uncategorized</label>
	</div>
</fieldset>

<fieldset class="flex-content">
	<legend>Disclosure Patterns</legend>
	<div>
		<input type="checkbox" id="AIUseCase" bind:checked={filters.aiUseCase}/>
		<label for="AIUseCase">AI Use Case Inventory</label>
	</div>
	<div>
		<input type="checkbox" id="SORNs" bind:checked={filters.sorns} />
		<label for="SORNs">Systems of Records Notices</label>
	</div>
	<div>
		<input type="checkbox" id="PRA" bind:checked={filters.praDocs} />
		<label for="PRA">Paperwork Reduction Act</label>
	</div>
</fieldset>
</div>-->

<select bind:value={filters.agency} name="agency" id="agency-select">
	<option value="" selected>All Agencies</option>
	<option>Commodity Futures Trading Commission</option>
	<option>Department of Agriculture</option>
	<option>Department of Commerce</option>
	<option>Department of Energy</option>
	<option>Department of Health and Human Services</option>
	<option>Department of Homeland Security</option>
	<option>Department of Housing and Urban Development</option>
	<option>Department of Justice</option>
	<option>Department of Labor</option>
	<option>Department of State</option>
	<option>Department of the Interior</option>
	<option>Department of the Treasury</option>
	<option>Department of Transportation</option>
	<option>Department of Veterans Affairs</option>
	<option>Election Assistance Commission</option>
	<option>Equal Employment Opportunity Commission</option>
	<option>Federal Deposit Insurance Corporation</option>
	<option>Federal Energy Regulatory Commission</option>
	<option>Federal Housing Finance Agency</option>
	<option>Federal Reserve Board Of Governors</option>
	<option>National Aeronautics And Space Administration</option>
	<option>National Archives And Records Administration</option>
	<option>National Credit Union Administration</option>
	<option>Office of Personnel Management</option>
	<option>Pension Benefit Guaranty Corporation</option>
	<option>Securities and Exchange Commission</option>
	<option>Tennessee Valley Authority</option>
	<option>United States Trade And Development Agency</option>
</select>

<input type="text" bind:value={query} placeholder="Search..." />

<button on:click={search}>
	Search
</button>

{#if error}
	<p style="color: red;">{error}</p>
{/if}

{#if results}
	<p>{results.length}{#if results.length == 250}+{/if} results {#if results.length == 0}(check your query!){/if}</p>
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