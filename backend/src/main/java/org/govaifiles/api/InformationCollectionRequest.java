package org.govaifiles.api;

import java.util.Set;

record InformationCollectionRequest(String referenceNumber, String title, String agency,
                                    String abstract_,
                                    Set<SupportingDocument> supportingDocuments) {
}
