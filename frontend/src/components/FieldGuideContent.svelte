<script>
	import { fieldGuideGroups } from '$lib/explorer';

	export let showIntro = false;
</script>

{#if showIntro}
	<p class="field-guide-page__intro">
		The explorer uses shorter labels on the homepage, but the underlying data stays exactly the
		same. This guide shows how each simplified label maps back to the original reported field.
	</p>
{/if}

<div class="field-guide-note">
	Blank values mean an agency did not report a value. That is different from a reported <strong
		>No</strong
	>.
</div>

<div class="field-guide-groups">
	{#each fieldGuideGroups as group (group.title)}
		<section class="field-guide-group">
			<div class="field-guide-group__header">
				<p>{group.title}</p>
			</div>

			<div class="field-guide-table">
				{#each group.items as item (item.source)}
					<div class="field-guide-row">
						<div>
							<h2>{item.label}</h2>
							<code>{item.source}</code>
						</div>
						<p>{@html item.description.replaceAll('\n', '<br>')}</p>
					</div>
				{/each}
			</div>
		</section>
	{/each}
</div>

<style>
	.field-guide-page__intro {
		font-size: 1.02rem;
		color: var(--ink-soft);
	}

	.field-guide-note {
		padding: 18px 20px;
		border-radius: 18px;
		background: var(--surface-strong);
		border: 1px solid var(--line);
		line-height: 1.65;
		color: var(--ink-soft);
	}

	.field-guide-groups {
		display: grid;
		gap: 18px;
	}

	.field-guide-group {
		background: rgba(255, 255, 255, 0.76);
		border: 1px solid rgba(167, 190, 180, 0.55);
		border-radius: var(--radius-lg);
		overflow: hidden;
		box-shadow: var(--shadow-soft);
	}

	.field-guide-group__header {
		padding: 18px 22px 0;
	}

	.field-guide-group__header p {
		margin: 0;
		font-family: var(--font-mono);
		font-size: 0.72rem;
		letter-spacing: 0.16em;
		text-transform: uppercase;
		color: var(--ink-muted);
	}

	.field-guide-table {
		padding: 8px 22px 18px;
	}

	.field-guide-row {
		display: grid;
		grid-template-columns: minmax(180px, 240px) minmax(0, 1fr);
		gap: 18px;
		padding: 14px 0;
		border-top: 1px solid var(--line);
	}

	.field-guide-row h2 {
		margin: 0 0 4px;
		font-size: 1rem;
		font-family: var(--font-sans);
		font-weight: 700;
		letter-spacing: 0;
		text-transform: none;
		color: var(--ink);
	}

	.field-guide-row code {
		font-family: var(--font-mono);
		font-size: 0.76rem;
		color: var(--ink-muted);
	}

	.field-guide-row p {
		margin: 0;
		max-width: none;
		color: var(--ink-soft);
	}

	@media (max-width: 720px) {
		.field-guide-row {
			grid-template-columns: 1fr;
			gap: 8px;
		}
	}
</style>
