package org.govaifiles.api;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import org.govaifiles.api.generated.db.tables.records.AiUseCase_2025Record;
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
				crud("ai-use-case-2025/{query}", crud);
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

		CollectionSchema collectionSchema = new CollectionSchema();
		collectionSchema.name("AIUseCase2025").fields(fields);

		boolean hasCollection = false;

		for (var i : client.collections().retrieve()) {
			if (i.getName().equals("AIUseCase2025")) {
				hasCollection = true;
				break;
			}
		}

		if (hasCollection) {
			client.collections("AIUseCase2025").delete();
		}

		client.collections().create(collectionSchema);

		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			Result<AiUseCase_2025Record> records = ctx.selectFrom(AI_USE_CASE_2025).fetch();

			JSONFormat jsonFormat = new JSONFormat().header(false).recordFormat(JSONFormat.RecordFormat.OBJECT);

			JsonArray recordsArray = JsonParser.parseString(records.formatJSON(jsonFormat)).getAsJsonArray();

			StringBuilder sb = new StringBuilder();

			for (JsonElement element : recordsArray) {
				sb.append(element.toString()).append("\n");
			}

			ImportDocumentsParameters queryParameters = new ImportDocumentsParameters();
			queryParameters.action(IndexAction.UPSERT);

			client.collections("AIUseCase2025").documents().import_(sb.toString(), queryParameters);
		}
	}
}