package org.govaifiles.api;

import com.google.gson.JsonArray;

record InformationCollectionRequest(String referenceNumber, String title, String agency,
                                    String abstract_,
                                    JsonArray supportingDocuments) {
}
