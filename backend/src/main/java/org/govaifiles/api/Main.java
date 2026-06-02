package org.govaifiles.api;

import com.google.gson.*;
import com.opencsv.CSVReaderHeaderAware;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import org.govaifiles.api.generated.db.tables.records.AiUseCasesRecord;
import org.govaifiles.api.generated.db.tables.records.CompletedTasksRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.typesense.api.Client;
import org.typesense.api.Configuration;
import org.typesense.api.FieldTypes;
import org.typesense.model.CollectionSchema;
import org.typesense.model.Field;
import org.typesense.model.ImportDocumentsParameters;
import org.typesense.model.IndexAction;
import org.typesense.resources.Node;

import java.io.*;
import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static io.javalin.apibuilder.ApiBuilder.crud;
import static org.govaifiles.api.generated.db.Tables.*;

public class Main {
	public static void main(String[] args) throws Exception {
		System.setProperty("org.jooq.no-logo", "true");
		System.setProperty("org.jooq.no-tips", "true");
		System.setProperty("org.jooq.log.org.jooq.impl.DefaultExecuteContext.logVersionSupport", "ERROR");

		Dotenv env = Dotenv.load();

		addInformationCollectionRequestEntries(
				env.get("DATA_ICR_PATH", "../linked_data/icr/"),
				env.get("POSTGRES_URL"), env.get("POSTGRES_USER"), env.get("POSTGRES_PASSWORD"));

		addSystemOfRecordsNoticeEntries(
				env.get("DATA_SORN_PATH", "../linked_data/sorn"),
				env.get("POSTGRES_URL"), env.get("POSTGRES_USER"), env.get("POSTGRES_PASSWORD"));

		addDataLinks(
				env.get("ACCEPTED_LINKS_FILE", "../surveillance-transparency/viz/accepted_links_unified.csv"),
				env.get("POSTGRES_URL"), env.get("POSTGRES_USER"), env.get("POSTGRES_PASSWORD"));

		searchSetup(env.get("TYPESENSE_API_KEY"), env.get("POSTGRES_URL"), env.get("POSTGRES_USER"), env.get("POSTGRES_PASSWORD"),
				env.get("TYPESENSE_PROTOCOL", "http"), env.get("TYPESENSE_HOST", "localhost"),
				env.get("TYPESENSE_PORT", "8108"));

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
		}).start(Integer.parseInt(env.get("BACKEND_PORT", "7070")));
	}

	private static boolean isTaskCompleted(String taskName, String url, String user, String password) throws SQLException {
		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			Result<CompletedTasksRecord> completedTask = ctx.selectFrom(COMPLETED_TASKS)
					.where(COMPLETED_TASKS.TASK_NAME.equal(taskName)).fetch();

			return completedTask.isNotEmpty();
		}
	}

	private static void completeTask(String taskName, String url, String user, String password) throws SQLException {
		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			ctx.dsl().insertInto(COMPLETED_TASKS, COMPLETED_TASKS.TASK_NAME, COMPLETED_TASKS.COMPLETED_AT)
						.values(taskName, LocalDateTime.now()).execute();
		}
	}

	private static String getString(JsonObject object, String field) {
		var element = object.get(field);
		if (element instanceof JsonNull) {
			return null;
		} else {
			return element.getAsString();
		}
	}

	private static void addInformationCollectionRequestEntries(String icrDataPath,
													   String url, String user, String password) throws Exception {
		if (isTaskCompleted("add_icr_entries", url, user, password)) {
			return;
		}

		File[] files = Path.of(icrDataPath).toFile().listFiles();
		if (files == null) {
			throw new RuntimeException("No ICR file could be found!");
		}

		JsonArray entries = new JsonArray();
		for (File file : files) {
			JsonElement element = JsonParser.parseReader(new FileReader(file));
			entries.addAll(element.getAsJsonArray());
		}

		Set<JsonObject> icrs = new HashSet<>();

		entries.forEach((entry) -> {
			JsonObject icr = entry.getAsJsonObject();
			if (icr != null) icrs.add(icr);
		});

		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			ctx.transaction((trx) -> {
				for (JsonObject icr : icrs) {
					JSONB jsonb = JSONB.jsonb(new Gson().toJson(icr.getAsJsonArray("supporting_documents")));
					trx.dsl().insertInto(
								INFORMATION_COLLECTION_REQUESTS,
								INFORMATION_COLLECTION_REQUESTS.ICR_REFERENCE_NUMBER,
								INFORMATION_COLLECTION_REQUESTS.TITLE,
								INFORMATION_COLLECTION_REQUESTS.AGENCY,
								INFORMATION_COLLECTION_REQUESTS.ABSTRACT,
								INFORMATION_COLLECTION_REQUESTS.SUPPORTING_DOCUMENTS
							)
							.values(
									getString(icr, "icr_reference_number"),
									getString(icr, "title"),
									getString(icr, "agency"),
									getString(icr, "abstract"),
									jsonb
							)
							.execute();
				}
			});
		}

		completeTask("add_icr_entries", url, user, password);
	}

	private static void addSystemOfRecordsNoticeEntries(String sornDataPath,
	                                            String url, String user, String password) throws Exception {
		if (isTaskCompleted("add_sorn_entries", url, user, password)) {
			return;
		}

		File[] files = Path.of(sornDataPath).toFile().listFiles();
		if (files == null) {
			throw new RuntimeException("No SORN file could be found!");
		}

		JsonArray entries = new JsonArray();
		for (File file : files) {
			JsonElement element = JsonParser.parseReader(new FileReader(file));
			entries.addAll(element.getAsJsonArray());
		}

		Set<JsonObject> sorns = new HashSet<>();

		entries.forEach((entry) -> {
			JsonObject sorn = entry.getAsJsonObject();
			if (sorn != null) sorns.add(sorn);
		});

		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			ctx.transaction((trx) -> {
				for (JsonObject sorn : sorns) {
					trx.dsl().insertInto(
								SYSTEMS_OF_RECORDS_NOTICES,
								SYSTEMS_OF_RECORDS_NOTICES.REFERENCE_ID,
								SYSTEMS_OF_RECORDS_NOTICES.AGENCY,
								SYSTEMS_OF_RECORDS_NOTICES.SUBJECT,
								SYSTEMS_OF_RECORDS_NOTICES.SUMMARY,
								SYSTEMS_OF_RECORDS_NOTICES.FR_DOC,
								SYSTEMS_OF_RECORDS_NOTICES.SUB_AGENCY,
								SYSTEMS_OF_RECORDS_NOTICES.ACTION,
								SYSTEMS_OF_RECORDS_NOTICES.DATES,
								SYSTEMS_OF_RECORDS_NOTICES.CONTACT,
								SYSTEMS_OF_RECORDS_NOTICES.SECURITY_CLASSIFICATION,
								SYSTEMS_OF_RECORDS_NOTICES.SYSTEM_LOCATION,
								SYSTEMS_OF_RECORDS_NOTICES.SYSTEM_MANAGER,
								SYSTEMS_OF_RECORDS_NOTICES.AUTHORITY,
								SYSTEMS_OF_RECORDS_NOTICES.PURPOSE,
								SYSTEMS_OF_RECORDS_NOTICES.CATEGORIES_OF_INDIVIDUALS,
								SYSTEMS_OF_RECORDS_NOTICES.CATEGORIES_OF_RECORDS,
								SYSTEMS_OF_RECORDS_NOTICES.RECORD_SOURCE_CATEGORIES,
								SYSTEMS_OF_RECORDS_NOTICES.ROUTINE_USES,
								SYSTEMS_OF_RECORDS_NOTICES.RETENTION_AND_DISPOSAL,
								SYSTEMS_OF_RECORDS_NOTICES.SAFEGUARDS,
								SYSTEMS_OF_RECORDS_NOTICES.ACCESS_PROCEDURES,
								SYSTEMS_OF_RECORDS_NOTICES.CONTESTING_PROCEDURES
							)
							.values(
								getString(sorn, "doc_id"),
								getString(sorn, "agency_name"),
								getString(sorn, "subject"),
								getString(sorn, "summary"),
								getString(sorn, "fr_doc"),
								getString(sorn, "sub_agency"),
								getString(sorn, "action"),
								getString(sorn, "dates"),
								getString(sorn, "contact"),
								getString(sorn, "security_classification"),
								getString(sorn, "system_location"),
								getString(sorn, "system_manager"),
								getString(sorn, "authority"),
								getString(sorn, "purpose"),
								getString(sorn, "categories_of_individuals"),
								getString(sorn, "categories_of_records"),
								getString(sorn, "record_source_categories"),
								getString(sorn, "routine_uses"),
								getString(sorn, "retention_and_disposal"),
								getString(sorn, "safeguards"),
								getString(sorn, "access_procedures"),
								getString(sorn, "contesting_procedures")
							)
							.execute();
				}
			});
		}

		completeTask("add_sorn_entries", url, user, password);
	}

	private static void addDataLinks(String acceptedLinksFile,
							 String url, String user, String password) throws Exception {
		if (isTaskCompleted("add_data_links", url, user, password)) {
			return;
		}

		CSVReaderHeaderAware linksEntriesReader = new CSVReaderHeaderAware(new FileReader(acceptedLinksFile));
		ArrayList<String[]> linksEntries = new ArrayList<>(linksEntriesReader.readAll());
		linksEntriesReader.close();

		Map<String, Set<String>> pairs = new HashMap<>();

		for (String[] entry : linksEntries) {
			addPairs(pairs, entry);
		}

		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			ctx.transaction((trx) -> {
				for (Map.Entry<String, Set<String>> entry : pairs.entrySet()) {
					trx.dsl().update(AI_USE_CASES).set(AI_USE_CASES.DATA_LINKS, entry.getValue().toArray(new String[0]))
							.where(AI_USE_CASES.USE_CASE_ID.equal(entry.getKey().replace("inv:", "")))
							.execute();
				}
			});
		}

		completeTask("add_data_links", url, user, password);
	}

	private static void addPairs(Map<String, Set<String>> pairs, String[] entry) {
		// source_id,target_id,pair,similarity,confidence,score_band,flags,explanation,custom_id
		// or
		// source_id,target_id,pair,similarity,confidence,score_band,flags,explanation,custom_id,link_type
		if (entry[1].startsWith("inv:auto_")) {
			return;
		}
		if (entry[2].equals("pra_inv") || entry[2].equals("sorn_inv")) {
			Set<String> inventoryLinks;
			if (pairs.containsKey(entry[1])) {
				inventoryLinks = pairs.get(entry[1]);
			} else {
				inventoryLinks = new HashSet<>();
			}
			inventoryLinks.add(entry[0]);
			pairs.put(entry[1], inventoryLinks);
		}
	}

	private static void searchSetup(String apiKey, String url, String user, String password,
							String typesenseProtocol, String typesenseHost, String typesensePort) throws Exception {
		List<Node> nodes = new ArrayList<>();

		nodes.add(new Node(typesenseProtocol, typesenseHost, typesensePort));

		Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), apiKey);

		Client client = new Client(configuration);

		// This list only needs to contain things used as sort-bys, see APIHandler#getOne
		List<Field> fields = new ArrayList<>();
		fields.add(new Field().name(".*").type(FieldTypes.AUTO));
		fields.add(new Field().name("agency_importance").type(FieldTypes.INT32).sort(true));
		fields.add(new Field().name("agency").type(FieldTypes.STRING).sort(true));
		fields.add(new Field().name("use_case_name").type(FieldTypes.STRING).sort(true));

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
				sb.append(element.toString()).append("\n");
			}

			ImportDocumentsParameters queryParameters = new ImportDocumentsParameters();
			queryParameters.action(IndexAction.UPSERT);

			client.collections("AIUseCases").documents().import_(sb.toString(), queryParameters);
		}
	}
}
