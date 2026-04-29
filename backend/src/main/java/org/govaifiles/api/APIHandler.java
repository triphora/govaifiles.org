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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class APIHandler implements CrudHandler {
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

		try {
			List<Node> nodes = new ArrayList<>();

			nodes.add(new Node("http", "localhost", "8108"));

			Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), env.get("TYPESENSE_API_KEY"));

			Client client = new Client(configuration);

			SearchParameters searchParameters = new SearchParameters()
				.q(s)
				.sortBy("agency_importance:desc,canonical_agency:asc,use_case_name:asc")
				.queryBy("use_case_name,use_case_id,agency,bureau_component,canonical_agency,canonical_abbreviation," +
					"canonical_sub_agency,canonical_agency,contact_email,public_reporting_status,stage_of_development," +
					"high_impact_status,use_case_topic_area,ai_classification,purpose_and_benefits," +
					"expected_benefits,system_outputs,development_source_type,vendor_names,system_names," +
					"training_data_description,demographic_variables_used,potential_impacts_identified," +
					"user_feedback_steps")
				.limit(250);

			List<String> filters = new ArrayList<>();

			for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
				for (String entryValue : entry.getValue()) {
					if (!entry.getKey().isEmpty()) filters.add(entry.getKey() + ":" + entryValue);
				}
			}

			searchParameters.filterBy(String.join(" && ", filters));

			SearchResult searchResult = client.collections("AIUseCases").documents().search(searchParameters);

			searchResult.getHits().forEach((hit) -> {
				JsonObject document = new Gson().toJsonTree(hit.getDocument()).getAsJsonObject();
				sanitizeDocument(document);
				hits.add(document);
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		ctx.contentType("application/json");
		ctx.result(new Gson().toJson(hits));
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
