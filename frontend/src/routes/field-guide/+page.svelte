<script>
	import FieldGuideContent from '../../components/FieldGuideContent.svelte';
</script>

<section class="content-page field-guide-page">
	<p class="field-guide-page__kicker">Reference</p>
	<h1>Field Guide</h1>
	<p class="field-guide-page__intro">
		The explorer uses shorter labels on the homepage, but the underlying data stays exactly the
		same. This guide shows how each simplified label maps back to the original reported field.
	</p>

	<FieldGuideContent />
</section>

<style>
	.field-guide-page {
		display: grid;
		gap: 20px;
	}

	.field-guide-page__kicker {
		margin: 0;
		font-family: var(--font-mono);
		font-size: 0.72rem;
		letter-spacing: 0.16em;
		text-transform: uppercase;
		color: var(--ink-muted);
	}
</style>
