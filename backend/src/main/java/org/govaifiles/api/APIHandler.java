package org.govaifiles.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.NotImplementedResponse;
import org.jetbrains.annotations.NotNull;
import org.typesense.api.Client;
import org.typesense.api.Configuration;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;
import org.typesense.resources.Node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class APIHandler implements CrudHandler {
	private static final String BLANK_IMPACT_VALUE = "__blank__";
	private static final String DATA_YEAR_FILTER_FIELD = "data_year_filter";
	private static final Gson GSON = new Gson();
	private static final Map<String, String> FILTER_FIELDS = Map.of(
		"agency", "agency",
		"bureau", "bureau_component",
		"stage", "stage_of_development",
		"impact", "impact_filter",
		"topic", "use_case_topic_area",
		"aiClassification", "ai_classification",
		"compliance", "compliance_status"
	);
	private static final Map<String, String> AGENCY_ALIASES = Map.of(
		"Board of Governors of the Federal Reserve System", "Federal Reserve Board of Governors",
		"U.S. Agency for Global Media", "United States Agency for Global Media",
		"U.S. Agency for International Development", "United States Agency for International Development",
		"U.S. Commission on Civil Rights", "United States Commission on Civil Rights",
		"U.S. Election Assistance Commission", "Election Assistance Commission"
	);
	private static final List<String> COMPLIANCE_OPTIONS = List.of(
		"In compliance",
		"Not in compliance",
		"Not required"
	);
	private static final Pattern AGENTIC_AI_PATTERN = Pattern.compile("^agentic ai", Pattern.CASE_INSENSITIVE);
	private static final Pattern AGENTIC_AI_HYPHEN_PATTERN = Pattern.compile("^agentic-ai", Pattern.CASE_INSENSITIVE);
	private static final Pattern CLASSICAL_AI_PATTERN = Pattern.compile("^classical/predictive machine learning", Pattern.CASE_INSENSITIVE);
	private static final Pattern COMPUTER_VISION_PATTERN = Pattern.compile("^computer vision", Pattern.CASE_INSENSITIVE);
	private static final Pattern GENERATIVE_AI_PATTERN = Pattern.compile("^generative ai", Pattern.CASE_INSENSITIVE);
	private static final Pattern NLP_PATTERN = Pattern.compile("^(natural language processing|nlp)", Pattern.CASE_INSENSITIVE);
	private static final Pattern REINFORCEMENT_PATTERN = Pattern.compile("^reinforcement learning", Pattern.CASE_INSENSITIVE);
	private static final Pattern OTHER_PATTERN = Pattern.compile("^other", Pattern.CASE_INSENSITIVE);
	private static final FilterOptionConfig FILTER_OPTION_CONFIG = loadFilterOptionConfig();

	private static class FilterOptionConfig {
		private final Map<String, LinkedHashMap<String, Integer>> facetTemplates;
		private final Map<String, Map<String, List<String>>> requestValueMappings;

		private FilterOptionConfig(
			Map<String, LinkedHashMap<String, Integer>> facetTemplates,
			Map<String, Map<String, List<String>>> requestValueMappings
		) {
			this.facetTemplates = facetTemplates;
			this.requestValueMappings = requestValueMappings;
		}
	}

	private static class SearchResponse {
		private final List<JsonObject> hits;
		private final Map<String, Map<String, Integer>> facets;
		private final Integer found;

		private SearchResponse(List<JsonObject> hits, Map<String, Map<String, Integer>> facets, Integer found) {
			this.hits = hits;
			this.facets = facets;
			this.found = found;
		}
	}

	private static Path resolveFilterOptionsPath() {
		List<Path> candidates = List.of(
			Path.of("frontend", "GOVAI_Filter_Options.csv"),
			Path.of("..", "frontend", "GOVAI_Filter_Options.csv")
		);

		for (Path candidate : candidates) {
			if (Files.exists(candidate)) {
				return candidate;
			}
		}

		throw new IllegalStateException("Could not locate frontend/GOVAI_Filter_Options.csv");
	}

	private static List<String> parseCsvRow(String row) {
		List<String> values = new ArrayList<>();
		StringBuilder currentValue = new StringBuilder();
		boolean inQuotes = false;

		for (int index = 0; index < row.length(); index += 1) {
			char character = row.charAt(index);

			if (character == '"') {
				if (inQuotes && index + 1 < row.length() && row.charAt(index + 1) == '"') {
					currentValue.append('"');
					index += 1;
				} else {
					inQuotes = !inQuotes;
				}
				continue;
			}

			if (character == ',' && !inQuotes) {
				values.add(currentValue.toString());
				currentValue = new StringBuilder();
				continue;
			}

			currentValue.append(character);
		}

		values.add(currentValue.toString());
		return values;
	}

	private static List<Map<String, String>> parseFilterOptionRows() {
		try {
			List<String> lines;
			try {
				lines = Files.readAllLines(resolveFilterOptionsPath(), StandardCharsets.UTF_8);
			} catch (IOException utf8Error) {
				lines = Files.readAllLines(resolveFilterOptionsPath(), java.nio.charset.Charset.forName("windows-1252"));
			}
			if (lines.isEmpty()) {
				return List.of();
			}

			String headerLine = lines.get(0);
			if (!headerLine.isEmpty() && headerLine.charAt(0) == '\ufeff') {
				headerLine = headerLine.substring(1);
			}

			List<String> headers = parseCsvRow(headerLine);
			List<Map<String, String>> rows = new ArrayList<>();

			for (int index = 1; index < lines.size(); index += 1) {
				String row = lines.get(index);
				if (row.trim().isEmpty()) {
					continue;
				}

				List<String> values = parseCsvRow(row);
				Map<String, String> parsedRow = new LinkedHashMap<>();
				for (int columnIndex = 0; columnIndex < headers.size(); columnIndex += 1) {
					parsedRow.put(headers.get(columnIndex), columnIndex < values.size() ? values.get(columnIndex).trim() : "");
				}
				rows.add(parsedRow);
			}

			return rows;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read GOVAI_Filter_Options.csv", e);
		}
	}

	private static String normalizeFilterValue(String value) {
		return value == null ? "" : value.trim();
	}

	private static String normalizeFacetValue(String key, String value) {
		String normalizedValue = normalizeFilterValue(value);
		if (normalizedValue.isEmpty()) {
			return "";
		}

		switch (key) {
			case "agency":
				return AGENCY_ALIASES.getOrDefault(normalizedValue, normalizedValue);
			case "stage":
				return normalizedValue.replace('_', '-');
			case "topic": {
				String simplified = normalizedValue.toLowerCase();
				if ("law enforcement".equals(simplified)) {
					return "Law & Justice";
				}
				if ("service delivery".equals(simplified)) {
					return "Government Services (includes Benefits and Service Delivery)";
				}
				return normalizedValue;
			}
			case "aiClassification":
				if (AGENTIC_AI_PATTERN.matcher(normalizedValue).find() || AGENTIC_AI_HYPHEN_PATTERN.matcher(normalizedValue).find()) {
					return "Agentic AI";
				}
				if (CLASSICAL_AI_PATTERN.matcher(normalizedValue).find()) {
					return "Classical/Predictive Machine Learning";
				}
				if (COMPUTER_VISION_PATTERN.matcher(normalizedValue).find()) {
					return "Computer Vision";
				}
				if (GENERATIVE_AI_PATTERN.matcher(normalizedValue).find()) {
					return "Generative AI";
				}
				if (NLP_PATTERN.matcher(normalizedValue).find()) {
					return "Natural Language Processing";
				}
				if (REINFORCEMENT_PATTERN.matcher(normalizedValue).find()) {
					return "Reinforcement Learning";
				}
				if (OTHER_PATTERN.matcher(normalizedValue).find()) {
					return "Other";
				}
				return normalizedValue;
			case "impact":
				return normalizedValue;
			default:
				return normalizedValue;
		}
	}

	private static void addFacetOption(Map<String, LinkedHashMap<String, Integer>> templates, String key, String value) {
		String normalizedValue = normalizeFacetValue(key, value);
		if (!normalizedValue.isEmpty()) {
			templates.get(key).putIfAbsent(normalizedValue, 0);
		}
	}

	private static void addRequestMapping(Map<String, Map<String, Set<String>>> mappings, String key, String queryValue, String rawValue) {
		String normalizedQueryValue = normalizeFilterValue(queryValue);
		String normalizedRawValue = normalizeFilterValue(rawValue);
		if (normalizedQueryValue.isEmpty() || normalizedRawValue.isEmpty()) {
			return;
		}

		mappings
			.computeIfAbsent(key, ignored -> new LinkedHashMap<>())
			.computeIfAbsent(normalizedQueryValue, ignored -> new LinkedHashSet<>())
			.add(normalizedRawValue);
	}

	private static void addConfiguredOption(
		Map<String, LinkedHashMap<String, Integer>> templates,
		Map<String, Map<String, Set<String>>> mappings,
		String key,
		String rawValue
	) {
		String normalizedRawValue = normalizeFilterValue(rawValue);
		if (normalizedRawValue.isEmpty()) {
			return;
		}

		String normalizedDisplayValue = normalizeFacetValue(key, normalizedRawValue);
		addFacetOption(templates, key, normalizedRawValue);
		addRequestMapping(mappings, key, normalizedRawValue, normalizedRawValue);
		addRequestMapping(mappings, key, normalizedDisplayValue, normalizedRawValue);

		if ("stage".equals(key)) {
			addRequestMapping(mappings, key, normalizedRawValue.replace('-', '_'), normalizedRawValue.replace('-', '_'));
			addRequestMapping(mappings, key, normalizedDisplayValue.replace('-', '_'), normalizedRawValue.replace('-', '_'));
		}
	}

	private static FilterOptionConfig loadFilterOptionConfig() {
		Map<String, LinkedHashMap<String, Integer>> templates = new LinkedHashMap<>();
		for (String key : FILTER_FIELDS.keySet()) {
			templates.put(key, new LinkedHashMap<>());
		}

		Map<String, Map<String, Set<String>>> requestMappings = new HashMap<>();
		List<Map<String, String>> rows = parseFilterOptionRows();

		for (Map<String, String> row : rows) {
			addConfiguredOption(templates, requestMappings, "agency", row.get("canonical_agency"));
			addConfiguredOption(templates, requestMappings, "stage", row.get("stage_of_development"));
			addConfiguredOption(templates, requestMappings, "impact", row.get("high_impact_status"));
			addConfiguredOption(templates, requestMappings, "topic", row.get("use_case_topic_area"));
			addConfiguredOption(templates, requestMappings, "aiClassification", row.get("ai_classification"));
		}

		for (String complianceOption : COMPLIANCE_OPTIONS) {
			templates.get("compliance").put(complianceOption, 0);
			addRequestMapping(requestMappings, "compliance", complianceOption, complianceOption);
		}

		templates.get("impact").putIfAbsent(BLANK_IMPACT_VALUE, 0);
		requestMappings.computeIfAbsent("impact", ignored -> new LinkedHashMap<>()).put("", new LinkedHashSet<>(Set.of(BLANK_IMPACT_VALUE)));
		requestMappings.get("impact").put(BLANK_IMPACT_VALUE, new LinkedHashSet<>(Set.of(BLANK_IMPACT_VALUE)));

		Map<String, Map<String, List<String>>> finalizedMappings = new HashMap<>();
		requestMappings.forEach((key, values) -> {
			Map<String, List<String>> finalizedValues = new LinkedHashMap<>();
			values.forEach((value, rawValues) -> finalizedValues.put(value, List.copyOf(rawValues)));
			finalizedMappings.put(key, finalizedValues);
		});

		return new FilterOptionConfig(templates, finalizedMappings);
	}

	private static String impactFilterValue(JsonObject document) {
		String value = getString(document, "high_impact_status");
		if (value == null || value.isBlank()) {
			value = getString(document, "is_high_impact");
		}

		return value == null || value.isBlank() ? BLANK_IMPACT_VALUE : value.trim();
	}

	private static String complianceStatus(JsonObject document) {
		String stage = getString(document, "stage_of_development");
		String impact = impactFilterValue(document);
		int score = parseComplianceScore(getString(document, "risk_management_compliance_score"));

		if (!"Deployed".equals(stage) || !"high_impact".equals(impact) || score < 0) {
			return "Not required";
		}

		return score >= 9 ? "In compliance" : "Not in compliance";
	}

	private static String getString(JsonObject document, String key) {
		if (!document.has(key) || document.get(key).isJsonNull()) {
			return null;
		}

		return document.get(key).getAsString();
	}

	private static int parseComplianceScore(String value) {
		if (value == null || value.isBlank()) {
			return -1;
		}

		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}

	private static Integer parseYearValue(String value) {
		if (value == null) {
			return null;
		}

		String normalizedValue = value.trim();
		if (!normalizedValue.matches("\\d{4}")) {
			return null;
		}

		try {
			return Integer.parseInt(normalizedValue);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static void sanitizeDocument(JsonObject document) {
		if (!document.has("use_case_id") || document.get("use_case_id").isJsonNull()) {
			return;
		}

		document.addProperty("use_case_id", sanitizeUseCaseId(document.get("use_case_id").getAsString()));
	}

	private static String sanitizeUseCaseId(String value) {
		if (value == null) {
			return null;
		}

		if (!value.contains("dtype:") && !value.contains("Use Case ID")) {
			return value;
		}

		for (String line : value.split("\\R")) {
			String candidate = line.trim();
			if (candidate.isEmpty() || candidate.startsWith("Name:") || candidate.startsWith("dtype:")) {
				continue;
			}

			candidate = candidate.replaceFirst("(?i)^Use Case ID\\s+", "");
			candidate = candidate.replaceFirst("^\\d+\\s+", "").trim();
			if (!candidate.isEmpty()
				&& !candidate.equalsIgnoreCase("nan")
				&& !candidate.equalsIgnoreCase("none")
				&& !candidate.equalsIgnoreCase("null")) {
				return candidate;
			}
		}

		return value.trim();
	}

	private static String buildFilterBy(Map<String, List<String>> queryParams) {
		List<String> filters = new ArrayList<>();

		for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
			String fieldName = FILTER_FIELDS.get(entry.getKey());
			if (fieldName == null) {
				continue;
			}

			List<String> values = new ArrayList<>();
			for (String rawValue : entry.getValue()) {
				if (rawValue == null) {
					continue;
				}

				String value = rawValue.trim();
				if (value.isEmpty() && !"impact".equals(entry.getKey())) {
					continue;
				}

				List<String> mappedValues = FILTER_OPTION_CONFIG.requestValueMappings
					.getOrDefault(entry.getKey(), Collections.emptyMap())
					.getOrDefault(value, List.of(value));
				values.addAll(mappedValues);
			}

			if (values.isEmpty()) {
				continue;
			}

			List<String> deduplicatedValues = values.stream().distinct().toList();
			filters.add(fieldName + ":=[" + String.join(",", deduplicatedValues.stream().map(APIHandler::quoteFilterValue).toList()) + "]");
		}

		Integer yearFrom = parseYearValue(queryParams.getOrDefault("yearFrom", List.of()).stream().findFirst().orElse(null));
		Integer yearTo = parseYearValue(queryParams.getOrDefault("yearTo", List.of()).stream().findFirst().orElse(null));

		if (yearFrom != null) {
			filters.add(DATA_YEAR_FILTER_FIELD + ":>=" + yearFrom);
		}

		if (yearTo != null) {
			filters.add(DATA_YEAR_FILTER_FIELD + ":<=" + yearTo);
		}

		return String.join(" && ", filters);
	}

	private static String quoteFilterValue(String value) {
		String normalizedValue = BLANK_IMPACT_VALUE.equals(value) ? BLANK_IMPACT_VALUE : value;
		return "`" + normalizedValue.replace("\\", "\\\\").replace("`", "\\`") + "`";
	}

	private static Map<String, Map<String, Integer>> newFacetMap() {
		Map<String, Map<String, Integer>> facets = new LinkedHashMap<>();
		FILTER_OPTION_CONFIG.facetTemplates.forEach((key, template) -> facets.put(key, new LinkedHashMap<>(template)));
		return facets;
	}

	private static String getFacetValue(JsonObject document, String key) {
		return switch (key) {
			case "agency" -> normalizeFacetValue(key, normalizeFilterValue(getString(document, "canonical_agency") != null ? getString(document, "canonical_agency") : getString(document, "agency")));
			case "bureau" -> normalizeFacetValue(key, getString(document, "bureau_component"));
			case "stage" -> normalizeFacetValue(key, getString(document, "stage_of_development"));
			case "impact" -> impactFilterValue(document);
			case "topic" -> normalizeFacetValue(key, getString(document, "use_case_topic_area"));
			case "aiClassification" -> normalizeFacetValue(key, getString(document, "ai_classification"));
			case "compliance" -> complianceStatus(document);
			default -> "";
		};
	}

	private static void incrementFacetCount(Map<String, Map<String, Integer>> facets, String key, String value) {
		String normalizedValue = normalizeFilterValue(value);
		if (normalizedValue.isEmpty()) {
			return;
		}

		Map<String, Integer> counts = facets.get(key);
		if (counts == null) {
			return;
		}

		counts.put(normalizedValue, counts.getOrDefault(normalizedValue, 0) + 1);
	}

	private static Map<String, Map<String, Integer>> buildFacetMap(List<JsonObject> hits) {
		Map<String, Map<String, Integer>> facets = newFacetMap();

		for (JsonObject document : hits) {
			for (String key : FILTER_FIELDS.keySet()) {
				incrementFacetCount(facets, key, getFacetValue(document, key));
			}
		}

		return facets;
	}

	@Override
	public void create(@NotNull Context ctx) {
		throw new NotImplementedResponse("not implemented");
	}

	@Override
	public void getAll(@NotNull Context ctx) {
		throw new NotImplementedResponse("not implemented");
	}

	@Override
	public void getOne(@NotNull Context ctx, @NotNull String s) {
		Map<String, List<String>> queryParams = ctx.queryParamMap();

		Dotenv env = Dotenv.load();
		ArrayList<JsonObject> hits = new ArrayList<>();
		Map<String, Map<String, Integer>> facets = new LinkedHashMap<>();
		Integer found = 0;

		try {
			List<Node> nodes = new ArrayList<>();

			nodes.add(new Node("http", "localhost", "8108"));

			Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), env.get("TYPESENSE_API_KEY"));

			Client client = new Client(configuration);

			SearchParameters searchParameters = new SearchParameters()
				.q(s)
				.sortBy("agency_importance:desc,agency:asc,use_case_name:asc")
				.queryBy("use_case_name,use_case_id,agency,bureau_component,agency_abbreviation,canonical_abbreviation," +
					"canonical_sub_agency,contact_email,public_reporting_status,stage_of_development," +
					"high_impact_status,use_case_topic_area,ai_classification,purpose_and_benefits," +
					"expected_benefits,system_outputs,development_source_type,vendor_names,system_names," +
					"training_data_description,demographic_variables_used,potential_impacts_identified," +
					"user_feedback_steps")
				.limit(250);

			String filterBy = buildFilterBy(queryParams);
			if (!filterBy.isEmpty()) {
				searchParameters.filterBy(filterBy);
			}

			SearchResult searchResult = client.collections("AIUseCases").documents().search(searchParameters);
			found = searchResult.getFound();

			if (searchResult.getHits() != null) {
				searchResult.getHits().forEach((hit) -> {
					JsonObject document = GSON.toJsonTree(hit.getDocument()).getAsJsonObject();
					sanitizeDocument(document);
					hits.add(document);
				});
			}
			facets = buildFacetMap(hits);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		ctx.contentType("application/json");
		ctx.result(GSON.toJson(new SearchResponse(hits, facets, found)));
	}

	@Override
	public void update(@NotNull Context ctx, @NotNull String s) {
		throw new NotImplementedResponse("not implemented");
	}

	@Override
	public void delete(@NotNull Context ctx, @NotNull String s) {
		throw new NotImplementedResponse("not implemented");
	}
}
