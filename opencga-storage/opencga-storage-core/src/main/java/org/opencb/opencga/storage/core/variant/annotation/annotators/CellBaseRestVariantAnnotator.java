/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.storage.core.variant.annotation.annotators;

import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.avro.VariantAnnotation;
import org.opencb.cellbase.client.config.ClientConfiguration;
import org.opencb.cellbase.client.rest.CellBaseClient;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryResponse;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.storage.core.config.StorageConfiguration;
import org.opencb.opencga.storage.core.variant.annotation.VariantAnnotatorException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created on 23/11/16.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class CellBaseRestVariantAnnotator extends AbstractCellBaseVariantAnnotator {
    private static final int TIMEOUT = 10000;

    private final CellBaseClient cellBaseClient;
    private final Function<Variant, String> variantSerializer;

    public CellBaseRestVariantAnnotator(StorageConfiguration storageConfiguration, ObjectMap options) throws VariantAnnotatorException {
        super(storageConfiguration, options);

        List<String> hosts = storageConfiguration.getCellbase().getHosts();
        if (hosts.isEmpty()) {
            throw new VariantAnnotatorException("Missing defaultValue \"CellBase Hosts\"");
        }

        String cellbaseRest = hosts.get(0);
        checkNotNull(cellbaseRest, "cellbase hosts");
        ClientConfiguration clientConfiguration = storageConfiguration.getCellbase().toClientConfiguration();
        clientConfiguration.getRest().setTimeout(TIMEOUT);
        cellBaseClient = new CellBaseClient(species, assembly, clientConfiguration);

        logger.info("Annotating with Cellbase REST. host '{}', version '{}', species '{}', assembly '{}'",
                cellbaseRest, cellbaseVersion, species, assembly);

        if (impreciseVariants) {
            // If the cellbase sever supports imprecise variants, use the original toString, which adds the CIPOS and CIEND
            variantSerializer = Variant::toString;
        } else {
            variantSerializer = variant -> variant.getChromosome()
                    + ':' + variant.getStart()
                    + ':' + (variant.getReference().isEmpty() ? "-" : variant.getReference())
                    + ':' + (variant.getAlternate().isEmpty() ? "-" : variant.getAlternate());
        }
    }

    @Override
    protected List<QueryResult<VariantAnnotation>> annotateFiltered(List<Variant> variants) throws VariantAnnotatorException {
        if (variants.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // FIXME getAnnotations has been renamed to getAnnotationByVariantIds
            QueryResponse<VariantAnnotation> queryResponse = cellBaseClient.getVariantClient()
                    .getAnnotations(variants.stream().map(variantSerializer).collect(Collectors.toList()), queryOptions, true);
            return queryResponse.getResponse();
        } catch (IOException e) {
            throw new VariantAnnotatorException("Error fetching variants from Client");
        }
    }
}
