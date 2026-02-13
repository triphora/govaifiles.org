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
				.queryBy("agency,use_case_id,use_case_name,bureau_component,stage_of_development_raw,stage_of_development," +
					"is_high_impact_raw,is_high_impact,justification,use_case_topic_area,ai_classification,problem_statement," +
					"expected_benefits,system_outputs,operational_start_date,development_source,vendor_name,has_ato,systems_name," +
					"training_and_evaluation_data,federal_data_catalog_link,involves_pii,pia_link,demographic_variables_used," +
					"includes_custom_code,open_source_code_link,pre_deployment_testing_status,ai_impact_assessment_status," +
					"potential_impacts_description,independent_review_status,ongoing_monitoring_process,operator_training_status," +
					"fail_safe_status,appeal_process_status,public_and_user_feedback")
				.limit(250);

			List<String> filters = new ArrayList<>();

			for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
				for (String entryValue : entry.getValue()) {
					if (!entry.getKey().isEmpty()) filters.add(entry.getKey() + ":" + entryValue);
				}
			}

			searchParameters.filterBy(String.join(" && ", filters));

			SearchResult searchResult = client.collections("AIUseCase2025").documents().search(searchParameters);

			searchResult.getHits().forEach((hit) ->
				hits.add(new Gson().toJsonTree(hit.getDocument()).getAsJsonObject()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		ctx.json(!hits.isEmpty() ? hits.toString() : "[]");
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
