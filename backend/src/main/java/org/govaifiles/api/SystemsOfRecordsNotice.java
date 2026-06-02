package org.govaifiles.api;

public record SystemsOfRecordsNotice(String referenceId, String agency, String subject,
                                     String summary, String subAgency,
                                     String categoriesOfIndividuals, String categoriesOfRecords) {
}
