package org.govaifiles.api;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import org.govaifiles.api.generated.db.tables.records.AiUseCasesRecord;
import org.jooq.DSLContext;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.typesense.api.Client;
import org.typesense.api.Configuration;
import org.typesense.api.FieldTypes;
import org.typesense.model.CollectionSchema;
import org.typesense.model.Field;
import org.typesense.model.ImportDocumentsParameters;
import org.typesense.model.IndexAction;
import org.typesense.resources.Node;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static io.javalin.apibuilder.ApiBuilder.*;
import static org.govaifiles.api.generated.db.Tables.*;

public class Main {
	private static final String BLANK_IMPACT_VALUE = "__blank__";
	private static final String DATA_YEAR_FILTER_FIELD = "data_year_filter";

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

	public static void main(String[] args) throws Exception {
		System.setProperty("org.jooq.no-logo", "true");
		System.setProperty("org.jooq.no-tips", "true");
		System.setProperty("org.jooq.log.org.jooq.impl.DefaultExecuteContext.logVersionSupport", "ERROR");

		Dotenv env = Dotenv.load();

		searchSetup(env.get("TYPESENSE_API_KEY"),	env.get("POSTGRES_URL"), env.get("POSTGRES_USER"), env.get("POSTGRES_PASSWORD"));

		APIHandler crud = new APIHandler();

		var app = Javalin.create(config -> {
			config.useVirtualThreads = true;
			config.http.asyncTimeout = 10_000L;
			config.router.apiBuilder(() -> {
				crud("ai-use-cases/{query}", crud);
			});
		});
		app.before(ctx -> {
			ctx.header("Access-Control-Allow-Origin", "*");
		}).start(7070);
	}

	static void searchSetup(String apiKey, String url, String user, String password) throws Exception {
		List<Node> nodes = new ArrayList<>();

		nodes.add(new Node("http", "localhost", "8108"));

		Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), apiKey);

		Client client = new Client(configuration);

		List<Field> fields = new ArrayList<>();
		fields.add(new Field().name(".*").type(FieldTypes.AUTO));
		fields.add(new Field().name("agency_importance").type(FieldTypes.INT32).sort(true));
		fields.add(new Field().name("agency").type(FieldTypes.STRING).sort(true).facet(true));
		fields.add(new Field().name("use_case_name").type(FieldTypes.STRING).sort(true));
		fields.add(new Field().name("bureau_component").type(FieldTypes.STRING).facet(true));
		fields.add(new Field().name("stage_of_development").type(FieldTypes.STRING).facet(true));
		fields.add(new Field().name("impact_filter").type(FieldTypes.STRING).facet(true));
		fields.add(new Field().name("use_case_topic_area").type(FieldTypes.STRING).facet(true).optional(true));
		fields.add(new Field().name("ai_classification").type(FieldTypes.STRING).facet(true).optional(true));
		fields.add(new Field().name("compliance_status").type(FieldTypes.STRING).facet(true));
		fields.add(new Field().name(DATA_YEAR_FILTER_FIELD).type(FieldTypes.INT32));

		CollectionSchema collectionSchema = new CollectionSchema();
		collectionSchema.name("AIUseCases").fields(fields);

		boolean hasCollection = false;

		for (var i : client.collections().retrieve()) {
			if (i.getName().equals("AIUseCases")) {
				hasCollection = true;
				break;
			}
		}

		if (hasCollection) {
			client.collections("AIUseCases").delete();
		}

		client.collections().create(collectionSchema);

		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			Result<AiUseCasesRecord> records = ctx.selectFrom(AI_USE_CASES).fetch();

			JSONFormat jsonFormat = new JSONFormat().header(false).recordFormat(JSONFormat.RecordFormat.OBJECT);

			JsonArray recordsArray = JsonParser.parseString(records.formatJSON(jsonFormat)).getAsJsonArray();

			StringBuilder sb = new StringBuilder();

			for (JsonElement element : recordsArray) {
				JsonObject document = element.getAsJsonObject();
				document.addProperty("impact_filter", impactFilterValue(document));
				document.addProperty("compliance_status", complianceStatus(document));
				Integer dataYearFilter = parseYearValue(getString(document, "data_year"));
				if (dataYearFilter != null) {
					document.addProperty(DATA_YEAR_FILTER_FIELD, dataYearFilter);
				}
				sb.append(document).append("\n");
			}

			ImportDocumentsParameters queryParameters = new ImportDocumentsParameters();
			queryParameters.action(IndexAction.UPSERT);

			client.collections("AIUseCases").documents().import_(sb.toString(), queryParameters);
		}
	}
}
