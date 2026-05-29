package org.govaifiles.api;

import com.google.gson.*;
import com.opencsv.CSVReaderHeaderAware;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import org.govaifiles.api.generated.db.tables.records.AiUseCasesRecord;
import org.govaifiles.api.generated.db.tables.records.CompletedTasksRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static io.javalin.apibuilder.ApiBuilder.crud;
import static org.govaifiles.api.generated.db.Tables.*;
import static org.jooq.impl.DSL.*;

public class Main {
	public static void main(String[] args) throws Exception {
		System.setProperty("org.jooq.no-logo", "true");
		System.setProperty("org.jooq.no-tips", "true");
		System.setProperty("org.jooq.log.org.jooq.impl.DefaultExecuteContext.logVersionSupport", "ERROR");

		Dotenv env = Dotenv.load();

		addInformationCollectionRequestEntries(
				env.get("DATA_ICR_PATH", "../sample_data/icr/"),
				env.get("POSTGRES_URL"), env.get("POSTGRES_USER"), env.get("POSTGRES_PASSWORD"));

		addDataLinks(
				env.get("ACCEPTED_LINKS_FILE", "../surveillance-transparency/viz/accepted_links_unified.csv"),
				env.get("RELATED_PAIRS_FILE", "../surveillance-transparency/viz/related_pairs_unified.csv"),
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

	static boolean taskCompleted(String taskName, String url, String user, String password) throws SQLException {
		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			Result<CompletedTasksRecord> completedTask = ctx.selectFrom(COMPLETED_TASKS)
					.where(COMPLETED_TASKS.TASK_NAME.equal(taskName)).fetch();

			return completedTask.isNotEmpty();
		}
	}

	static void completeTask(String taskName, String url, String user, String password) throws SQLException {
		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			ctx.dsl().insertInto(COMPLETED_TASKS, COMPLETED_TASKS.TASK_NAME, COMPLETED_TASKS.COMPLETED_AT)
						.values(taskName, LocalDateTime.now()).execute();
		}
	}

	private record InformationCollectionRequest (String referenceNumber, String title, String agency,
												 String abstract_, Set<SupportingDocument> supportingDocuments) {}

	private record SupportingDocument (String type, String name, String url) {}

	static void addInformationCollectionRequestEntries(String icrDataPath,
													   String url, String user, String password) throws Exception {
		if (taskCompleted("add_icr_entries", url, user, password)) {
			return;
		}

		File[] files = Path.of(icrDataPath).toFile().listFiles();
		if (files == null) {
			throw new RuntimeException("No ICR files could be found!");
		}

		JsonArray entries = new JsonArray();
		for (File file : files) {
			JsonElement element = JsonParser.parseReader(new FileReader(file));
			entries.addAll(element.getAsJsonArray());
		}

		Set<InformationCollectionRequest> icrs = new HashSet<>();

		entries.forEach((entry) -> {
			JsonElement icrElem = entry.getAsJsonObject().get("information_collection_request");
			if (icrElem == null) return;

			JsonObject icr = icrElem.getAsJsonObject();
			JsonObject id = icr.get("identification").getAsJsonObject();

			String referenceNumber = id.get("icr_reference_number").getAsString();
			String title = id.get("title").getAsString();
			String agency = id.get("agency").getAsString();
			String abstract_ = icr.get("abstract").getAsString();

			JsonArray supportingDocumentsJson = icr.get("supporting_documents").getAsJsonArray();
			Set<SupportingDocument> supportingDocuments = new HashSet<>();

			supportingDocumentsJson.forEach((document) -> {
				JsonObject doc = document.getAsJsonObject();
				String type = doc.get("type").getAsString();
				String docUrl = doc.get("url").getAsString();
				String filename = doc.get("filename").getAsString();
				supportingDocuments.add(new SupportingDocument(type, docUrl, filename));
			});

			icrs.add(new InformationCollectionRequest(referenceNumber, title, agency, abstract_, supportingDocuments));
		});

		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			ctx.transaction((trx) -> {
				for (InformationCollectionRequest icr : icrs) {
					JSONB jsonb = JSONB.jsonb(new Gson().toJson(icr.supportingDocuments()));
					trx.dsl().insertInto(INFORMATION_COLLECTION_REQUESTS,
							INFORMATION_COLLECTION_REQUESTS.ICR_REFERENCE_NUMBER,
							INFORMATION_COLLECTION_REQUESTS.TITLE,
							INFORMATION_COLLECTION_REQUESTS.AGENCY,
							INFORMATION_COLLECTION_REQUESTS.ABSTRACT,
							INFORMATION_COLLECTION_REQUESTS.SUPPORTING_DOCUMENTS)
									.values(icr.referenceNumber(), icr.title(), icr.agency(), icr.abstract_(), jsonb)
											.execute();
				}
			});
		}

		taskCompleted("add_icr_entries", url, user, password);
	}

	static void addDataLinks(String acceptedLinksFile, String relatedPairsFile,
												 String url, String user, String password) throws Exception {
		if (taskCompleted("add_data_links", url, user, password)) {

			return;
		}

		CSVReaderHeaderAware linksEntriesReader = new CSVReaderHeaderAware(new FileReader(acceptedLinksFile));
		ArrayList<String[]> linksEntries = new ArrayList<>(linksEntriesReader.readAll());
		linksEntriesReader.close();

		CSVReaderHeaderAware pairsEntriesReader = new CSVReaderHeaderAware(new FileReader(relatedPairsFile));
		ArrayList<String[]> pairsEntries = new ArrayList<>(pairsEntriesReader.readAll());
		linksEntriesReader.close();

		Map<String, Set<String>> pairs = new HashMap<>();

		for (String[] entry : linksEntries) {
			addPairs(pairs, entry);
		}
		for (String[] entry : pairsEntries) {
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

	static void addPairs(Map<String, Set<String>> pairs, String[] entry) {
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

	static void searchSetup(String apiKey, String url, String user, String password,
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
